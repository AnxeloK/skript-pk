package me.anxelok.syntax.expressions.bendingplayer;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.projectkorra.projectkorra.BendingPlayer;
import me.anxelok.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Get All Bound Bending Abilities")
@Description("Returns a list of all abilities currently bound to a player's slots (1-9). Empty slots will return an empty string. The returned list is always 9 elements long, representing each slot.")
@Examples({
        "# Store all bound abilities in a list variable",
        "set {_abilities::*} to player's bound abilities",
        "# Loop through each slot to display abilities",
        "loop {_abilities::*}:",
        "    # Skip empty slots",
        "    if loop-value is not \"\":",
        "        send \"Slot %loop-index%: %loop-value%\""
})
@Since(Main.VERSION)
public class ExprBoundAbilities extends SimpleExpression<String> {

    private ch.njol.skript.lang.Expression<Player> playerExpr;
    private static final int MAX_SLOTS = 9; // max ability slots

    static {
        Skript.registerExpression(ExprBoundAbilities.class, String.class,
                ch.njol.skript.lang.ExpressionType.SIMPLE,
                "%player%'s bound abilities");
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
        if (player == null) return new String[0];  // no player
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer == null) return new String[0];  // no bending player

        // get abilities from slots
        java.util.Map<Integer, String> abilities = bPlayer.getAbilities();

        // create output array
        String[] output = new String[MAX_SLOTS];
        for (int slot = 1; slot <= MAX_SLOTS; slot++) {
            // fill slots with abilities or empty
            output[slot - 1] = abilities != null && abilities.containsKey(slot) ? abilities.get(slot) : "";
        }
        return output;
    }

    @Override
    public boolean isSingle() {
        return false;  // return list
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
