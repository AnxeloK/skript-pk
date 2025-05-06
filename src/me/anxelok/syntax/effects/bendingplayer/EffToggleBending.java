package me.anxelok.syntax.effects.bendingplayer;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.projectkorra.projectkorra.BendingPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import me.anxelok.Main;

@Name("Toggle Player's Bending")
@Description("Enables or disables all bending abilities for a player. When disabled, the player cannot use any bending abilities until re-enabled. Can toggle between states or set to a specific state.")
@Examples({
    "# Toggle between enabled/disabled",
    "toggle player's bending",
    "# Force bending to be enabled",
    "toggle player's bending to true",
    "# Force bending to be disabled",
    "toggle player's bending to false",
    "# Toggle based on a condition",
    "if player's health is less than 5:",
    "    toggle player's bending to false"
})
@Since(Main.VERSION)
public class EffToggleBending extends Effect {
    private Expression<Player> playerExpr;
    private Expression<Boolean> booleanExpr;

    static {
        // register the effect with Skript
        Skript.registerEffect(EffToggleBending.class,
                "toggle %player%'s bending[ to %-boolean%]");
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        playerExpr = (Expression<Player>) exprs[0];
        if (exprs.length > 1) {
            booleanExpr = (Expression<Boolean>) exprs[1];
        }
        return true;
    }

    @Override
    protected void execute(Event e) {
        Player player = playerExpr.getSingle(e);
        if (player == null) return;

        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer == null) return;

        if (booleanExpr != null) {
            Boolean value = booleanExpr.getSingle(e);
            if (value != null) {
                if (value != bPlayer.isToggled()) {
                    bPlayer.toggleBending();
                }
            }
        } else {
            bPlayer.toggleBending();
        }
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "toggle " + playerExpr.toString(e, debug) + "'s bending" +
                (booleanExpr != null ? " to " + booleanExpr.toString(e, debug) : "");
    }
}
