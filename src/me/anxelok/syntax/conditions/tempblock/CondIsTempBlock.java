package me.anxelok.syntax.conditions.tempblock;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import com.projectkorra.projectkorra.util.TempBlock;
import me.anxelok.Main;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Is Temp Block")
@Description("Checks whether a location is occupied by a ProjectKorra TempBlock.")
@Examples({
    "# check if the target location is a temporary block",
    "if location of target is a temporary block:",
    "# revert the temp block if it exists",
    "\trevert temporary block at location of target"
})
@Since(Main.VERSION)
public class CondIsTempBlock extends Condition {

    static {
        Skript.registerCondition(
            CondIsTempBlock.class,
            "%location% is [a] temp[orary] block",
            "%location% is(n't| not) [a] temp[orary] block"
        );
    }

    private Expression<Location> locationExpression;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean kleenean, ParseResult parseResult) {
        locationExpression = (Expression<Location>) expressions[0];
        setNegated(matchedPattern == 1);
        return true;
    }

    @Override
    public boolean check(Event event) {
        Location location = locationExpression.getSingle(event);
        if (location == null) {
            return isNegated();
        }
        boolean isTemp = TempBlock.isTempBlock(location.getBlock());
        return isNegated() != isTemp;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return locationExpression.toString(event, debug) + (isNegated() ? " is not a temporary block" : " is a temporary block");
    }
}
