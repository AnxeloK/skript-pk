package me.anxelok.syntax.effects.abilities;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import me.anxelok.ability.AbilityRegistry;
import me.anxelok.ability.GeneratedAbility;
import me.anxelok.ability.LazyAbilityReference;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import me.anxelok.Main;
import com.projectkorra.projectkorra.ability.CoreAbility;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.Nullable;

@Name("Set Ability Location")
@Description("Sets the anchor location for a running ability instance generated through this addon, allowing you to track it independently from the player.")
@Examples({
    "# anchor the active event ability to the player's location",
    "set location of event-ability to location of player",
    "# move a stored ability instance to a marker location",
    "set location of {_ability} to {_marker}"
})
@Since(Main.VERSION)
public class EffSetAbilityLocation extends Effect {

    static {
        AbilityRegistry.ensureTypeRegistered();
        Skript.registerEffect(EffSetAbilityLocation.class, "set location of %ability% to %location%");
    }

    private Expression<GeneratedAbility> abilityExpression;
    private Expression<Location> locationExpression;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean kleenean, ParseResult parseResult) {
        abilityExpression = (Expression<GeneratedAbility>) expressions[0];
        locationExpression = (Expression<Location>) expressions[1];
        return true;
    }

    @Override
    protected void execute(Event event) {
        GeneratedAbility ability = abilityExpression.getSingle(event);
        Location location = locationExpression.getSingle(event);
        if (ability == null || location == null) {
            return;
        }

        ability = resolveAbility(ability, event);
        if (ability == null) {
            return;
        }

        ability.setCurrentLocation(location.clone());
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "set location of " + abilityExpression.toString(event, debug) + " to " + locationExpression.toString(event, debug);
    }

    private @Nullable GeneratedAbility resolveAbility(GeneratedAbility ability, Event event) {
        if (!(ability instanceof LazyAbilityReference)) {
            return ability;
        }

        LazyAbilityReference ref = (LazyAbilityReference) ability;
        GeneratedAbility resolved = ref.resolve();
        if (resolved != null) {
            return resolved;
        }

        Player player = resolvePlayer(event);
        Class<? extends GeneratedAbility> clazz = ref.resolveClass();
        if (player != null && clazz != null) {
            CoreAbility running = CoreAbility.getAbility(player, clazz.asSubclass(CoreAbility.class));
            if (running instanceof GeneratedAbility) {
                return (GeneratedAbility) running;
            }
        }
        return null;
    }

    private @Nullable Player resolvePlayer(Event event) {
        if (event instanceof me.anxelok.ability.AbilityTriggerEvent) {
            return ((me.anxelok.ability.AbilityTriggerEvent) event).getPlayer();
        }
        if (event instanceof PlayerEvent) {
            return ((PlayerEvent) event).getPlayer();
        }
        return null;
    }
}
