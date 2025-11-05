package me.anxelok.ability;

import ch.njol.skript.lang.Trigger;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ElementalAbility;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base runtime implementation shared by all dynamically generated PK abilities.
 */
public abstract class SkriptGeneratedAbility extends ElementalAbility implements AddonAbility {

    private static final Map<Class<? extends SkriptGeneratedAbility>, SkriptAbilityDefinition> DEFINITIONS = new ConcurrentHashMap<>();

    private final SkriptAbilityDefinition definition;
    private Location currentLocation;
    private boolean runningRemovePhase;

    protected SkriptGeneratedAbility() {
        super(null);
        this.definition = requireDefinition();
        this.setHiddenAbility(definition.isHidden());
    }

    protected SkriptGeneratedAbility(Player player) {
        super(player);
        this.definition = requireDefinition();
        this.setHiddenAbility(definition.isHidden());

        if (player == null || this.bPlayer == null) {
            return;
        }
        if (!this.bPlayer.canBend(this)) {
            return;
        }
        if (this.bPlayer.isOnCooldown(this)) {
            return;
        }

        this.currentLocation = player.getLocation();
        if (definition.getCooldownMillis() > 0) {
            this.bPlayer.addCooldown(this);
        }

        this.start();
        if (this.isRemoved()) {
            return;
        }

        if (!executePhase(SkriptAbilityTriggerEvent.Phase.START)) {
            remove();
            return;
        }

        if (!definition.hasProgressTrigger()) {
            remove();
        }
    }

    SkriptAbilityDefinition getDefinition() {
        return definition;
    }

    private SkriptAbilityDefinition requireDefinition() {
        SkriptAbilityDefinition def = DEFINITIONS.get(getClass());
        if (def == null) {
            throw new IllegalStateException("No PK ability definition registered for " + getClass().getName());
        }
        return def;
    }

    static void attachDefinition(Class<? extends SkriptGeneratedAbility> type, SkriptAbilityDefinition definition) {
        DEFINITIONS.put(type, definition);
    }

    static void detachDefinition(Class<? extends SkriptGeneratedAbility> type) {
        DEFINITIONS.remove(type);
    }

    static SkriptAbilityDefinition definitionFor(Class<? extends SkriptGeneratedAbility> type) {
        return DEFINITIONS.get(type);
    }

    private boolean executePhase(SkriptAbilityTriggerEvent.Phase phase) {
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
            return phase != SkriptAbilityTriggerEvent.Phase.PROGRESS;
        }

        SkriptAbilityTriggerEvent event = new SkriptAbilityTriggerEvent(this, definition, phase);
        boolean success = trigger.execute(event);
        return success && !event.isCancelled();
    }

    @Override
    public void progress() {
        if (this.player == null || !this.player.isOnline()) {
            remove();
            return;
        }
        if (!definition.hasProgressTrigger()) {
            remove();
            return;
        }
        if (!executePhase(SkriptAbilityTriggerEvent.Phase.PROGRESS)) {
            remove();
        }
    }

    @Override
    public void remove() {
        if (this.player != null && !this.isRemoved() && !runningRemovePhase && definition.hasRemoveTrigger()) {
            runningRemovePhase = true;
            try {
                executePhase(SkriptAbilityTriggerEvent.Phase.REMOVE);
            } finally {
                runningRemovePhase = false;
            }
        }
        super.remove();
        this.currentLocation = null;
    }

    public void setCurrentLocation(Location location) {
        this.currentLocation = location;
    }

    @Override
    public Location getLocation() {
        if (currentLocation != null) {
            return currentLocation;
        }
        return this.player != null ? this.player.getLocation() : null;
    }

    @Override
    public String getName() {
        return definition.getName();
    }

    @Override
    public Element getElement() {
        return definition.getElement();
    }

    @Override
    public boolean isSneakAbility() {
        return definition.isSneakAbility();
    }

    @Override
    public boolean isHarmlessAbility() {
        return definition.isHarmless();
    }

    @Override
    public boolean isIgniteAbility() {
        return definition.isIgniteAbility();
    }

    @Override
    public boolean isExplosiveAbility() {
        return definition.isExplosiveAbility();
    }

    @Override
    public long getCooldown() {
        return definition.getCooldownMillis();
    }

    @Override
    public String getDescription() {
        return definition.getDescription();
    }

    @Override
    public String getInstructions() {
        return definition.getInstructions();
    }

    @Override
    public void load() {
        // No-op, handled entirely by Skript scripts.
    }

    @Override
    public void stop() {
        // No-op, handled entirely by Skript scripts.
    }

    @Override
    public String getAuthor() {
        return definition.getAuthor();
    }

    @Override
    public String getVersion() {
        return definition.getVersion();
    }

    @Override
    public boolean isDefault() {
        return definition.isDefaultGranted();
    }
}
