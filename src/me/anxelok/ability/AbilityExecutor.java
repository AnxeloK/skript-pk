package me.anxelok.ability;

import ch.njol.skript.lang.Trigger;
import com.projectkorra.projectkorra.ability.CoreAbility;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Objects;

import me.anxelok.ability.AbilityDefinition;
import me.anxelok.ability.AbilityDefinitionStore;
import me.anxelok.ability.AbilityTriggerEvent;
import me.anxelok.ability.GeneratedAbility;

/**
 * Shared runtime logic for generated abilities, delegated to by element-specific base classes.
 */
public final class AbilityExecutor {

    private final CoreAbility ability;
    private Location currentLocation;
    private boolean runningRemovePhase;

    public AbilityExecutor(CoreAbility ability) {
        this.ability = Objects.requireNonNull(ability, "ability");
    }

    public AbilityDefinition getDefinition() {
        return AbilityDefinitionStore.requireDefinition(ability.getClass());
    }

    public void initializePrototype() {
        ability.setHiddenAbility(getDefinition().isHidden());
    }

    public void initializeForPlayer(Player player) {
        AbilityDefinition definition = getDefinition();
        ability.setHiddenAbility(definition.isHidden());

        if (player == null || ability.getBendingPlayer() == null) {
            return;
        }
        if (!ability.getBendingPlayer().canBend(ability)) {
            return;
        }
        if (ability.getBendingPlayer().isOnCooldown(ability)) {
            return;
        }

        currentLocation = player.getLocation();
        ability.start();
        if (ability.isRemoved()) {
            return;
        }

        if (!executePhase(AbilityTriggerEvent.Phase.START, definition)) {
            ability.remove();
            return;
        }

        if (ability.isRemoved()) {
            return;
        }

        if (definition.getCooldownMillis() > 0) {
            ability.getBendingPlayer().addCooldown(ability);
        }

        if (!definition.hasProgressTrigger()) {
            ability.remove();
        }
    }

    public void progress() {
        AbilityDefinition definition = getDefinition();
        Player player = ability.getPlayer();
        if (player == null || !player.isOnline()) {
            ability.remove();
            return;
        }
        if (!definition.hasProgressTrigger()) {
            ability.remove();
            return;
        }
        if (!executePhase(AbilityTriggerEvent.Phase.PROGRESS, definition)) {
            ability.remove();
        }
    }

    public void remove(Runnable superRemove) {
        AbilityDefinition definition = getDefinition();
        if (ability.getPlayer() != null && !ability.isRemoved() && !runningRemovePhase && definition.hasRemoveTrigger()) {
            runningRemovePhase = true;
            try {
                executePhase(AbilityTriggerEvent.Phase.REMOVE, definition);
            } finally {
                runningRemovePhase = false;
            }
        }
        superRemove.run();
        currentLocation = null;
    }

    public Location resolveLocation(Location fallback) {
        if (currentLocation != null) {
            return currentLocation;
        }
        Player player = ability.getPlayer();
        if (player != null) {
            return player.getLocation();
        }
        return fallback;
    }

    public void setCurrentLocation(Location location) {
        this.currentLocation = location != null ? location.clone() : null;
    }

    private boolean executePhase(AbilityTriggerEvent.Phase phase, AbilityDefinition definition) {
        Trigger trigger;
        switch (phase) {
            case START:
                trigger = definition.getStartTrigger();
                break;
            case PROGRESS:
                trigger = definition.getProgressTrigger();
                break;
            case REMOVE:
                trigger = definition.getRemoveTrigger();
                break;
            default:
                trigger = null;
        }

        if (trigger == null) {
            return phase != AbilityTriggerEvent.Phase.PROGRESS;
        }

        AbilityTriggerEvent event = new AbilityTriggerEvent(
            (GeneratedAbility) ability, definition, phase
        );
        boolean success = trigger.execute(event);
        return success && !event.isCancelled();
    }
}
