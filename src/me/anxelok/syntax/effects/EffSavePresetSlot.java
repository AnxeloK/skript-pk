package me.anxelok.syntax.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.projectkorra.projectkorra.object.Preset;
import me.anxelok.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

@Name("Save Preset Slot")
@Description("Saves an ability to a specific slot in a player's preset")
@Examples({
    "save ability \"WaterBubble\" to slot 1 in player's preset \"watermoves\"",
    "save ability \"AirScooter\" to slot 5 in player's preset \"mobility\""
})
@Since(Main.VERSION)
public class EffSavePresetSlot extends Effect {

    private Expression<String> abilityExpr;
    private Expression<Number> slotExpr;
    private Expression<String> presetNameExpr;
    private Expression<Player> playerExpr;

    static {
        Skript.registerEffect(EffSavePresetSlot.class,
                "save ability %string% to slot %number% in %player%'s preset %string%");
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        abilityExpr = (Expression<String>) exprs[0];
        slotExpr = (Expression<Number>) exprs[1];
        playerExpr = (Expression<Player>) exprs[2];
        presetNameExpr = (Expression<String>) exprs[3];
        return true;
    }

    @Override
    protected void execute(Event e) {
        String ability = abilityExpr.getSingle(e);
        Number slot = slotExpr.getSingle(e);
        Player player = playerExpr.getSingle(e);
        String presetName = presetNameExpr.getSingle(e);
        
        if (ability == null || slot == null || player == null || presetName == null) return;
        if (slot.intValue() < 1 || slot.intValue() > 9) return;

        Preset preset = Preset.getPreset(player, presetName);
        if (preset == null) return;

        // Update the slot
        preset.getAbilities().put(slot.intValue(), ability);
        // Save the changes
        preset.save(player);
    }

    @Override
    public String toString(Event e, boolean debug) {
        return "save ability " + abilityExpr.toString(e, debug) + " to slot " + slotExpr.toString(e, debug) + 
               " in " + playerExpr.toString(e, debug) + "'s preset " + presetNameExpr.toString(e, debug);
    }
}
