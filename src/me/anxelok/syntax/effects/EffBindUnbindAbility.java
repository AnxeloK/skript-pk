package me.anxelok.syntax.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.event.PlayerBindChangeEvent;
import com.projectkorra.projectkorra.ability.util.MultiAbilityManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import me.anxelok.Main;

@Name("Bind and Unbind Abilities")
@Description("Binds or unbinds a bending ability to/from a player's hotbar slot or clears all abilities")
@Examples({
        "bind ability \"EarthBlast\" to player's slot 5",
        "bind ability \"EarthBlast\" to player's current slot",
        "unbind player's ability \"EarthBlast\"",
        "unbind player's slot 5",
        "unbind player's all abilities"
})
@Since(Main.VERSION)
public class EffBindUnbindAbility extends Effect {
    private Expression<Player> playerExpr;
    private Expression<String> abilityExpr;
    private Expression<Integer> slotExpr;
    private boolean isBinding = false;
    private boolean clearAll = false;
    private boolean currentSlot = false;

    static {
        Skript.registerEffect(EffBindUnbindAbility.class,
                "bind ability %string% to %player%'s (slot %integer% | current slot)",
                "unbind player's (all abilities | ability %string% | slot %integer%)");
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (matchedPattern == 0 || matchedPattern == 1) {
            abilityExpr = (Expression<String>) exprs[0];
            playerExpr = (Expression<Player>) exprs[1];
            if (matchedPattern == 0) {
                slotExpr = (Expression<Integer>) exprs[2];
            } else {
                currentSlot = true; // Set current slot mode if "current slot" is used
            }
            isBinding = true; // This is a binding operation
        } else {
            playerExpr = (Expression<Player>) exprs[0];
            if (exprs.length == 2) {
                abilityExpr = (Expression<String>) exprs[1];
            }
            if (matchedPattern == 1) {
                clearAll = true; // Unbinding all abilities
            } else if (exprs.length == 3) {
                slotExpr = (Expression<Integer>) exprs[2]; // Unbinding a specific slot
            }
            isBinding = false; // This is an unbinding operation
        }
        return true;
    }

    @Override
    protected void execute(Event e) {
        Player player = playerExpr.getSingle(e);
        if (player == null) return;

        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer == null) return;

        // Binding logic
        if (isBinding) {
            String ability = abilityExpr.getSingle(e);
            if (ability == null) return;

            if (currentSlot) {
                Integer slot = bPlayer.getCurrentSlot(); // Get the current hotbar slot of the player
                bindAbilityToSlot(bPlayer, ability, slot, player);
            } else {
                Integer slot = slotExpr.getSingle(e);
                if (slot == null) return;
                bindAbilityToSlot(bPlayer, ability, slot, player);
            }
        } else { // Unbinding logic
            if (clearAll) {
                unbindAllAbilities(bPlayer, player);
            } else if (abilityExpr != null) {
                String ability = abilityExpr.getSingle(e);
                if (ability != null) {
                    unbindAbility(bPlayer, ability, player);
                }
            } else if (slotExpr != null) {
                Integer slot = slotExpr.getSingle(e);
                if (slot != null) {
                    unbindSlot(bPlayer, slot, player);
                }
            }
        }
    }

    // Bind ability to a specific slot
    private void bindAbilityToSlot(BendingPlayer bPlayer, String ability, Integer slot, Player player) {
        if (MultiAbilityManager.playerAbilities.containsKey(player)) {
            return; // Prevent binding if the player has multi-ability active
        }

        CoreAbility coreAbil = CoreAbility.getAbility(ability);
        if (coreAbil == null) return;

        PlayerBindChangeEvent event = new PlayerBindChangeEvent(player, coreAbil.getName(), slot, true, false);
        com.projectkorra.projectkorra.ProjectKorra.plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        bPlayer.getAbilities().put(slot, coreAbil.getName());
        bPlayer.saveAbility(coreAbil.getName(), slot);
    }

    // Unbind specific ability
    private void unbindAbility(BendingPlayer bPlayer, String ability, Player player) {
        for (int i = 1; i <= 9; i++) {
            if (bPlayer.getAbilities().get(i).equals(ability)) {
                PlayerBindChangeEvent event = new PlayerBindChangeEvent(player, ability, i, false, false);
                com.projectkorra.projectkorra.ProjectKorra.plugin.getServer().getPluginManager().callEvent(event);
                if (event.isCancelled()) return;

                bPlayer.getAbilities().remove(i);
                bPlayer.saveAbility(null, i);
            }
        }
    }

    // Unbind specific slot
    private void unbindSlot(BendingPlayer bPlayer, Integer slot, Player player) {
        String ability = bPlayer.getAbilities().get(slot);
        if (ability != null) {
            PlayerBindChangeEvent event = new PlayerBindChangeEvent(player, ability, slot, false, false);
            com.projectkorra.projectkorra.ProjectKorra.plugin.getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) return;

            bPlayer.getAbilities().remove(slot);
            bPlayer.saveAbility(null, slot);
        }
    }

    // Unbind all abilities
    private void unbindAllAbilities(BendingPlayer bPlayer, Player player) {
        for (int i = 1; i <= 9; i++) {
            String ability = bPlayer.getAbilities().get(i);
            if (ability != null) {
                PlayerBindChangeEvent event = new PlayerBindChangeEvent(player, ability, i, false, false);
                com.projectkorra.projectkorra.ProjectKorra.plugin.getServer().getPluginManager().callEvent(event);
                if (event.isCancelled()) continue;

                bPlayer.getAbilities().remove(i);
                bPlayer.saveAbility(null, i);
            }
        }
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        if (isBinding) {
            if (currentSlot) {
                return "bind ability " + abilityExpr.toString(e, debug) + " to " + playerExpr.toString(e, debug) + "'s current slot";
            } else {
                return "bind ability " + abilityExpr.toString(e, debug) + " to " + playerExpr.toString(e, debug) + "'s slot " + slotExpr.toString(e, debug);
            }
        } else {
            if (clearAll) {
                return "unbind player's all abilities";
            } else if (abilityExpr != null) {
                return "unbind player's ability " + abilityExpr.toString(e, debug);
            } else {
                return "unbind player's slot " + slotExpr.toString(e, debug);
            }
        }
    }
}
