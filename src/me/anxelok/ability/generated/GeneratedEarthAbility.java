package me.anxelok.ability.generated;

import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ability.EarthAbility;
import me.anxelok.ability.AbilityDefinition;
import me.anxelok.ability.AbilityExecutor;
import me.anxelok.ability.GeneratedAbility;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public abstract class GeneratedEarthAbility extends EarthAbility implements GeneratedAbility {

    private final AbilityExecutor executor = new AbilityExecutor(this);

    protected GeneratedEarthAbility() {
        super(null);
        executor.initializePrototype();
    }

    protected GeneratedEarthAbility(Player player) {
        super(player);
        executor.initializeForPlayer(player);
    }

    @Override
    public void progress() {
        executor.progress();
    }

    @Override
    public void remove() {
        executor.remove(super::remove);
    }

    @Override
    public Location getLocation() {
        return executor.resolveLocation(null);
    }

    @Override
    public void setCurrentLocation(Location location) {
        executor.setCurrentLocation(location);
    }

    @Override
    public AbilityDefinition getDefinition() {
        return executor.getDefinition();
    }

    @Override
    public String getName() {
        return executor.getDefinition().getName();
    }

    @Override
    public boolean isSneakAbility() {
        return executor.getDefinition().isSneakAbility();
    }

    @Override
    public boolean isHarmlessAbility() {
        return executor.getDefinition().isHarmless();
    }

    @Override
    public Element getElement() {
        return executor.getDefinition().getElement();
    }

    @Override
    public boolean isIgniteAbility() {
        return executor.getDefinition().isIgniteAbility();
    }

    @Override
    public boolean isExplosiveAbility() {
        return executor.getDefinition().isExplosiveAbility();
    }

    @Override
    public long getCooldown() {
        return executor.getDefinition().getCooldownMillis();
    }

    @Override
    public String getDescription() {
        return executor.getDefinition().getDescription();
    }

    @Override
    public String getInstructions() {
        return executor.getDefinition().getInstructions();
    }

    @Override
    public String getAuthor() {
        return executor.getDefinition().getAuthor();
    }

    @Override
    public String getVersion() {
        return executor.getDefinition().getVersion();
    }

    @Override
    public boolean isDefault() {
        return executor.getDefinition().isDefaultGranted();
    }
}
