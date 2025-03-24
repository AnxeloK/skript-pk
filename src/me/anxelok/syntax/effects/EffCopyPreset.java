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
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Name("Copy Preset")
@Description("Copies a preset from one place to another (player to player, external to player, player to external, or external to external)")
@Examples({
    "copy preset \"basic\" from player to external preset \"newbasic\"",
    "copy external preset \"water\" to player's preset \"waterstyle\"",
    "copy preset \"fire\" from player to target's preset \"firemoves\"",
    "copy external preset \"earth\" to external preset \"earthnew\""
})
@Since(Main.VERSION)
public class EffCopyPreset extends Effect {

    private Expression<String> sourceNameExpr;
    private Expression<String> targetNameExpr;
    private Expression<Player> sourcePlayerExpr;
    private Expression<Player> targetPlayerExpr;
    private int pattern;

    static {
        Skript.registerEffect(EffCopyPreset.class,
                "copy preset %string% from %player% to external preset %string%", // 0: player -> external
                "copy external preset %string% to %player%'s preset %string%", // 1: external -> player
                "copy preset %string% from %player% to %player%'s preset %string%", // 2: player -> player
                "copy external preset %string% to external preset %string%"); // 3: external -> external
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        pattern = matchedPattern;
        switch (pattern) {
            case 0: // player -> external
                sourceNameExpr = (Expression<String>) exprs[0];
                sourcePlayerExpr = (Expression<Player>) exprs[1];
                targetNameExpr = (Expression<String>) exprs[2];
                break;
            case 1: // external -> player
                sourceNameExpr = (Expression<String>) exprs[0];
                targetPlayerExpr = (Expression<Player>) exprs[1];
                targetNameExpr = (Expression<String>) exprs[2];
                break;
            case 2: // player -> player
                sourceNameExpr = (Expression<String>) exprs[0];
                sourcePlayerExpr = (Expression<Player>) exprs[1];
                targetPlayerExpr = (Expression<Player>) exprs[2];
                targetNameExpr = (Expression<String>) exprs[3];
                break;
            case 3: // external -> external
                sourceNameExpr = (Expression<String>) exprs[0];
                targetNameExpr = (Expression<String>) exprs[1];
                break;
        }
        return true;
    }

    @Override
    protected void execute(Event e) {
        String sourceName = sourceNameExpr.getSingle(e);
        String targetName = targetNameExpr.getSingle(e);
        if (sourceName == null || targetName == null) return;

        switch (pattern) {
            case 0: // player -> external
                Player sourcePlayer = sourcePlayerExpr.getSingle(e);
                if (sourcePlayer == null) return;
                copyPlayerToExternal(sourcePlayer, sourceName, targetName);
                break;
            case 1: // external -> player
                Player targetPlayer = targetPlayerExpr.getSingle(e);
                if (targetPlayer == null) return;
                copyExternalToPlayer(sourceName, targetPlayer, targetName);
                break;
            case 2: // player -> player
                Player source = sourcePlayerExpr.getSingle(e);
                Player target = targetPlayerExpr.getSingle(e);
                if (source == null || target == null) return;
                copyPlayerToPlayer(source, sourceName, target, targetName);
                break;
            case 3: // external -> external
                copyExternalToExternal(sourceName, targetName);
                break;
        }
    }

    private void copyPlayerToExternal(Player source, String sourceName, String targetName) {
        Preset sourcePreset = Preset.getPreset(source, sourceName);
        if (sourcePreset == null) return;

        List<String> abilities = new ArrayList<>();
        HashMap<Integer, String> sourceAbilities = sourcePreset.getAbilities();
        for (int i = 1; i <= 9; i++) {
            if (sourceAbilities.containsKey(i)) {
                abilities.add(sourceAbilities.get(i));
            }
        }

        FileConfiguration config = Preset.config;
        config.set(targetName.toLowerCase(), abilities);
        try {
            config.save("presets.yml");
            Preset.loadExternalPresets();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void copyExternalToPlayer(String sourceName, Player target, String targetName) {
        if (!Preset.externalPresetExists(sourceName)) return;

        HashMap<Integer, String> abilities = new HashMap<>();
        List<String> sourceAbilities = Preset.externalPresets.get(sourceName.toLowerCase());
        if (sourceAbilities != null) {
            for (int i = 0; i < sourceAbilities.size() && i < 9; i++) {
                abilities.put(i + 1, sourceAbilities.get(i));
            }
        }

        new Preset(target.getUniqueId(), targetName, abilities).save(target);
    }

    private void copyPlayerToPlayer(Player source, String sourceName, Player target, String targetName) {
        Preset sourcePreset = Preset.getPreset(source, sourceName);
        if (sourcePreset == null) return;

        new Preset(target.getUniqueId(), targetName, new HashMap<>(sourcePreset.getAbilities())).save(target);
    }

    private void copyExternalToExternal(String sourceName, String targetName) {
        if (!Preset.externalPresetExists(sourceName)) return;

        FileConfiguration config = Preset.config;
        List<String> abilities = config.getStringList(sourceName.toLowerCase());
        config.set(targetName.toLowerCase(), abilities);
        try {
            config.save("presets.yml");
            Preset.loadExternalPresets();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public String toString(Event e, boolean debug) {
        switch (pattern) {
            case 0:
                return "copy preset " + sourceNameExpr.toString(e, debug) + " from " + sourcePlayerExpr.toString(e, debug) + 
                       " to external preset " + targetNameExpr.toString(e, debug);
            case 1:
                return "copy external preset " + sourceNameExpr.toString(e, debug) + " to " + 
                       targetPlayerExpr.toString(e, debug) + "'s preset " + targetNameExpr.toString(e, debug);
            case 2:
                return "copy preset " + sourceNameExpr.toString(e, debug) + " from " + sourcePlayerExpr.toString(e, debug) + 
                       " to " + targetPlayerExpr.toString(e, debug) + "'s preset " + targetNameExpr.toString(e, debug);
            case 3:
                return "copy external preset " + sourceNameExpr.toString(e, debug) + " to external preset " + 
                       targetNameExpr.toString(e, debug);
            default:
                return "copy preset";
        }
    }
}
