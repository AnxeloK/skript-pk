package me.anxelok.syntax.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.projectkorra.projectkorra.BendingPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Bound Ability by Slot")
@Description("Returns the bound ability from a specific slot (first, second, etc.).")
@Examples({
        "set {_ability::1} to player's first bound ability",
        "set {_ability::2} to player's second bound ability"
})
@Since("1.0")
public class ExprBoundAbilityBySlot extends SimpleExpression<String> {

    private ch.njol.skript.lang.Expression<Player> playerExpr;
    private int slot;  // stores the ability slot (1 for first, etc.)

    static {
        Skript.registerExpression(ExprBoundAbilityBySlot.class, String.class,
                ch.njol.skript.lang.ExpressionType.SIMPLE,
                "%player%'s [(1¦first)|(2¦second)|(3¦third)|(4¦fourth)|(5¦fifth)|(6¦sixth)|(7¦seventh)|(8¦eighth)|(9¦ninth)] bound ability");
    }

    @Override
    public boolean init(ch.njol.skript.lang.Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ch.njol.skript.lang.SkriptParser.ParseResult parseResult) {
        playerExpr = (ch.njol.skript.lang.Expression<Player>) exprs[0];
        slot = parseResult.mark;  // set slot based on parsed pattern
        return true;
    }

    @Nullable
    @Override
    protected String[] get(Event e) {
        Player player = playerExpr.getSingle(e);
        if (player == null) return new String[0];  // return empty if no player
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer == null) return new String[0];  // return empty if no bending player
        java.util.HashMap<Integer, String> abilities = bPlayer.getAbilities();
        if (abilities == null || !abilities.containsKey(slot)) return new String[0];  // return empty if no ability found for slot
        String ability = abilities.get(slot);
        return new String[]{ ability };  // return the found ability
    }

    @Override
    public boolean isSingle() {
        return true;  // expression returns a single value
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;  // returns a string type
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return playerExpr.toString(e, debug) + "'s " + ordinal(slot) + " bound ability";  // string representation
    }

    // converts integer to its ordinal form (e.g., 1 -> first)
    private String ordinal(int i) {
        switch(i) {
            case 1: return "first";
            case 2: return "second";
            case 3: return "third";
            case 4: return "fourth";
            case 5: return "fifth";
            case 6: return "sixth";
            case 7: return "seventh";
            case 8: return "eighth";
            case 9: return "ninth";
            default: return i + "th";
        }
    }
}
