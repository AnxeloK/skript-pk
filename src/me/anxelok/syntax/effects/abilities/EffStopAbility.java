package me.anxelok.syntax.effects.abilities;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import me.anxelok.ability.AbilityTriggerEvent;
import me.anxelok.ability.GeneratedAbility;
import me.anxelok.ability.LazyAbilityReference;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import me.anxelok.Main;
import com.projectkorra.projectkorra.ability.CoreAbility;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Stop Ability")
@Description("Ends a running ProjectKorra ability generated through this addon. When used without an argument inside start/progress/remove sections, it stops the active ability instance. You can also target a specific player instance.")
@Examples({
    "# stop the current ability inside an ability section",
    "stop ability",
    "# stop the ability that triggered the current event",
    "stop ability event-ability",
    "# stop a stored ability instance explicitly",
    "stop ability {_ability}",
    "# stop another player's instance explicitly",
    "stop ability AirBlast for victim"
})
@Since(Main.VERSION)
public class EffStopAbility extends Effect {

    static {
        me.anxelok.ability.AbilityRegistry.ensureTypeRegistered();
        Skript.registerEffect(
            EffStopAbility.class,
            "stop ability %ability%",
            "stop ability",
            "stop ability %ability% for %player%"
        );
    }

    private Expression<GeneratedAbility> abilityExpression;
    private @Nullable Expression<Player> playerExpression;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean kleenean, ParseResult parseResult) {
        if (expressions.length > 0) {
            abilityExpression = (Expression<GeneratedAbility>) expressions[0];
        }
        if (expressions.length > 1) {
            playerExpression = (Expression<Player>) expressions[1];
        }
        return true;
    }

    @Override
    protected void execute(Event event) {
        Player targetPlayer = playerExpression != null ? playerExpression.getSingle(event) : resolvePlayer(event);
        GeneratedAbility ability = abilityExpression != null ? abilityExpression.getSingle(event) : resolveAbility(event);
        if (ability == null) {
            Skript.warning("No addon-managed ability available to stop.");
            return;
        }

        // If the ability is a placeholder or prototype, try to resolve a running instance for the player.
        if (ability instanceof LazyAbilityReference) {
            GeneratedAbility resolved = ((LazyAbilityReference) ability).resolve();
            if (resolved != null) {
                ability = resolved;
            } else {
                Class<? extends GeneratedAbility> clazz = ((LazyAbilityReference) ability).resolveClass();
                if (targetPlayer != null && clazz != null) {
                    CoreAbility running = CoreAbility.getAbility(targetPlayer, clazz.asSubclass(CoreAbility.class));
                    if (running instanceof GeneratedAbility) {
                        ((GeneratedAbility) running).remove();
                        return;
                    }
                }
                Skript.warning("No active ability named '" + ability.getName() + "' is running for the specified player.");
                return;
            }
        }

        // If a target player was specified (or resolved) and the ability isn't already bound to that player, try to find their running instance.
        if (targetPlayer != null && (ability.getPlayer() == null || !targetPlayer.equals(ability.getPlayer()))) {
            CoreAbility running = CoreAbility.getAbility(targetPlayer, ability.getClass().asSubclass(CoreAbility.class));
            if (running instanceof GeneratedAbility) {
                ((GeneratedAbility) running).remove();
                return;
            }
            Skript.warning("No active ability named '" + ability.getName() + "' is running for the specified player.");
            return;
        }

        ability.remove();
    }

    private GeneratedAbility resolveAbility(Event event) {
        if (event instanceof AbilityTriggerEvent) {
            return ((AbilityTriggerEvent) event).getAbility();
        }
        return null;
    }

    private @Nullable Player resolvePlayer(Event event) {
        if (event instanceof AbilityTriggerEvent) {
            return ((AbilityTriggerEvent) event).getPlayer();
        }
        if (event instanceof PlayerEvent) {
            return ((PlayerEvent) event).getPlayer();
        }
        return null;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (abilityExpression != null) {
            String base = "stop ability " + abilityExpression.toString(event, debug);
            if (playerExpression != null) {
                base += " for " + playerExpression.toString(event, debug);
            }
            return base;
        }
        return "stop current ability";
    }
}
