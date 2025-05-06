package me.anxelok.syntax.effects.bendingplayer;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.event.PlayerBindChangeEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import me.anxelok.Main;

@Name("Clear ProjectKorra Ability Binding")
@Description("Removes any bending ability bound to a specified hotbar slot. If no slot number is given, clears the ability from the player's currently selected slot. Use this to remove unwanted abilities from a player's hotbar.")
@Examples({
    "# Remove the ability from slot 5",
    "clear player's slot 5",
    "# Clear the current selected slot",
    "clear player's slot",
    "# Reset all of a player's ability bindings",
    "loop 9 times:",
    "    clear player's slot loop-number"
})
@Since(Main.VERSION)
public class EffClearAbility extends Effect {
    private Expression<Player> playerExpr;
    private Expression<Integer> slotExpr;

    static {
        Skript.registerEffect(EffClearAbility.class,
                "clear %player%'s slot [%-integer%]");
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        playerExpr = (Expression<Player>) exprs[0];
        slotExpr = (Expression<Integer>) exprs[1];
        return true;
    }

    @Override
    protected void execute(Event e) {
        Player player = playerExpr.getSingle(e);
        if (player == null) return;

        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer == null) return;

        Integer slot = slotExpr.getSingle(e);
        if (slot == null) return;

        String ability = bPlayer.getAbilities().get(slot);
        if (ability != null) {
            PlayerBindChangeEvent event = new PlayerBindChangeEvent(player, ability, slot, false, false);
            com.projectkorra.projectkorra.ProjectKorra.plugin.getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) return;

            bPlayer.getAbilities().remove(slot);
            bPlayer.saveAbility(null, slot);
        }
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "clear " + playerExpr.toString(e, debug) + "'s slot " + slotExpr.toString(e, debug);
    }
}
