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
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.ability.ElementalAbility;
import com.projectkorra.projectkorra.ability.WaterAbility;
import me.anxelok.Main;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Block Is Bendable")
@Description("Checks if a block is bendable for a given ProjectKorra element (ignores player permissions).")
@Examples({
    "if target block is bendable for earth:",
    "    send \"That block can be earthbent\""
})
@Since(Main.VERSION)
public class ExprBendableBlocks extends SimpleExpression<Boolean> {

    static {
        Skript.registerExpression(
            ExprBendableBlocks.class,
            Boolean.class,
            ExpressionType.SIMPLE,
            "%block% is bendable for %element%"
        );
    }

    private Expression<Block> blockExpr;
    private Expression<Element> elementExpr;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean kleenean, ParseResult parseResult) {
        blockExpr = (Expression<Block>) expressions[0];
        elementExpr = (Expression<Element>) expressions[1];
        return true;
    }

    @Override
    protected Boolean[] get(Event event) {
        Block block = blockExpr.getSingle(event);
        Element element = elementExpr.getSingle(event);
        if (block == null || element == null) {
            return new Boolean[]{false};
        }

        Element baseElement = element;
        if (element instanceof Element.SubElement) {
            Element.SubElement subElement = (Element.SubElement) element;
            if (subElement.getParentElement() != null) {
                baseElement = subElement.getParentElement();
            }
        }

        boolean bendable = isBendable(baseElement, block);
        return new Boolean[]{bendable};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Boolean> getReturnType() {
        return Boolean.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return blockExpr.toString(event, debug) + " is bendable for " + elementExpr.toString(event, debug);
    }

    private boolean isBendable(Element baseElement, Block block) {
        final org.bukkit.Material mat = block.getType();
        if (baseElement == Element.WATER) {
            return ElementalAbility.isWater(block)
                || ElementalAbility.isIce(block)
                || ElementalAbility.isPlant(block)
                || WaterAbility.isSnow(block)
                || isMud(mat);
        } else if (baseElement == Element.EARTH) {
            return ElementalAbility.isEarth(mat)
                || ElementalAbility.isMetal(mat)
                || ElementalAbility.isSand(mat)
                || ElementalAbility.isLava(block)
                || isMud(mat);
        } else if (baseElement == Element.FIRE) {
            return ElementalAbility.isFire(block) || ElementalAbility.isLava(block);
        } else if (baseElement == Element.AIR) {
            return ElementalAbility.isAir(mat);
        } else if (baseElement == Element.CHI) {
            return false;
        } else if (baseElement == Element.AVATAR) {
            return true;
        }
        return false;
    }

    private boolean isMud(org.bukkit.Material material) {
        if (material == null) {
            return false;
        }
        String name = material.name();
        return name.contains("MUD");
    }
}
