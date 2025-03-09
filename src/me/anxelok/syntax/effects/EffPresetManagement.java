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
import com.projectkorra.projectkorra.object.Preset;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Name("Preset Management")
@Description("Create, modify, and delete global and player presets for abilities.")
@Examples({
        "create global preset \"presetName\" with abilities \"Ability1\", \"Ability2\", \"Ability3\"",
        "create player preset \"playerPreset\" for player \"playerName\" with abilities \"Ability1\", \"Ability2\"",
        "change slot 1 of global preset \"presetName\" to \"Ability1\"",
        "change slot 1 of player preset \"playerPreset\" for player to \"Ability1\"",
        "create global preset \"presetName\" from global preset \"existingPresetName\"",
        "change name of global preset \"oldPresetName\" to \"newPresetName\"",
        "delete player preset \"playerPreset\" for player",
        "delete global preset \"presetName\""
})
@Since("1.0")
public class EffPresetManagement extends Effect {

    private Expression<String> presetNameExpr;
    private Expression<String> abilityExpr;
    private Expression<Integer> slotExpr;
    private Expression<String> playerExpr; // For player-specific presets
    private Expression<String> newPresetNameExpr; // For renaming presets
    private boolean isCreatePreset;
    private boolean isChangeSlot;
    private boolean isCreatePlayerPreset;
    private boolean isCreatePresetFromAnother;
    private boolean isRenamePreset;
    private boolean isDeletePreset;

