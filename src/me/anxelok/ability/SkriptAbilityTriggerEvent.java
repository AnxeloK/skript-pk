package me.anxelok.ability;

import me.anxelok.ability.SkriptGeneratedAbility;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Lightweight Bukkit event used to execute Skript triggers for generated abilities.
 */
public class SkriptAbilityTriggerEvent extends Event implements Cancellable {

    public enum Phase {
        START,
        PROGRESS,
        REMOVE
    }

    private static final HandlerList HANDLERS = new HandlerList();

    private final SkriptGeneratedAbility ability;
    private final SkriptAbilityDefinition definition;
    private final Phase phase;
    private boolean cancelled;

    public SkriptAbilityTriggerEvent(SkriptGeneratedAbility ability, SkriptAbilityDefinition definition, Phase phase) {
        super(!Bukkit.isPrimaryThread());
        this.ability = ability;
        this.definition = definition;
        this.phase = phase;
    }

    public SkriptGeneratedAbility getAbility() {
        return ability;
    }

    public SkriptAbilityDefinition getDefinition() {
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
