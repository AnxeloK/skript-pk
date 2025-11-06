package me.anxelok.syntax.effects.abilities;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import me.anxelok.ability.SkriptAbilityTriggerEvent;
import me.anxelok.ability.SkriptGeneratedAbility;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import me.anxelok.Main;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Stop Ability")
@Description("Ends a running ProjectKorra ability generated through this addon. When used without an argument inside start/progress/remove sections, it stops the active ability instance.")
@Examples({
    "# stop the current ability inside an ability section",
    "stop ability",
    "# stop the ability that triggered the current event",
    "stop ability event-ability",
    "# stop a stored ability instance explicitly",
    "stop ability {_ability}"
})
@Since(Main.VERSION)
public class EffStopAbility extends Effect {

    static {
        me.anxelok.ability.SkriptAbilityRegistry.ensureTypeRegistered();
        Skript.registerEffect(
            EffStopAbility.class,
            "stop ability %skriptability%",
            "stop ability"
        );
    }

    private Expression<SkriptGeneratedAbility> abilityExpression;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean kleenean, ParseResult parseResult) {
        abilityExpression = expressions.length > 0 ? (Expression<SkriptGeneratedAbility>) expressions[0] : null;
        return true;
    }

    @Override
    protected void execute(Event event) {
        SkriptGeneratedAbility ability = abilityExpression != null ? abilityExpression.getSingle(event) : resolveAbility(event);
        if (ability == null) {
            Skript.warning("No addon-managed ability available to stop.");
            return;
        }
        ability.remove();
    }

    private SkriptGeneratedAbility resolveAbility(Event event) {
        if (event instanceof SkriptAbilityTriggerEvent) {
            return ((SkriptAbilityTriggerEvent) event).getAbility();
        }
        return null;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (abilityExpression != null) {
            return "stop ability " + abilityExpression.toString(event, debug);
        }
        return "stop current ability";
    }
}
