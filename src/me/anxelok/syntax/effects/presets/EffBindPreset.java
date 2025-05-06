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
import org.jetbrains.annotations.Nullable;

@Name("Bind Preset")
@Description({
    "Binds a preset's abilities to a player.",
    "Can bind either player-specific presets or external presets.",
    "The abilities will be bound to the player's hotbar in the order they were added to the preset."
})
@Examples({
    "# Bind a player's personal preset",
    "bind preset \"watermoves\" to player",
    "",
    "# Bind an external preset",
    "bind external preset \"basic_fire\" to player",
    "",
    "# Bind presets to multiple players",
    "loop all players:",
    "    bind external preset \"starter\" to loop-player",
    "",
    "# Bind preset after creating it",
    "create preset \"combat\" for player with abilities \"FireBlast\", \"AirBlast\"",
    "bind preset \"combat\" to player"
})
@Since(Main.VERSION)
public class EffBindPreset extends Effect {

    private Expression<Player> playerExpr;
    private Expression<String> presetNameExpr;
    private boolean external;

    static {
        Skript.registerEffect(EffBindPreset.class,
                "bind [player] preset %string% to %player%",
                "bind external preset %string% to %player%");
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        external = matchedPattern == 1;
        presetNameExpr = (Expression<String>) exprs[0];
        playerExpr = (Expression<Player>) exprs[1];
        return true;
    }

    @Override
    protected void execute(Event e) {
        Player player = playerExpr.getSingle(e);
        String presetName = presetNameExpr.getSingle(e);
        if (player == null || presetName == null) return;

        if (external) {
            Preset.bindExternalPreset(player, presetName);
        } else {
            Preset preset = Preset.getPreset(player, presetName);
            if (preset != null) {
                Preset.bindPreset(player, preset);
            }
        }
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "bind " + (external ? "external " : "") + "preset " + presetNameExpr.toString(e, debug) + 
               " to " + playerExpr.toString(e, debug);
    }
}
