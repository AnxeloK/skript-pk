package me.anxelok.syntax.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.projectkorra.projectkorra.BendingPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@Name("Bind and Unbind Ability")
@Description("Bind or unbind an ability to/from a player or preset")
@Examples({
        "bind \"EarthWave\" to current slot of player",
        "unbind ability from slot 1 of player",
        "unbind ability \"EarthWave\" of player"
})
@Since("1.0")
public class EffBindUnbindAbility extends Effect {

    private Expression<String> abilityExpr;
    private Expression<Player> playerExpr;
    private Expression<Integer> slotExpr;
    private boolean isBind; // true for bind, false for unbind by slot
    private boolean unbindByName; // true for unbind by name

    static {
        Skript.registerEffect(EffBindUnbindAbility.class,
                "bind %string% to (slot %integer%|current slot) of %player%",
                "unbind ability (from slot %integer%|current slot|%string%) of %player%");
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (matchedPattern == 0) {
            // bind ability to current slot
            abilityExpr = (Expression<String>) exprs[0];
            playerExpr = (Expression<Player>) exprs[1];
            isBind = true;
        } else if (matchedPattern == 1) {
            // unbind by slot
            slotExpr = (Expression<Integer>) exprs[0];
            playerExpr = (Expression<Player>) exprs[1];
            isBind = false;
        } else {
            // unbind by ability name
            abilityExpr = (Expression<String>) exprs[0];
            playerExpr = (Expression<Player>) exprs[1];
            unbindByName = true;
            isBind = false;
        }
        return true;
    }

    @Override
    protected void execute(Event e) {
        Player player = playerExpr.getSingle(e);
        if (player == null) return;
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer == null) return;

        Map<Integer, String> abilities = bPlayer.getAbilities();
        if (abilities == null) {
            abilities = new HashMap<>();
        }

        if (isBind) {
            // bind ability to specific slot or current slot
            String ability = abilityExpr.getSingle(e);
            if (ability == null) return;

            Integer slot;
            if (slotExpr != null) {
                slot = slotExpr.getSingle(e); // Use specific slot
            } else {
                slot = player.getInventory().getHeldItemSlot(); // Use current slot
            }

            if (slot == null) return;
            abilities.put(slot, ability);
            bPlayer.setAbilities((HashMap<Integer, String>) abilities);
        } else if (unbindByName) {
            // unbind ability by name
            String ability = abilityExpr.getSingle(e);
            if (ability == null) return;
            abilities.entrySet().removeIf(entry -> entry.getValue().equalsIgnoreCase(ability));
            bPlayer.setAbilities((HashMap<Integer, String>) abilities);
        } else {
            // unbind by slot number or current slot
            Integer slot;
            if (slotExpr != null) {
                slot = slotExpr.getSingle(e); // Use specific slot
            } else {
                slot = player.getInventory().getHeldItemSlot(); // Use current slot
            }

            if (slot == null) return;
            abilities.remove(slot);
            bPlayer.setAbilities((HashMap<Integer, String>) abilities);
        }
    }


    @Override
    public String toString(@Nullable Event e, boolean debug) {
        if (isBind) {
            return "bind " + abilityExpr.toString(e, debug) + " to current slot of " + playerExpr.toString(e, debug);
        } else if (unbindByName) {
            return "unbind ability " + abilityExpr.toString(e, debug) + " of " + playerExpr.toString(e, debug);
        } else {
            return "unbind ability from slot " + slotExpr.toString(e, debug) + " of " + playerExpr.toString(e, debug);
        }
    }
}
