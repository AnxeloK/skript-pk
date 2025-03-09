package me.anxelok.syntax.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.projectkorra.projectkorra.BendingPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Toggle Bending")
@Description("Toggles a player's bending ability on/off")
@Examples({
        "toggle player's bending",
        "toggle player's bending on",
        "toggle player's bending off"
})
@Since("1.0")
public class EffToggleBending extends Effect {
    private Expression<Player> playerExpr;
    private String mode; // toggle, on, off

    static {
        // register the effect with Skript
        Skript.registerEffect(EffToggleBending.class,
                "toggle %player%'s bending",
                "toggle %player%'s bending to on",
                "toggle %player%'s bending to off");
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        playerExpr = (Expression<Player>) exprs[0];

        switch (matchedPattern) {
            case 0: mode = "toggle"; break;
            case 1: mode = "on"; break;
            case 2: mode = "off"; break;
        }
        return true;
    }

    @Override
    protected void execute(Event e) {
        Player player = playerExpr.getSingle(e);
        if (player == null) return;

        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer == null) return;

        // toggle, on, or off
        switch (mode) {
            case "toggle":
                bPlayer.toggleBending();
                break;
            case "on":
                if (!bPlayer.isToggled()) bPlayer.toggleBending();
                break;
            case "off":
                if (bPlayer.isToggled()) bPlayer.toggleBending();
                break;
        }
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        // return effect string
        return "toggle " + playerExpr.toString(e, debug) + "'s bending" +
                (mode.equals("toggle") ? "" : " to " + mode);
    }
}
