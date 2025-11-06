package me.anxelok.syntax.effects.abilities;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import com.projectkorra.projectkorra.ability.CoreAbility;
import me.anxelok.ability.SkriptGeneratedAbility;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import me.anxelok.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

@Name("Start Ability")
@Description("Launches a registered ProjectKorra ability defined through this addon for a specific player.")
@Examples({
    "start ability \"Air Gust\" for player",
    "start ability \"Toss\" for victim"
})
@Since(Main.VERSION)
public class EffStartAbility extends Effect {

    static {
        Skript.registerEffect(EffStartAbility.class, "start ability %string% for %player%");
    }

    private Expression<String> abilityNameExpression;
    private Expression<Player> playerExpression;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean kleenean, ParseResult parseResult) {
        abilityNameExpression = (Expression<String>) expressions[0];
        playerExpression = (Expression<Player>) expressions[1];
        return true;
    }

    @Override
    protected void execute(Event event) {
        String abilityName = abilityNameExpression.getSingle(event);
        Player player = playerExpression.getSingle(event);
        if (abilityName == null || player == null) {
            return;
        }

        CoreAbility coreAbility = CoreAbility.getAbility(abilityName);
        if (!(coreAbility instanceof SkriptGeneratedAbility)) {
            Skript.warning("Ability '" + abilityName + "' is not managed by this addon.");
            return;
        }

        Class<? extends SkriptGeneratedAbility> abilityClass = coreAbility.getClass().asSubclass(SkriptGeneratedAbility.class);
        try {
            Constructor<? extends SkriptGeneratedAbility> constructor = abilityClass.getDeclaredConstructor(Player.class);
            constructor.setAccessible(true);
            constructor.newInstance(player);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            Skript.error("Failed to start ability '" + abilityName + "': " + ex.getMessage());
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "start ability " + abilityNameExpression.toString(event, debug) + " for " + playerExpression.toString(event, debug);
    }
}
