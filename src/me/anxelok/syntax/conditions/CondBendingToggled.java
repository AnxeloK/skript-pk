package me.anxelok.syntax.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.projectkorra.projectkorra.BendingPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Bending Toggled")
@Description("Checks if a player's bending is enabled or disabled.")
@Examples({
        "if player's bending is on:",
        "if player's bending is off:"
})
@Since("1.0")
public class CondBendingToggled extends Condition {

    private Expression<Player> playerExpr;
    private boolean shouldBeOn; // true if checking for "on", false for "off"

    static {
        // Register two patterns: one for on, one for off.
        Skript.registerCondition(CondBendingToggled.class,
                "%player%'s bending is on",
                "%player%'s bending is off");
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        playerExpr = (Expression<Player>) exprs[0];
        // Using matchedPattern: 0 for "on", 1 for "off"
        shouldBeOn = matchedPattern == 0;
        return true;
    }

    @Override
    public boolean check(Event e) {
        Player player = playerExpr.getSingle(e);
        if (player == null) return false;

        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer == null) return false;

        return bPlayer.isToggled() == shouldBeOn;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return playerExpr.toString(e, debug) + "'s bending is " + (shouldBeOn ? "on" : "off");
    }
}
