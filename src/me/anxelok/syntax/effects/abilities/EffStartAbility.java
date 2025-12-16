package me.anxelok.syntax.effects.abilities;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import me.anxelok.Main;
import me.anxelok.ability.AbilityRegistry;
import me.anxelok.ability.GeneratedAbility;
import me.anxelok.ability.LazyAbilityReference;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

@Name("Start Ability")
@Description("Launches a registered ProjectKorra ability defined through this addon for a specific player.")
@Examples({
    "# trigger a custom ability for the player",
    "start ability AirGust for player",
    "# start an ability for the victim of an event",
    "start ability Toss for victim"
})
@Since(Main.VERSION)
public class EffStartAbility extends Effect {

    static {
        AbilityRegistry.ensureTypeRegistered();
        Skript.registerEffect(EffStartAbility.class, "start ability %ability% for %player%");
    }

    private Expression<GeneratedAbility> abilityExpression;
    private Expression<Player> playerExpression;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean kleenean, ParseResult parseResult) {
        abilityExpression = (Expression<GeneratedAbility>) expressions[0];
        playerExpression = (Expression<Player>) expressions[1];
        return true;
    }

    @Override
    protected void execute(Event event) {
        GeneratedAbility ability = abilityExpression.getSingle(event);
        Player player = playerExpression.getSingle(event);
        if (ability == null || player == null) {
            return;
        }

        Class<? extends GeneratedAbility> abilityClass;
        if (ability instanceof LazyAbilityReference) {
            abilityClass = ((LazyAbilityReference) ability).resolveClass();
        } else {
            abilityClass = ability.getClass().asSubclass(GeneratedAbility.class);
        }
        if (abilityClass == null) {
            Skript.error("Failed to start ability '" + ability.getName() + "': ability is not registered.");
            return;
        }
        try {
            Constructor<? extends GeneratedAbility> constructor = abilityClass.getDeclaredConstructor(Player.class);
            constructor.setAccessible(true);
            constructor.newInstance(player);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            Skript.error("Failed to start ability '" + ability.getName() + "': " + ex.getMessage());
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "start ability " + abilityExpression.toString(event, debug) + " for " + playerExpression.toString(event, debug);
    }
}
