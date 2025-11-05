package me.anxelok.ability;

import ch.njol.skript.lang.Trigger;
import com.projectkorra.projectkorra.Element;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.script.Script;

/**
 * Immutable description of a Skript-PK defined ProjectKorra ability, including metadata and compiled triggers.
 */
public final class SkriptAbilityDefinition {

    private final String name;
    private final Element element;
    private final boolean sneakAbility;
    private final boolean hidden;
    private final boolean harmless;
    private final boolean ignite;
    private final boolean explosive;
    private final boolean defaultGranted;
    private final long cooldownMillis;
    private final String description;
    private final String instructions;
    private final String author;
    private final String version;
    private final Trigger startTrigger;
    private final @Nullable Trigger progressTrigger;
    private final @Nullable Trigger removeTrigger;
    private final Script script;

    private SkriptAbilityDefinition(Builder builder) {
        this.name = builder.name;
        this.element = builder.element;
        this.sneakAbility = builder.sneakAbility;
        this.hidden = builder.hidden;
        this.harmless = builder.harmless;
        this.ignite = builder.ignite;
        this.explosive = builder.explosive;
        this.defaultGranted = builder.defaultGranted;
        this.cooldownMillis = builder.cooldownMillis;
        this.description = builder.description;
        this.instructions = builder.instructions;
        this.author = builder.author;
        this.version = builder.version;
        this.startTrigger = builder.startTrigger;
        this.progressTrigger = builder.progressTrigger;
        this.removeTrigger = builder.removeTrigger;
        this.script = builder.script;
    }

    public String getName() {
        return name;
    }

    public Element getElement() {
        return element;
    }

    public boolean isSneakAbility() {
        return sneakAbility;
    }

    public boolean isHidden() {
        return hidden;
    }

    public boolean isHarmless() {
        return harmless;
    }

    public boolean isIgniteAbility() {
        return ignite;
    }

    public boolean isExplosiveAbility() {
        return explosive;
    }

    public boolean isDefaultGranted() {
        return defaultGranted;
    }

    public long getCooldownMillis() {
        return cooldownMillis;
    }

    public String getDescription() {
        return description;
    }

    public String getInstructions() {
        return instructions;
    }

    public String getAuthor() {
        return author;
    }

    public String getVersion() {
        return version;
    }

    public Trigger getStartTrigger() {
        return startTrigger;
    }

    public @Nullable Trigger getProgressTrigger() {
        return progressTrigger;
    }

    public boolean hasProgressTrigger() {
        return progressTrigger != null;
    }

    public @Nullable Trigger getRemoveTrigger() {
        return removeTrigger;
    }

    public boolean hasRemoveTrigger() {
        return removeTrigger != null;
    }

    public Script getScript() {
        return script;
    }

    public String getPermissionNode() {
        return "bending.ability." + name;
    }

    public static Builder builder(String name, Element element, Script script, Trigger startTrigger) {
        return new Builder(name, element, script, startTrigger);
    }

    public static final class Builder {
        private final String name;
        private final Element element;
        private final Script script;
        private final Trigger startTrigger;

        private boolean sneakAbility;
        private boolean hidden;
        private boolean harmless;
        private boolean ignite;
        private boolean explosive;
        private boolean defaultGranted = true;
        private long cooldownMillis;
        private String description = "";
        private String instructions = "";
        private String author = "Unknown";
        private String version = "1.0.0";
        private Trigger progressTrigger;
        private Trigger removeTrigger;

        private Builder(String name, Element element, Script script, Trigger startTrigger) {
            this.name = name;
            this.element = element;
            this.script = script;
            this.startTrigger = startTrigger;
        }

        public Builder sneakAbility(boolean value) {
            this.sneakAbility = value;
            return this;
        }

        public Builder hidden(boolean value) {
            this.hidden = value;
            return this;
        }

        public Builder harmless(boolean value) {
            this.harmless = value;
            return this;
        }

        public Builder ignite(boolean value) {
            this.ignite = value;
            return this;
        }

        public Builder explosive(boolean value) {
            this.explosive = value;
            return this;
        }

        public Builder defaultGranted(boolean value) {
            this.defaultGranted = value;
            return this;
        }

        public Builder cooldownMillis(long value) {
            this.cooldownMillis = Math.max(value, 0);
            return this;
        }

        public Builder description(String value) {
            this.description = value;
            return this;
        }

        public Builder instructions(String value) {
            this.instructions = value;
            return this;
        }

        public Builder author(String value) {
            this.author = value;
            return this;
        }

        public Builder version(String value) {
            this.version = value;
            return this;
        }

        public Builder progressTrigger(@Nullable Trigger trigger) {
            this.progressTrigger = trigger;
            return this;
        }

        public Builder removeTrigger(@Nullable Trigger trigger) {
            this.removeTrigger = trigger;
            return this;
        }

        public SkriptAbilityDefinition build() {
            return new SkriptAbilityDefinition(this);
        }
    }
}
