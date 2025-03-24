package me.anxelok.syntax.effects;

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

@Name("Delete Preset")
@Description("Deletes a preset from a player")
@Examples({
    "delete preset \"combo1\" from player",
    "delete player's preset \"watermoves\""
})
@Since(Main.VERSION)
public class EffDeletePreset extends Effect {

    private Expression<String> presetNameExpr;
    private Expression<Player> playerExpr;

    static {
        Skript.registerEffect(EffDeletePreset.class,
                "delete preset %string% from %player%",
                "delete %player%'s preset %string%");
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        presetNameExpr = (Expression<String>) exprs[0];
        playerExpr = (Expression<Player>) exprs[1];
        return true;
    }

    @Override
    protected void execute(Event e) {
        String presetName = presetNameExpr.getSingle(e);
        Player player = playerExpr.getSingle(e);
        
        if (presetName == null || player == null) return;

        Preset preset = Preset.getPreset(player, presetName);
        if (preset != null) {
            preset.delete();
        }
    }

    @Override
    public String toString(Event e, boolean debug) {
        return "delete preset " + presetNameExpr.toString(e, debug) + " from " + playerExpr.toString(e, debug);
    }
}
