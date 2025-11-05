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

@Name("Stop PK Ability")
@Description("Ends a running Skript-PK ability. If used without an ability argument inside start/progress/remove, it stops the active ability instance.")
@Examples({
    "stop pk ability",
    "stop pk ability event-ability",
    "stop pk ability {_ability}"
})
@Since(Main.VERSION)
public class EffStopAbility extends Effect {

    static {
        me.anxelok.ability.SkriptAbilityRegistry.ensureTypeRegistered();
        Skript.registerEffect(
            EffStopAbility.class,
            "stop [pk] ability %skriptability%",
            "stop [pk] ability"
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
            Skript.warning("No PK ability available to stop.");
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
            return "stop PK ability " + abilityExpression.toString(event, debug);
        }
        return "stop current PK ability";
    }
}
