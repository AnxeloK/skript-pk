package me.anxelok.syntax.conditions.presets;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import com.projectkorra.projectkorra.object.Preset;
import me.anxelok.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Preset Exists")
@Description("Checks if a preset exists for a player")
@Examples({
    "if player has preset \"basic\":",
    "if preset \"waterbender\" exists externally:"
})
@Since(Main.VERSION)
public class CondPresetExists extends Condition {

    private Expression<Player> playerExpr;
    private Expression<String> presetNameExpr;
    private boolean external;

    static {
        Skript.registerCondition(CondPresetExists.class,
                "%player% has preset %string%",
                "preset %string% exists externally");
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        external = matchedPattern == 1;
        if (!external) {
            playerExpr = (Expression<Player>) exprs[0];
            presetNameExpr = (Expression<String>) exprs[1];
        } else {
            presetNameExpr = (Expression<String>) exprs[0];
        }
        return true;
    }

    @Override
    public boolean check(Event e) {
        String presetName = presetNameExpr.getSingle(e);
        if (presetName == null) return false;

        if (external) {
            return Preset.externalPresetExists(presetName);
        } else {
            Player player = playerExpr.getSingle(e);
            return player != null && Preset.presetExists(player, presetName);
        }
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return (external ? "preset " + presetNameExpr.toString(e, debug) + " exists externally" :
               playerExpr.toString(e, debug) + " has preset " + presetNameExpr.toString(e, debug));
    }
}
