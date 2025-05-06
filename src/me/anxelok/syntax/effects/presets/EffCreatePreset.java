package me.anxelok.syntax.effects.presets;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.projectkorra.projectkorra.object.Preset;
import me.anxelok.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

@Name("Create Preset")
@Description({
    "Creates a new preset with optional abilities.",
    "Can create either a player-specific preset or an external preset.",
    "If a preset with the same name exists, it will be overwritten.",
    "External presets are available to all players."
})
@Examples({
    "# Create an empty preset for a player",
    "create preset \"basic\" for player",
    "",
    "# Create a preset with multiple abilities",
    "create preset \"watermoves\" for player with abilities \"WaterBlast\", \"WaterSpout\", \"WaterManipulation\"",
    "",
    "# Create an external preset available to all players",
    "create external preset \"firebender\" with abilities \"FireBlast\", \"FireJet\", \"FireShield\"",
    "",
    "# Update existing preset",
    "create preset \"combo1\" for player with abilities \"FireBlast\", \"FireBurst\""
})
@Since(Main.VERSION)
public class EffCreatePreset extends Effect {

    private Expression<String> nameExpr;
    private Expression<Player> playerExpr;
    private ExpressionList<String> abilitiesExpr;
    private boolean isExternal;

    static {
        Skript.registerEffect(EffCreatePreset.class,
                "create [new] preset %string% for %player% [with abilities %-strings%]",
                "create [new] external preset %string% [with abilities %-strings%]");
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        nameExpr = (Expression<String>) exprs[0];
        isExternal = matchedPattern == 1;
        if (!isExternal) {
            playerExpr = (Expression<Player>) exprs[1];
        }
        if (exprs.length > 2) {
            abilitiesExpr = (ExpressionList<String>) exprs[2];
        }
        return true;
    }

    @Override
    protected void execute(Event e) {
        String name = nameExpr.getSingle(e);
        if (name == null) return;

        if (isExternal) {
            if (!Preset.config.getStringList(name).isEmpty()) {
                Preset.externalPresets.remove(name.toLowerCase());
            }
            ArrayList<String> emptyList = new ArrayList<>();
            if (abilitiesExpr != null) {
                emptyList.addAll(Arrays.asList(abilitiesExpr.getAll(e)));
            }
            Preset.externalPresets.put(name.toLowerCase(), emptyList);
            Preset.config.set(name, emptyList);
        } else {
            Player player = playerExpr.getSingle(e);
            if (player == null) return;
            
            HashMap<Integer, String> abilities = new HashMap<>();
            if (abilitiesExpr != null) {
                String[] abilitiesArray = abilitiesExpr.getAll(e);
                for (int i = 0; i < abilitiesArray.length; i++) {
                    abilities.put(i + 1, abilitiesArray[i]);
                }
            }
            
            Preset existingPreset = Preset.getPreset(player, name);
            if (existingPreset != null) {
                existingPreset.delete().thenRun(() -> {
                    new Preset(player.getUniqueId(), name, abilities).save(player);
                });
            } else {
                new Preset(player.getUniqueId(), name, abilities).save(player);
            }
        }
    }

    @Override
    public String toString(Event e, boolean debug) {
        return "create new " + (isExternal ? "external preset " + nameExpr.toString(e, debug) 
               : "preset " + nameExpr.toString(e, debug) + " for " + playerExpr.toString(e, debug))
               + (abilitiesExpr != null ? " with abilities " + abilitiesExpr.toString(e, debug) : "");
    }
}
