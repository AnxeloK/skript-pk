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

import java.util.ArrayList;
import java.util.List;

@Name("Save External Preset")
@Description("Saves abilities to an external preset")
@Examples({
    "save abilities {_abilities::*} as external preset \"basic\"",
})
@Since(Main.VERSION)
public class EffSaveExternalPreset extends Effect {

    private Expression<String> presetNameExpr;
    private Expression<String> abilitiesExpr;

    static {
        Skript.registerEffect(EffSaveExternalPreset.class,
                "save abilities %strings% as external preset %string%");
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        abilitiesExpr = (Expression<String>) exprs[0];
        presetNameExpr = (Expression<String>) exprs[1];
        return true;
    }

    @Override
    protected void execute(Event e) {
        String presetName = presetNameExpr.getSingle(e);
        String[] abilities = abilitiesExpr.getArray(e);
        
        if (presetName == null || abilities == null) return;

        FileConfiguration config = Preset.config;
        List<String> abilityList = new ArrayList<>();
        
        // Add abilities to list
        for (String ability : abilities) {
            if (ability != null && !ability.isEmpty()) {
                abilityList.add(ability);
            }
        }

        // Save to config
        config.set(presetName.toLowerCase(), abilityList);
        try {
            config.save("presets.yml");
            Preset.loadExternalPresets(); // Reload presets
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public String toString(Event e, boolean debug) {
        return "save abilities " + abilitiesExpr.toString(e, debug) + " as external preset " + presetNameExpr.toString(e, debug);
    }
}
