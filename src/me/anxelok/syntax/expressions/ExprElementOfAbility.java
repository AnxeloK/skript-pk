package me.anxelok.syntax.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.projectkorra.projectkorra.Element;
import me.anxelok.Main;
import me.anxelok.ability.GeneratedAbility;
import me.anxelok.ability.LazyAbilityReference;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Get Bending Element of Ability")
@Description("Returns the element (Water, Earth, Fire, Air, Chi, or sub-elements) of a specified ProjectKorra ability reference. Returns nothing if the ability isn't available.")
@Examples({
    "# Get the element of WaterSpout",
    "set {_element} to element of ability WaterSpout",
    "# Check if an ability is from a specific element",
    "if element of ability EarthBlast is earth:",
    "    send \"That's an earth ability!\""
})
@Since(Main.VERSION)
public class ExprElementOfAbility extends SimpleExpression<Element> {

    private Expression<GeneratedAbility> abilityExpr;

    static {
        Skript.registerExpression(ExprElementOfAbility.class, Element.class,
                ExpressionType.SIMPLE,
                "element of ability %ability%");
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        abilityExpr = (Expression<GeneratedAbility>) exprs[0];
        return true;
    }

    @Nullable
    @Override
    protected Element[] get(Event e) {
        GeneratedAbility ability = abilityExpr.getSingle(e);
        if (ability == null) {
            return new Element[0];
        }
        if (ability instanceof LazyAbilityReference) {
            GeneratedAbility resolved = ((LazyAbilityReference) ability).resolve();
            if (resolved != null) {
                ability = resolved;
            } else {
                return new Element[0];
            }
        }
        Element element = ability.getElement();
        if (element == null) {
            return new Element[0];
        }
        return new Element[]{element};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Element> getReturnType() {
        return Element.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "element of ability " + abilityExpr.toString(e, debug);
    }
}
