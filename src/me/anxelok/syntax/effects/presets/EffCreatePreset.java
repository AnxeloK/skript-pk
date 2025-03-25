package me.anxelok.syntax.effects.presets;

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

import java.util.ArrayList;
import java.util.HashMap;

@Name("Create Preset")
@Description("Creates a new empty preset. Can create either a player-specific preset or an external preset.")
@Examples({
    "create new preset \"combo1\" for player",
    "create preset \"watermoves\" for player",
    "create external preset \"basic_water\""
})
@Since(Main.VERSION)
public class EffCreatePreset extends Effect {

    private Expression<String> nameExpr;
    private Expression<Player> playerExpr;
    private boolean isExternal;

    static {
        Skript.registerEffect(EffCreatePreset.class,
                "create [new] preset %string% for %player%",
                "create [new] external preset %string%");
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        nameExpr = (Expression<String>) exprs[0];
        isExternal = matchedPattern == 1;
        if (!isExternal) {
            playerExpr = (Expression<Player>) exprs[1];
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
            Preset.externalPresets.put(name.toLowerCase(), emptyList);
            Preset.config.set(name, emptyList);
        } else {
            Player player = playerExpr.getSingle(e);
            if (player == null) return;
            
            Preset existingPreset = Preset.getPreset(player, name);
            if (existingPreset != null) {
                existingPreset.delete().thenRun(() -> {
                    new Preset(player.getUniqueId(), name, new HashMap<>()).save(player);
                });
            } else {
                new Preset(player.getUniqueId(), name, new HashMap<>()).save(player);
            }
        }
    }

    @Override
    public String toString(Event e, boolean debug) {
        return "create new " + (isExternal ? "external preset " + nameExpr.toString(e, debug) 
               : "preset " + nameExpr.toString(e, debug) + " for " + playerExpr.toString(e, debug));
    }
}
