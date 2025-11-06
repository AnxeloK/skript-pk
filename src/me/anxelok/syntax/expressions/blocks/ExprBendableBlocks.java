package me.anxelok.syntax.expressions.blocks;

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
import com.projectkorra.projectkorra.util.BlockSource;
import com.projectkorra.projectkorra.util.BlockSource.BlockSourceType;
import com.projectkorra.projectkorra.util.ClickType;
import me.anxelok.Main;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Name("Bendable Blocks")
@Description("Returns recent bendable block sources for a player's element using ProjectKorra's BlockSource logic.")
@Examples({
    "set {_blocks::*} to bendable blocks of \"earth\" for player"
})
@Since(Main.VERSION)
public class ExprBendableBlocks extends SimpleExpression<Block> {

    static {
        Skript.registerExpression(
            ExprBendableBlocks.class,
            Block.class,
            ExpressionType.COMBINED,
            "[the] bendable blocks of %string% for %player% [within %-number% [blocks]]"
        );
    }

    private static final Map<Element, Set<BlockSourceType>> SOURCE_MAP = new HashMap<>();

    static {
        register(Element.WATER, type("WATER"), type("ICE"), type("PLANT"), type("SNOW"), type("MUD"));
        register(Element.EARTH, type("EARTH"), type("METAL"), type("LAVA"), type("MUD"));
    }

    private static BlockSourceType type(String name) {
        try {
            return BlockSourceType.valueOf(name);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static void register(Element element, BlockSourceType... types) {
        if (element == null) {
            return;
        }
        EnumSet<BlockSourceType> set = EnumSet.noneOf(BlockSourceType.class);
        if (types != null) {
            for (BlockSourceType type : types) {
                if (type != null) {
                    set.add(type);
                }
            }
        }
        SOURCE_MAP.put(element, set);
    }

    private Expression<String> elementNameExpr;
    private Expression<Player> playerExpr;
    private @Nullable Expression<Number> rangeExpr;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean kleenean, ParseResult parseResult) {
        elementNameExpr = (Expression<String>) expressions[0];
        playerExpr = (Expression<Player>) expressions[1];
        rangeExpr = (Expression<Number>) expressions[2];
        return true;
    }

    @Override
    protected Block[] get(Event event) {
        String elementName = elementNameExpr.getSingle(event);
        Player player = playerExpr.getSingle(event);
        if (elementName == null || player == null) {
            return new Block[0];
        }

        Element element = Element.getElement(elementName);
        if (element == null) {
            Skript.error("Unknown element '" + elementName + "' while retrieving bendable blocks.");
            return new Block[0];
        }

        Element baseElement = element;
        if (element instanceof Element.SubElement) {
            Element.SubElement subElement = (Element.SubElement) element;
            if (subElement.getParentElement() != null) {
                baseElement = subElement.getParentElement();
            }
        }

        Set<BlockSourceType> types = SOURCE_MAP.get(baseElement);
        if (types == null || types.isEmpty()) {
            return new Block[0];
        }

        double range = Double.MAX_VALUE;
        if (rangeExpr != null) {
            Number number = rangeExpr.getSingle(event);
            if (number != null) {
                range = Math.max(0D, number.doubleValue());
            }
        }

        List<Block> result = new ArrayList<>();
        for (BlockSourceType type : types) {
            for (ClickType clickType : new ClickType[]{ClickType.LEFT_CLICK, ClickType.SHIFT_DOWN}) {
                Block block = BlockSource.getSourceBlock(player, range, type, clickType);
                if (block != null && !result.contains(block)) {
                    result.add(block);
                }
            }
        }

        return result.toArray(new Block[0]);
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends Block> getReturnType() {
        return Block.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        String range = rangeExpr != null ? " within " + rangeExpr.toString(event, debug) : "";
        return "bendable blocks of " + elementNameExpr.toString(event, debug) + " for " + playerExpr.toString(event, debug) + range;
    }
}
