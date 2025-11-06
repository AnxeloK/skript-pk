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
import ch.njol.util.Kleenean;
import me.anxelok.Main;
import me.anxelok.syntax.cooldown.CooldownHelper;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

@Name("Active Cooldown Names")
@Description("Lists active ProjectKorra cooldowns for a player in the same order as /bending cooldown view.")
@Examples({
    "set {_cooldowns::*} to active cooldowns of player",
    "loop active cooldowns of victim:"
})
@Since(Main.VERSION)
public class ExprPlayerCooldownNames extends SimpleExpression<String> {

    static {
        Skript.registerExpression(
            ExprPlayerCooldownNames.class,
            String.class,
            ExpressionType.COMBINED,
            "[the] active cooldowns (of|for) %player%"
        );
    }

    private Expression<Player> playerExpr;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean kleenean, ParseResult parseResult) {
        playerExpr = (Expression<Player>) expressions[0];
        return true;
    }

    @Override
    protected String[] get(Event event) {
        Player player = playerExpr.getSingle(event);
        if (player == null) {
            return new String[0];
        }

        return CooldownHelper.getActiveCooldowns(player).entrySet().stream()
            .sorted(Comparator.comparingLong(entry -> entry.getValue().getCooldown()))
            .map(entry -> entry.getKey())
            .toArray(String[]::new);
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "active cooldowns of " + playerExpr.toString(event, debug);
    }
}
