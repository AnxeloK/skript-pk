package me.anxelok.syntax.effects.abilities;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import me.anxelok.ability.SkriptAbilityRegistry;
import me.anxelok.ability.SkriptGeneratedAbility;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import me.anxelok.Main;
import org.bukkit.Location;
import org.bukkit.event.Event;
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
        SkriptAbilityRegistry.ensureTypeRegistered();
        Skript.registerEffect(EffSetAbilityLocation.class, "set location of %skriptability% to %location%");
    }

    private Expression<SkriptGeneratedAbility> abilityExpression;
    private Expression<Location> locationExpression;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean kleenean, ParseResult parseResult) {
        abilityExpression = (Expression<SkriptGeneratedAbility>) expressions[0];
        locationExpression = (Expression<Location>) expressions[1];
        return true;
    }

    @Override
    protected void execute(Event event) {
        SkriptGeneratedAbility ability = abilityExpression.getSingle(event);
        Location location = locationExpression.getSingle(event);
        if (ability == null || location == null) {
            return;
        }
        ability.setCurrentLocation(location.clone());
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "set location of " + abilityExpression.toString(event, debug) + " to " + locationExpression.toString(event, debug);
    }
}
