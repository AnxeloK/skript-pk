package me.anxelok.syntax.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.projectkorra.projectkorra.BendingPlayer;
import me.anxelok.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Bending Status")
@Description("Checks if a player's bending is enabled (true) or disabled (false).")
@Examples({
        "set {_isBendingEnabled} to player's bending status"
})
@Since(Main.VERSION)
public class ExprBendingToggledState extends SimpleExpression<Boolean> {

    private Expression<Player> playerExpr;
    private boolean checkStatus;  // true for on, false for off

    static {
        Skript.registerExpression(ExprBendingToggledState.class, Boolean.class, ExpressionType.SIMPLE,
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
        if (player == null) return new Boolean[0];  // no player

        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer == null) return new Boolean[0];  // no bending player

        // check if bending is on
        return new Boolean[] { bPlayer.isToggled() };
    }

    @Override
    public boolean isSingle() {
        return true;  // returns one value
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
