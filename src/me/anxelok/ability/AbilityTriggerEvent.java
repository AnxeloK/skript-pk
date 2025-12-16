package me.anxelok.ability;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import me.anxelok.ability.AbilityDefinition;
import me.anxelok.ability.GeneratedAbility;

/**
 * Lightweight Bukkit event used to execute Skript triggers for generated abilities.
 */
public class AbilityTriggerEvent extends Event implements Cancellable {

    public enum Phase {
        START,
        PROGRESS,
        REMOVE
    }

    private static final HandlerList HANDLERS = new HandlerList();

    private final GeneratedAbility ability;
    private final AbilityDefinition definition;
    private final Phase phase;
    private boolean cancelled;

    public AbilityTriggerEvent(GeneratedAbility ability, AbilityDefinition definition, Phase phase) {
        super(!Bukkit.isPrimaryThread());
        this.ability = ability;
        this.definition = definition;
        this.phase = phase;
    }

    public GeneratedAbility getAbility() {
        return ability;
    }

    public AbilityDefinition getDefinition() {
        return definition;
    }

    public Phase getPhase() {
        return phase;
    }

    public Player getPlayer() {
        return ability.getPlayer();
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
