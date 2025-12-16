package me.anxelok.syntax.effects.bendingplayer;

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
import me.anxelok.ability.GeneratedAbility;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import me.anxelok.Main;

@Name("Bind ProjectKorra Ability")
@Description("Assigns a ProjectKorra bending ability to a specific hotbar slot (1-9) for a player. If no slot is specified, it uses the player's currently selected hotbar slot.")
@Examples({
    "# Bind the EarthBlast ability to slot 5",
    "bind ability EarthBlast to player slot 5",
    "# Bind WaterSpout to the current hotbar slot",
    "bind ability WaterSpout to player",
    "# Create a basic bender setup",
    "bind ability AirBlast to player slot 1",
    "bind ability WaterWhip to player slot 2",
    "bind ability EarthBlast to player slot 3",
    "bind ability FireBlast to player slot 4"
})
@Since(Main.VERSION)
public class EffBindAbility extends Effect {
    private Expression<Player> playerExpr;
    private Expression<GeneratedAbility> abilityExpr;
    private Expression<Integer> slotExpr;
    private boolean currentSlot = false;

    static {
        Skript.registerEffect(EffBindAbility.class,
                "bind ability %ability% to %player% [slot %-integer%]");
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        abilityExpr = (Expression<GeneratedAbility>) exprs[0];
        playerExpr = (Expression<Player>) exprs[1];
        if (exprs.length > 2) {
            slotExpr = (Expression<Integer>) exprs[2];
        } else {
            currentSlot = true;
        }
        return true;
    }

    @Override
    protected void execute(Event e) {
        Player player = playerExpr.getSingle(e);
        if (player == null) return;

        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer == null) return;

        GeneratedAbility ability = abilityExpr.getSingle(e);
        if (ability == null) return;

        if (currentSlot) {
            Integer slot = bPlayer.getCurrentSlot();
            bindAbilityToSlot(bPlayer, ability.getName(), slot, player);
        } else {
            Integer slot = slotExpr.getSingle(e);
            if (slot == null) return;
            bindAbilityToSlot(bPlayer, ability.getName(), slot, player);
        }
    }

    private void bindAbilityToSlot(BendingPlayer bPlayer, String ability, Integer slot, Player player) {
        if (MultiAbilityManager.playerAbilities.containsKey(player)) {
            return;
        }

        CoreAbility coreAbil = CoreAbility.getAbility(ability);
        if (coreAbil == null) return;

        PlayerBindChangeEvent event = new PlayerBindChangeEvent(player, coreAbil.getName(), slot, true, false);
        com.projectkorra.projectkorra.ProjectKorra.plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        bPlayer.getAbilities().put(slot, coreAbil.getName());
        bPlayer.saveAbility(coreAbil.getName(), slot);
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        String base = "bind ability " + abilityExpr.toString(e, debug) + " to " + playerExpr.toString(e, debug);
        return currentSlot ? base : base + " slot " + slotExpr.toString(e, debug);
    }
}
