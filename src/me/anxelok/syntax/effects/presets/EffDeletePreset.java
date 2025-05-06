package me.anxelok.syntax.effects.presets;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.projectkorra.projectkorra.object.Preset;
import me.anxelok.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

@Name("Delete Preset")
@Description({
    "Deletes a preset from a player or removes an external preset.",
    "Player presets are only deleted for that specific player.",
    "External presets are completely removed from the server."
})
@Examples({
    "# Delete a player's personal preset",
    "delete preset \"combo1\" from player",
    "",
    "# Alternative syntax for player presets",
    "delete player's preset \"watermoves\"",
    "",
    "# Delete an external preset",
    "delete external preset \"basic\"",
    "",
    "# Delete all presets matching a pattern",
    "loop all of player's presets:",
    "    if loop-value starts with \"temp_\":",
    "        delete preset loop-value from player"
})
@Since(Main.VERSION)
public class EffDeletePreset extends Effect {

    private Expression<String> presetNameExpr;
    private Expression<Player> playerExpr;
    private boolean isExternal;

    static {
        Skript.registerEffect(EffDeletePreset.class,
                "delete [player['s]] preset %string% from %player%",
                "delete %player%'s preset %string%",
                "delete external preset %string%");
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        isExternal = matchedPattern == 2;
        if (!isExternal) {
            presetNameExpr = (Expression<String>) exprs[0];
            playerExpr = (Expression<Player>) (matchedPattern == 0 ? exprs[1] : exprs[0]);
        } else {
            presetNameExpr = (Expression<String>) exprs[0];
        }
        return true;
    }

    @Override
    protected void execute(Event e) {
        String presetName = presetNameExpr.getSingle(e);
        if (presetName == null) return;

        if (isExternal) {
            FileConfiguration config = Preset.config;
            if (config.contains(presetName.toLowerCase())) {
                config.set(presetName.toLowerCase(), null);
                try {
                    config.save("presets.yml");
                    Preset.loadExternalPresets();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } else {
            Player player = playerExpr.getSingle(e);
            if (player == null) return;

            Preset preset = Preset.getPreset(player, presetName);
            if (preset != null) {
                preset.delete();
            }
        }
    }

    @Override
    public String toString(Event e, boolean debug) {
        if (isExternal) {
            return "delete external preset " + presetNameExpr.toString(e, debug);
        }
        return "delete preset " + presetNameExpr.toString(e, debug) + " from " + playerExpr.toString(e, debug);
    }
}
