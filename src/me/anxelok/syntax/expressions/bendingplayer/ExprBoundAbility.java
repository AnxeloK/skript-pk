package me.anxelok.syntax.expressions.bendingplayer;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.projectkorra.projectkorra.BendingPlayer;
import me.anxelok.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Bound Ability")
@Description({
    "Returns the player's currently bound ability or the ability bound to a specific slot.",
    "If no slot is specified, returns the ability from the current active slot.",
    "Returns nothing if the slot is empty or invalid."
})
@Examples({
    "# Get current bound ability",
    "set {_ability} to player's bound ability",
    "",
    "# Get ability from specific slot",
    "set {_slotOne} to player's bound ability in slot 1",
    "",
    "# Check for specific abilities",
    "if player's bound ability is \"WaterManipulation\":",
    "    send \"You have WaterManipulation ready!\"",
    "",
    "# Loop through all slots (1-9)",
    "loop 9 times:",
    "    set {_slot::%loop-number%} to player's bound ability in slot loop-number"
})
@Since(Main.VERSION)
public class ExprBoundAbility extends SimpleExpression<String> {

    private Expression<Player> playerExpr;
    private Expression<Integer> slotExpr;

    static {
        Skript.registerExpression(ExprBoundAbility.class, String.class,
                ch.njol.skript.lang.ExpressionType.SIMPLE,
                "%player%'s bound ability[ in slot %-integer%]");
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ch.njol.skript.lang.SkriptParser.ParseResult parseResult) {
        playerExpr = (Expression<Player>) exprs[0];
        slotExpr = (Expression<Integer>) exprs[1];
        return true;
    }

    @Nullable
    @Override
    protected String[] get(Event e) {
        Player player = playerExpr.getSingle(e);
        if (player == null) return new String[0];
        
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer == null) return new String[0];

        // If slot is specified, get ability from that slot
        if (slotExpr != null) {
            Integer slot = slotExpr.getSingle(e);
            if (slot == null) return new String[0];
            
            java.util.HashMap<Integer, String> abilities = bPlayer.getAbilities();
            if (abilities == null || !abilities.containsKey(slot)) return new String[0];
            return new String[]{ abilities.get(slot) };
        }
        
        // Otherwise get current bound ability
        String ability = bPlayer.getBoundAbilityName();
        return ability != null ? new String[]{ ability } : new String[0];
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return playerExpr.toString(e, debug) + "'s bound ability" + 
               (slotExpr != null ? " in slot " + slotExpr.toString(e, debug) : "");
    }
}
