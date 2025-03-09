package me.anxelok.syntax.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.projectkorra.projectkorra.BendingPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Bending Status")
@Description("Checks if a player's bending is enabled (true) or disabled (false).")
@Examples({
        "set {_isBendingEnabled} to player's bending status"
})
@Since("1.0")
public class ExprBendingStatus extends SimpleExpression<Boolean> {

    private Expression<Player> playerExpr;
    private boolean checkStatus;  // true to check if bending is on, false to check if bending is off

    static {
        Skript.registerExpression(ExprBendingStatus.class, Boolean.class, ExpressionType.SIMPLE,
                "%player%'s bending toggled state");
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        playerExpr = (Expression<Player>) exprs[0];
        return true;
    }

    @Nullable
    @Override
    public Boolean[] get(Event e) {
        Player player = playerExpr.getSingle(e);
        if (player == null) return new Boolean[0];  // Return empty array if no player found

        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer == null) return new Boolean[0];  // Return empty array if no BendingPlayer is found

        // Return whether bending is enabled or not
        return new Boolean[] { bPlayer.isToggled() };
    }

    @Override
    public boolean isSingle() {
        return true;  // We return a single boolean (whether bending is on or off)
    }

    @Override
    public Class<? extends Boolean> getReturnType() {
        return Boolean.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return playerExpr.toString(e, debug) + "'s bending status";
    }
}
