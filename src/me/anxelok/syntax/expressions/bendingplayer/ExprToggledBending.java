package me.anxelok.syntax.expressions.bendingplayer;

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
@Description({
    "Checks if a player's bending is enabled or disabled.",
    "Returns true if bending is enabled, false if disabled."
})
@Examples({
    "# Check if player has bending enabled",
    "set {_isBendingEnabled} to player's toggled bending",
    "",
    "# Use in conditions",
    "if player's toggled bending is false:",
    "    send \"Your bending is currently disabled!\"",
    "",
    "# Store status for multiple players",
    "loop all players:",
    "    set {_status::%loop-player%} to loop-player's toggled bending"
})
@Since(Main.VERSION)
public class ExprToggledBending extends SimpleExpression<Boolean> {

    private Expression<Player> playerExpr;
    private boolean checkStatus;  // true for on, false for off

    static {
        Skript.registerExpression(ExprToggledBending.class, Boolean.class, ExpressionType.SIMPLE,
                "%player%'s toggled bending");
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

        // check if bending is true
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
