package me.anxelok.syntax.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.projectkorra.projectkorra.object.Preset;
import me.anxelok.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Event;

@Name("Delete External Preset")
@Description("Deletes an external preset")
@Examples({
    "delete external preset \"basic\"",
})
@Since(Main.VERSION)
public class EffDeleteExternalPreset extends Effect {

    private Expression<String> presetNameExpr;

    static {
        Skript.registerEffect(EffDeleteExternalPreset.class,
                "delete external preset %string%");
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        presetNameExpr = (Expression<String>) exprs[0];
        return true;
    }

    @Override
    protected void execute(Event e) {
        String presetName = presetNameExpr.getSingle(e);
        if (presetName == null) return;

        FileConfiguration config = Preset.config;
        
        // Remove from config if exists
        if (config.contains(presetName.toLowerCase())) {
            config.set(presetName.toLowerCase(), null);
            try {
                config.save("presets.yml");
                Preset.loadExternalPresets(); // Reload presets
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public String toString(Event e, boolean debug) {
        return "delete external preset " + presetNameExpr.toString(e, debug);
    }
}
