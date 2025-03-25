package me.anxelok.syntax.expressions.presets;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.projectkorra.projectkorra.object.Preset;
import me.anxelok.Main;
import org.bukkit.event.Event;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Name("All Presets")
@Description("Returns all presets - either player's presets, external presets, or both")
@Examples({
    "send all presets of player to player",
    "send all external presets to player",
    "send all presets to player"
})
@Since(Main.VERSION)
public class ExprAllPresets extends SimpleExpression<String> {

    private Expression<Player> playerExpr;
    private boolean onlyExternal;

    static {
        Skript.registerExpression(ExprAllPresets.class, String.class, ExpressionType.SIMPLE,
                "all [external] presets [of %-player%]");
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        onlyExternal = parseResult.expr.contains("external");
        playerExpr = (Expression<Player>) exprs[0];
        return true;
    }

    @Override
    protected String[] get(Event e) {
        List<String> presetNames = new ArrayList<>();
        
        // Add external presets if requested
        if (onlyExternal || playerExpr == null) {
            presetNames.addAll(Preset.externalPresets.keySet());
        }
        
        // Add player presets if not only external and player is specified
        if (!onlyExternal && playerExpr != null) {
            Player player = playerExpr.getSingle(e);
            if (player != null) {
                List<Preset> playerPresets = Preset.presets.get(player.getUniqueId());
                if (playerPresets != null) {
                    for (Preset preset : playerPresets) {
                        presetNames.add(preset.getName());
                    }
                }
            }
        }
        
        return presetNames.toArray(new String[0]);
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        if (onlyExternal) {
            return "all external presets";
        } else if (playerExpr != null) {
            return "all presets of " + playerExpr.toString(e, debug);
        }
        return "all presets";
    }
}
