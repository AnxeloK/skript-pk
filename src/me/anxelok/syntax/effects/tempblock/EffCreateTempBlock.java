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
import org.bukkit.block.data.BlockData;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@Name("Create Temp Block")
@Description("Creates a ProjectKorra TempBlock at the specified location, optionally reverting after a duration.")
@Examples({
    "# replace the player's block with stone for five seconds",
    "create temp block at location of player with stone for 5 seconds",
    "# use precomputed block data for precise states",
    "create temp block at {_loc} with blockdata {_data}",
    "# turn the victim's location into water temporarily",
    "create temp block at location of victim with water for 2 seconds"
})
@Since(Main.VERSION)
public class EffCreateTempBlock extends Effect {

    static {
        Skript.registerEffect(
            EffCreateTempBlock.class,
            "create [a] temp[orary] block at %location% with %itemtype% [for %-timespan%]",
            "create [a] temp[orary] block at %location% with blockdata %blockdata% [for %-timespan%]"
        );
    }

    private Expression<Location> locationExpression;
    private Expression<?> dataExpression;
    private @Nullable Expression<Timespan> timespanExpression;
    private int pattern;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean kleenean, ParseResult parseResult) {
        locationExpression = (Expression<Location>) expressions[0];
        dataExpression = expressions[1];
        timespanExpression = (Expression<Timespan>) expressions[2];
        pattern = matchedPattern;
        return true;
    }

    @Override
    protected void execute(Event event) {
        Location location = locationExpression.getSingle(event);
        if (location == null) {
            return;
        }

        BlockData blockData = resolveBlockData(dataExpression.getSingle(event));
        if (blockData == null) {
            Skript.error("Unable to resolve block data for temp block creation.");
            return;
        }

        long revertMillis = 0L;
        if (timespanExpression != null) {
            Timespan span = timespanExpression.getSingle(event);
            if (span != null) {
                revertMillis = Math.max(0L, span.getMilliSeconds());
            }
        }

        Block block = location.getBlock();
        new TempBlock(block, blockData, revertMillis);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "create temp block at " + locationExpression.toString(event, debug);
    }

    private @Nullable BlockData resolveBlockData(@Nullable Object input) {
        if (input == null) {
            return null;
        }

        if (pattern == 0) {
            if (!(input instanceof ItemType)) {
                return null;
            }
            ItemStack stack = ((ItemType) input).getRandom();
            if (stack == null) {
                return null;
            }
            Material material = stack.getType();
            if (!material.isBlock()) {
                Skript.error("Item " + material.name().toLowerCase() + " is not a placeable block.");
                return null;
            }
            return material.createBlockData();
        }

        if (input instanceof BlockData) {
            return (BlockData) input;
        }

        return null;
    }
}
