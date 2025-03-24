package me.anxelok.syntax.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.projectkorra.projectkorra.object.Preset;
import me.anxelok.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

@Name("Preset Abilities")
@Description("Gets all abilities from a player's preset.")
@Examples("set {_abilities::*} to abilities of player's preset \"basic\"")
@Since(Main.VERSION)
public class ExprPresetAbilities extends SimpleExpression<String> {

    private ch.njol.skript.lang.Expression<Player> playerExpr;
    private ch.njol.skript.lang.Expression<String> presetNameExpr;

    static {
        Skript.registerExpression(ExprPresetAbilities.class, String.class,
                ch.njol.skript.lang.ExpressionType.SIMPLE,
                "abilities (of|from) %player%'s preset %string%");
    }

    @Override
    public boolean init(ch.njol.skript.lang.Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ch.njol.skript.lang.SkriptParser.ParseResult parseResult) {
        playerExpr = (ch.njol.skript.lang.Expression<Player>) exprs[0];
        presetNameExpr = (ch.njol.skript.lang.Expression<String>) exprs[1];
        return true;
    }

    @Override
    protected String[] get(Event e) {
        Player player = playerExpr.getSingle(e);
        String presetName = presetNameExpr.getSingle(e);
        if (player == null || presetName == null) return new String[0];

        HashMap<Integer, String> abilities = Preset.getPresetContents(player, presetName);
        if (abilities == null) return new String[0];

        return abilities.values().toArray(new String[0]);
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "abilities of " + playerExpr.toString(e, debug) + "'s preset " + presetNameExpr.toString(e, debug);
    }
}
