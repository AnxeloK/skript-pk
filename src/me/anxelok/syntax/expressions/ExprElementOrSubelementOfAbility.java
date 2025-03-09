package me.anxelok.syntax.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.util.Kleenean;
import com.projectkorra.projectkorra.ability.CoreAbility;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import com.projectkorra.projectkorra.ability.Ability;
import com.projectkorra.projectkorra.Element;
import ch.njol.skript.doc.*;

import me.anxelok.Main;

@Name("Element of Ability")
@Description("Gets the element of an ability. Prioritizes subelement.")
@Examples({
        "get element of ability \"Ability1\"",
        "get element of ability \"Ability2\""
})
@Since(Main.VERSION)
public class ExprElementOrSubelementOfAbility extends SimpleExpression<String> {

    private Expression<String> abilityExpr;

    static {
        Skript.registerExpression(ExprElementOrSubelementOfAbility.class, String.class,
                ch.njol.skript.lang.ExpressionType.SIMPLE,
                "get element of ability %string%");
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ch.njol.skript.lang.SkriptParser.ParseResult parseResult) {
        abilityExpr = (Expression<String>) exprs[0];
        return true;
    }

    @Nullable
    @Override
    protected String[] get(Event e) {
        String abilityName = abilityExpr.getSingle(e);
        if (abilityName == null) return new String[0];

        Ability ability = CoreAbility.getAbility(abilityName);
        if (ability == null) return new String[0];

        Element element = ability.getElement();
        if (element == null) return new String[0];

        // Return the clean name of the element (without color codes or formatting)
        return new String[] { element.getName() };
    }

    @Override
    public boolean isSingle() {
        return true;  // only one result
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "get element of ability " + abilityExpr.toString(e, debug);
    }
}
