package me.anxelok.ability;

import com.projectkorra.projectkorra.ability.CoreAbility;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a reference to an ability by name that may not be registered yet.
 * This lets Skript parse ability literals before ProjectKorra has the
 * generated ability registered. Resolution is deferred until runtime.
 */
public final class LazyAbilityReference implements GeneratedAbility {

    private final String name;

    public LazyAbilityReference(String name) {
        this.name = name;
    }

    /**
     * Resolves to the registered generated ability prototype, if available.
     */
    @Nullable
    public GeneratedAbility resolve() {
        CoreAbility ability = CoreAbility.getAbility(name);
        if (ability instanceof GeneratedAbility) {
            return (GeneratedAbility) ability;
        }
        AbilityRegistry.RegisteredAbility registered = AbilityRegistry.getByName(name);
        return registered != null ? registered.getPrototype() : null;
    }

    /**
     * Resolves to the generated ability class, if available.
     */
    @Nullable
    public Class<? extends GeneratedAbility> resolveClass() {
        GeneratedAbility ability = resolve();
        if (ability != null) {
            return ability.getClass().asSubclass(GeneratedAbility.class);
        }
        AbilityRegistry.RegisteredAbility registered = AbilityRegistry.getByName(name);
        return registered != null ? registered.getAbilityClass() : null;
    }

    @Override
    public AbilityDefinition getDefinition() {
        GeneratedAbility ability = resolve();
        return ability != null ? ability.getDefinition() : null;
    }

    @Override
    public void load() {
        GeneratedAbility ability = resolve();
        if (ability instanceof com.projectkorra.projectkorra.ability.AddonAbility) {
            ((com.projectkorra.projectkorra.ability.AddonAbility) ability).load();
        }
    }

    @Override
    public void stop() {
        GeneratedAbility ability = resolve();
        if (ability instanceof com.projectkorra.projectkorra.ability.AddonAbility) {
            ((com.projectkorra.projectkorra.ability.AddonAbility) ability).stop();
        }
    }

    @Override
    public String getAuthor() {
        GeneratedAbility ability = resolve();
        return ability != null ? ability.getAuthor() : "Unknown";
    }

    @Override
    public String getVersion() {
        GeneratedAbility ability = resolve();
        return ability != null ? ability.getVersion() : "0.0.0";
    }

    @Override
    public boolean isDefault() {
        GeneratedAbility ability = resolve();
        return ability == null || ability.isDefault();
    }

    @Override
    public void setCurrentLocation(Location location) {
        GeneratedAbility ability = resolve();
        if (ability != null) {
            ability.setCurrentLocation(location);
        }
    }

    @Override
    public void remove() {
        GeneratedAbility ability = resolve();
        if (ability != null) {
            ability.remove();
        }
    }

    @Override
    public Player getPlayer() {
        GeneratedAbility ability = resolve();
        return ability != null ? ability.getPlayer() : null;
    }

    @Override
    public Location getLocation() {
        GeneratedAbility ability = resolve();
        return ability != null ? ability.getLocation() : null;
    }

    @Override
    public com.projectkorra.projectkorra.Element getElement() {
        GeneratedAbility ability = resolve();
        return ability != null ? ability.getElement() : null;
    }

    @Override
    public String getName() {
        return name;
    }
}
