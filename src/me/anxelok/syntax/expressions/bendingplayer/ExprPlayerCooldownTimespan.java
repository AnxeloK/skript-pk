package me.anxelok.syntax.expressions.bendingplayer;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;
import com.projectkorra.projectkorra.BendingPlayer;
import me.anxelok.Main;
import me.anxelok.syntax.cooldown.CooldownHelper;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Remaining Cooldown Timespan")
@Description("Returns the remaining duration of a ProjectKorra cooldown for a player as a timespan. Returns zero when the cooldown is not active.")
@Examples({
    "# store the remaining time of a cooldown",
    "set {_remaining} to remaining cooldown \"fireblast\" of player",
    "# check if a victim still has an active cooldown",
    "if remaining cooldown \"avatarstate\" of victim is greater than 0:"
})
@Since(Main.VERSION)
public class ExprPlayerCooldownTimespan extends SimpleExpression<Timespan> {

    static {
        Skript.registerExpression(
            ExprPlayerCooldownTimespan.class,
            Timespan.class,
            ExpressionType.COMBINED,
            "[the] [remaining] cooldown %string% (of|for) %player%"
        );
    }

    private Expression<String> cooldownName;
    private Expression<Player> playerExpr;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean kleenean, ParseResult parseResult) {
        cooldownName = (Expression<String>) expressions[0];
        playerExpr = (Expression<Player>) expressions[1];
        return true;
    }

    @Override
    protected Timespan[] get(Event event) {
        String name = cooldownName.getSingle(event);
        Player player = playerExpr.getSingle(event);
        if (name == null || player == null) {
            return new Timespan[0];
        }

        String resolved = CooldownHelper.resolveCooldown(name);
        if (resolved == null || "*".equals(resolved)) {
            Skript.error("Unknown ProjectKorra cooldown '" + name + "'.");
            return new Timespan[0];
        }

        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer == null) {
            return new Timespan[0];
        }

        long expiry = bPlayer.getCooldown(resolved);
        long remaining = expiry > 0 ? expiry - System.currentTimeMillis() : 0;
        if (remaining < 0) {
            remaining = 0;
        }
        return new Timespan[]{new Timespan(remaining)};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Timespan> getReturnType() {
        return Timespan.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "cooldown " + cooldownName.toString(event, debug) + " of " + playerExpr.toString(event, debug);
    }
}
