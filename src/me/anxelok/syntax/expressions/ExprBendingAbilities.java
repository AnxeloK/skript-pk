package me.anxelok.syntax.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.projectkorra.projectkorra.BendingPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Bending Abilities")
@Description("Returns a list of all abilities bound to a player as a fixed-size array (slots 1 to 9). Unset slots will be empty.")
@Examples({
        "set {_abilities::*} to player's abilities",
        "loop {_abilities::*}:",
        "    send \"Slot %loop-index%: %loop-value%\""
})
@Since("1.0")
public class ExprBendingAbilities extends SimpleExpression<String> {

    private ch.njol.skript.lang.Expression<Player> playerExpr;
    private static final int MAX_SLOTS = 9; // Maximum number of ability slots

    static {
        Skript.registerExpression(ExprBendingAbilities.class, String.class,
                ch.njol.skript.lang.ExpressionType.SIMPLE,
                "%player%'s abilities");
    }

    @Override
    public boolean init(ch.njol.skript.lang.Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ch.njol.skript.lang.SkriptParser.ParseResult parseResult) {
        playerExpr = (ch.njol.skript.lang.Expression<Player>) exprs[0];
        return true;
    }

    @Nullable
    @Override
    protected String[] get(Event e) {
        Player player = playerExpr.getSingle(e);
        if (player == null) return new String[0];
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer == null) return new String[0];

        // Get the abilities map (slot -> ability name)
        java.util.Map<Integer, String> abilities = bPlayer.getAbilities();

        // Create an array with a fixed length of MAX_SLOTS.
        String[] output = new String[MAX_SLOTS];
        for (int slot = 1; slot <= MAX_SLOTS; slot++) {
            if (abilities != null && abilities.containsKey(slot)) {
                output[slot - 1] = abilities.get(slot);
            } else {
                output[slot - 1] = ""; // or you could use "none" as a placeholder
            }
        }
        return output;
    }

    @Override
    public boolean isSingle() {
        return false;  // We return a list of abilities (one for each slot).
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return playerExpr.toString(e, debug) + "'s abilities";
    }
}
