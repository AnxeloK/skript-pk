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

import java.util.HashMap;

@Name("Create Preset")
@Description("Creates a new empty preset for a player")
@Examples({
    "create new preset \"combo1\" for player",
    "create preset \"watermoves\" for player"
})
@Since(Main.VERSION)
public class EffCreatePreset extends Effect {

    private Expression<String> nameExpr;
    private Expression<Player> playerExpr;

    static {
        Skript.registerEffect(EffCreatePreset.class,
                "create [new] preset %string% for %player%");
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        nameExpr = (Expression<String>) exprs[0];
        playerExpr = (Expression<Player>) exprs[1];
        return true;
    }

    @Override
    protected void execute(Event e) {
        String name = nameExpr.getSingle(e);
        Player player = playerExpr.getSingle(e);
        
        if (name == null || player == null) return;
        
        // Create empty preset
        new Preset(player.getUniqueId(), name, new HashMap<>());
    }

    @Override
    public String toString(Event e, boolean debug) {
        return "create new preset " + nameExpr.toString(e, debug) + " for " + playerExpr.toString(e, debug);
    }
}
