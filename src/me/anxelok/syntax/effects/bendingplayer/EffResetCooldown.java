package me.anxelok.syntax.effects.cooldown;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import com.projectkorra.projectkorra.BendingPlayer;
import me.anxelok.Main;
import me.anxelok.syntax.cooldown.CooldownHelper;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Name("Reset Cooldown")
@Description("Clears a ProjectKorra cooldown for a player, or removes every active cooldown when using the \"all\" variant.")
@Examples({
    "reset cooldown \"WaterManipulation\" for player",
    "reset all cooldowns of victim"
})
@Since(Main.VERSION)
public class EffResetCooldown extends Effect {

    static {
        Skript.registerEffect(
            EffResetCooldown.class,
            "reset cooldown %string% (of|for) %player%",
            "reset all cooldowns (of|for) %player%"
        );
    }

    private Expression<String> nameExpr;
    private Expression<Player> playerExpr;
    private boolean all;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean kleenean, ParseResult parseResult) {
        if (matchedPattern == 0) {
            nameExpr = (Expression<String>) expressions[0];
            playerExpr = (Expression<Player>) expressions[1];
            all = false;
        } else {
            playerExpr = (Expression<Player>) expressions[0];
            all = true;
        }
        return true;
    }

    @Override
    protected void execute(Event event) {
        Player player = playerExpr.getSingle(event);
        if (player == null) {
            return;
        }

        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer == null) {
            return;
        }

        if (all) {
            Map<String, com.projectkorra.projectkorra.util.Cooldown> cooldowns = bPlayer.getCooldowns();
            if (cooldowns == null || cooldowns.isEmpty()) {
                return;
            }
            List<String> keys = new ArrayList<>(cooldowns.keySet());
            for (String key : keys) {
                bPlayer.removeCooldown(key);
            }
            return;
        }

        String requested = nameExpr.getSingle(event);
        if (requested == null) {
            return;
        }
        String resolved = CooldownHelper.resolveCooldown(requested);
        if (resolved == null) {
            Skript.error("Unknown ProjectKorra cooldown '" + requested + "'.");
            return;
        }

        if (CooldownHelper.isAllToken(resolved)) {
            Map<String, com.projectkorra.projectkorra.util.Cooldown> cooldowns = bPlayer.getCooldowns();
            if (cooldowns == null || cooldowns.isEmpty()) {
                return;
            }
            List<String> keys = new ArrayList<>(cooldowns.keySet());
            for (String key : keys) {
                bPlayer.removeCooldown(key);
            }
            return;
        }

        bPlayer.removeCooldown(resolved);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (all) {
            return "reset all cooldowns of " + playerExpr.toString(event, debug);
        }
        return "reset cooldown " + nameExpr.toString(event, debug) + " of " + playerExpr.toString(event, debug);
    }
}
