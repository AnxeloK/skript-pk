package me.anxelok.syntax.effects.tempblock;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;
import ch.njol.skript.aliases.ItemType;
import com.projectkorra.projectkorra.util.TempBlock;
import me.anxelok.Main;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@Name("Revert Temp Block")
@Description("Reverts a ProjectKorra TempBlock at a location, optionally specifying the default material.")
@Examples({
    "# revert a temporary block at a stored location",
    "revert temporary block at {_loc}",
    "# revert the block at the player's feet and force it to water",
    "revert temporary block at player to water"
})
@Since(Main.VERSION)
public class EffRevertTempBlock extends Effect {

    static {
        Skript.registerEffect(
            EffRevertTempBlock.class,
            "revert [the] temp[orary] block at %location% [to %-itemtype%]"
        );
    }

    private Expression<Location> locationExpression;
    private @Nullable Expression<ItemType> materialExpression;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean kleenean, ParseResult parseResult) {
        locationExpression = (Expression<Location>) expressions[0];
        materialExpression = (Expression<ItemType>) expressions[1];
        return true;
    }

    @Override
    protected void execute(Event event) {
        Location location = locationExpression.getSingle(event);
        if (location == null) {
            return;
        }

        Material defaultMaterial = Material.AIR;
        if (materialExpression != null) {
            ItemType type = materialExpression.getSingle(event);
            if (type != null) {
                ItemStack stack = type.getRandom();
                if (stack != null && stack.getType().isBlock()) {
                    defaultMaterial = stack.getType();
                } else if (stack != null) {
                    Skript.warning(stack.getType().name().toLowerCase() + " is not a placeable block; defaulting to air.");
                }
            }
        }

        Block block = location.getBlock();
        TempBlock.revertBlock(block, defaultMaterial);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (materialExpression != null) {
            return "revert temporary block at " + locationExpression.toString(event, debug) + " to " + materialExpression.toString(event, debug);
        }
        return "revert temporary block at " + locationExpression.toString(event, debug);
    }
}
