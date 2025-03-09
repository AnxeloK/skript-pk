package me.anxelok.syntax.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.projectkorra.projectkorra.BendingPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Current Bound Ability")
@Description("Returns the player's currently selected bound ability.")
@Examples({
        "set {_ability} to player's current bound ability",
        "if player's current bound ability is \"WaterManipulation\":",
        "    send \"You have WaterManipulation bound!\""
})
@Since("1.0")
public class ExprCurrentBoundAbility extends SimpleExpression<String> {

    private ch.njol.skript.lang.Expression<Player> playerExpr;

    static {
        Skript.registerExpression(ExprCurrentBoundAbility.class, String.class,
                ch.njol.skript.lang.ExpressionType.SIMPLE,
                "%player%'s current bound ability");
    }

    @Override
    public boolean init(ch.njol.skript.lang.Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ch.njol.skript.lang.SkriptParser.ParseResult parseResult) {
        playerExpr = (ch.njol.skript.lang.Expression<Player>) exprs[0];  // set player expression
        return true;
    }

    @Nullable
    @Override
    protected String[] get(Event e) {
        Player player = playerExpr.getSingle(e);  // get player
        if (player == null) return new String[0];  // check if player exists
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);  // get bending player
        if (bPlayer == null) return new String[0];  // check if bending player exists
        String ability = bPlayer.getBoundAbilityName();  // get bound ability
        return ability != null ? new String[]{ ability } : new String[0];  // return ability if exists
    }

    @Override
    public boolean isSingle() {
        return true;  // return single value
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;  // return String type
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return playerExpr.toString(e, debug) + "'s current bound ability";  // convert to string
    }
}