    static {
        Skript.registerEffect(EffPresetManagement.class,
                "create (global|player) preset %string% [with abilities %strings%|from global preset %string%]",
                "change slot %integer% of (global|player) preset %string% to %string% [for %player%]",
                "change name of global preset %string% to %string%",
                "delete (player|global) preset %string% [for %player%]"
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (matchedPattern == 0) {
            presetNameExpr = (Expression<String>) exprs[0];
            abilityExpr = (Expression<String>) exprs[1];
            isCreatePreset = true;
        } else if (matchedPattern == 1) {
            presetNameExpr = (Expression<String>) exprs[0];
            playerExpr = (Expression<String>) exprs[1];
            abilityExpr = (Expression<String>) exprs[2];
            isCreatePlayerPreset = true;
        } else if (matchedPattern == 2) {
            slotExpr = (Expression<Integer>) exprs[0];
            presetNameExpr = (Expression<String>) exprs[1];
            abilityExpr = (Expression<String>) exprs[2];
            isChangeSlot = true;
        } else if (matchedPattern == 3) {
            slotExpr = (Expression<Integer>) exprs[0];
            presetNameExpr = (Expression<String>) exprs[1];
            playerExpr = (Expression<String>) exprs[2];
            abilityExpr = (Expression<String>) exprs[3];
            isChangeSlot = true;
        } else if (matchedPattern == 4) {
            presetNameExpr = (Expression<String>) exprs[0];
            String existingPresetName = (String) exprs[1].getSingle(null);
            isCreatePresetFromAnother = true;
        } else if (matchedPattern == 5) {
            String oldPresetName = (String) exprs[0].getSingle(null);
            newPresetNameExpr = (Expression<String>) exprs[1];
            isRenamePreset = true;
        } else if (matchedPattern == 6) {
            presetNameExpr = (Expression<String>) exprs[0];
            playerExpr = (Expression<String>) exprs[1];
            isDeletePreset = true;
        } else if (matchedPattern == 7) {
            presetNameExpr = (Expression<String>) exprs[0];
            isDeletePreset = true;
        }
        return true;
    }

    @Override
    protected void execute(Event e) {
        if (isCreatePreset) {
            String presetName = presetNameExpr.getSingle(e);
            String[] abilities = abilityExpr.getAll(e);
            if (presetName == null || abilities == null) return;

            // Create a map of abilities for the global preset
            Map<Integer, String> presetMap = new HashMap<>();
            for (int i = 0; i < abilities.length; i++) {
                presetMap.put(i + 1, abilities[i]); // Slot numbers start from 1
            }

            // Save the global preset in externalPresets (global storage)
            Preset.externalPresets.put(presetName, new ArrayList<>(Arrays.asList(abilities)));
        } else if (isCreatePlayerPreset) {
            String presetName = presetNameExpr.getSingle(e);
            String playerName = playerExpr.getSingle(e);
            String[] abilities = abilityExpr.getAll(e);
            if (presetName == null || playerName == null || abilities == null) return;

            // Create the player-specific preset
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                Map<Integer, String> playerPresetMap = new HashMap<>();
                for (int i = 0; i < abilities.length; i++) {
                    playerPresetMap.put(i + 1, abilities[i]);
                }

                // Use HashMap directly to match the constructor
                Preset playerPreset = new Preset(player.getUniqueId(), presetName, new HashMap<>(playerPresetMap));
                Preset.presets.put(player.getUniqueId(), Arrays.asList(playerPreset)); // Store player preset
            }
        } else if (isCreatePresetFromAnother) {
            String presetName = presetNameExpr.getSingle(e);
            String existingPresetName = abilityExpr.getSingle(e);
            if (presetName == null || existingPresetName == null) return;

            // Copy abilities from the existing preset and create a new one
            ArrayList<String> existingAbilities = Preset.externalPresets.get(existingPresetName);
            if (existingAbilities != null) {
                Preset.externalPresets.put(presetName, existingAbilities);
            }
        } else if (isChangeSlot) {
            Integer slot = slotExpr.getSingle(e);
            String presetName = presetNameExpr.getSingle(e);
            String ability = abilityExpr.getSingle(e);
            if (slot == null || presetName == null || ability == null) return;

            // Update the ability in the corresponding slot for the preset (global or player)
            if (playerExpr != null) {
                String playerName = playerExpr.getSingle(e);
                Player player = Bukkit.getPlayer(playerName);
                if (player != null) {
                    ArrayList<String> playerPresetAbilities = getPlayerPresetAbilities(player.getUniqueId(), presetName);
                    if (playerPresetAbilities != null && slot >= 1 && slot <= playerPresetAbilities.size()) {
                        playerPresetAbilities.set(slot - 1, ability);
                    }
                }
            } else {
                ArrayList<String> presetAbilities = Preset.externalPresets.get(presetName);
                if (presetAbilities != null && slot >= 1 && slot <= presetAbilities.size()) {
                    presetAbilities.set(slot - 1, ability);
                    Preset.externalPresets.put(presetName, presetAbilities); // Update the global preset
                }
            }
        } else if (isRenamePreset) {
            String oldPresetName = presetNameExpr.getSingle(e);
            String newPresetName = newPresetNameExpr.getSingle(e);
            if (oldPresetName == null || newPresetName == null) return;

            // Rename the preset in global storage
            ArrayList<String> abilities = Preset.externalPresets.get(oldPresetName);
            if (abilities != null) {
                Preset.externalPresets.put(newPresetName, abilities);
                Preset.externalPresets.remove(oldPresetName); // Remove old name
            }
        } else if (isDeletePreset) {
            String presetName = presetNameExpr.getSingle(e);
            String playerName = playerExpr != null ? playerExpr.getSingle(e) : null;
            if (presetName == null) return;

            // Delete the preset from global storage or player-specific presets
            if (playerName != null) {
                Player player = Bukkit.getPlayer(playerName);
                if (player != null) {
                    Preset.presets.get(player.getUniqueId()).removeIf(p -> p.getName().equals(presetName));
                }
            } else {
                Preset.externalPresets.remove(presetName);
            }
        }
    }

    private ArrayList<String> getPlayerPresetAbilities(UUID playerUUID, String presetName) {
        // Get the player-specific preset abilities
        for (Preset preset : Preset.presets.get(playerUUID)) {
            if (preset.getName().equals(presetName)) {
                return new ArrayList<>(preset.getAbilities().values());
            }
        }
        return null;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        if (isCreatePreset) {
            String[] abilities = abilityExpr.getAll(e);
            return "create global preset " + presetNameExpr.toString(e, debug) + " with abilities " + (abilities == null ? "null" : String.join(", ", abilities));
        } else if (isCreatePlayerPreset) {
            String[] abilities = abilityExpr.getAll(e);
            return "create player preset " + presetNameExpr.toString(e, debug) + " for player " + playerExpr.toString(e, debug) + " with abilities " + (abilities == null ? "null" : String.join(", ", abilities));
        } else if (isChangeSlot) {
            return "change slot " + slotExpr.toString(e, debug) + " of preset " + presetNameExpr.toString(e, debug) + " to " + abilityExpr.toString(e, debug);
        } else if (isRenamePreset) {
            return "rename preset " + presetNameExpr.toString(e, debug) + " to " + newPresetNameExpr.toString(e, debug);
        } else if (isDeletePreset) {
            return "delete preset " + presetNameExpr.toString(e, debug);
        }
        return "";
    }
}
