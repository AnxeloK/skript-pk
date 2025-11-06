package me.anxelok.syntax.effects.cooldown;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;
import com.projectkorra.projectkorra.BendingPlayer;
import me.anxelok.Main;
import me.anxelok.syntax.cooldown.CooldownHelper;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Set Cooldown")
@Description("Sets a ProjectKorra cooldown for a player. Using a non-positive duration removes the cooldown instead.")
@Examples({
    "set cooldown \"WaterManipulation\" for player to 5 seconds",
    "set cooldown {_ability} of victim to 2 minutes"
})
@Since(Main.VERSION)
public class EffSetCooldown extends Effect {

    static {
        Skript.registerEffect(
            EffSetCooldown.class,
            "set cooldown %string% (of|for) %player% to %timespan%"
        );
    }

    private Expression<String> nameExpr;
    private Expression<Player> playerExpr;
    private Expression<Timespan> durationExpr;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean kleenean, ParseResult parseResult) {
        nameExpr = (Expression<String>) expressions[0];
        playerExpr = (Expression<Player>) expressions[1];
        durationExpr = (Expression<Timespan>) expressions[2];
        return true;
    }

    @Override
    protected void execute(Event event) {
        String requested = nameExpr.getSingle(event);
        Player player = playerExpr.getSingle(event);
        Timespan duration = durationExpr.getSingle(event);
        if (requested == null || player == null || duration == null) {
            return;
        }

        String resolved = CooldownHelper.resolveCooldown(requested);
        if (resolved == null || CooldownHelper.isAllToken(resolved)) {
            Skript.error("Unknown ProjectKorra cooldown '" + requested + "'.");
            return;
        }

        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer == null) {
            return;
        }

        long millis = Math.max(0L, duration.getMilliSeconds());
        if (millis <= 0L) {
            bPlayer.removeCooldown(resolved);
            return;
        }

        bPlayer.addCooldown(resolved, millis, false);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "set cooldown " + nameExpr.toString(event, debug) + " for " + playerExpr.toString(event, debug) + " to " + durationExpr.toString(event, debug);
    }
}
