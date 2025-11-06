package me.anxelok.syntax.structures;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.util.Timespan;
import ch.njol.skript.config.SectionNode;
import com.projectkorra.projectkorra.Element;
import me.anxelok.Main;
import me.anxelok.ability.SkriptAbilityDefinition;
import me.anxelok.ability.SkriptAbilityRegistry;
import me.anxelok.ability.SkriptAbilityRegistry.RegisteredAbility;
import me.anxelok.ability.SkriptAbilityTriggerEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.lang.entry.util.LiteralEntryData;
import org.skriptlang.skript.lang.structure.Structure;
import org.skriptlang.skript.lang.script.Script;

@Name("Ability")
@Description("Defines a ProjectKorra ability implemented in Skript.")
@Examples({
    "ability AirBurst",
    "    element: Air",
    "    description: air ability",
    "    instructions: Left click to use air burst",
    "    author: AnxeloK",
    "    cooldown: 3 seconds",
    "",
    "    start:",
    "        send \"Started Air Burst\" to player",
    "    progress:",
    "        # optional looping logic",
    "    remove:",
    "        send \"Air Burst ended\" to player",
    "",
    "on left click:",
    "    start ability \"AirBurst\" for player"
})
@Since(Main.VERSION)
public class StructAbility extends Structure {

    static {
        Skript.registerStructure(
            StructAbility.class,
            EntryValidator.builder()
                .addEntry("element", null, false)
                .addEntry("description", "", true)
                .addEntry("instructions", "", true)
                .addEntry("author", "Unknown", true)
                .addEntry("version", "1.0.0", true)
                .addEntryData(new LiteralEntryData<>("cooldown", null, true, Timespan.class))
                .addEntryData(new LiteralEntryData<>("sneak ability", false, true, Boolean.class))
                .addEntryData(new LiteralEntryData<>("hidden", false, true, Boolean.class))
                .addEntryData(new LiteralEntryData<>("harmless", false, true, Boolean.class))
                .addEntryData(new LiteralEntryData<>("ignite ability", false, true, Boolean.class))
                .addEntryData(new LiteralEntryData<>("explosive ability", false, true, Boolean.class))
                .addEntryData(new LiteralEntryData<>("default permission", true, true, Boolean.class))
                .addSection("start", false)
                .addSection("progress", true)
                .addSection("remove", true)
                .build(),
            "ability <.+>"
        );
    }

    private EntryContainer entryContainer;
    private RegisteredAbility registeredAbility;
    private String abilityName;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, ParseResult parseResult, @Nullable EntryContainer entryContainer) {
        if (entryContainer == null) {
            Skript.error("Ability structure requires a body");
            return false;
        }
        this.entryContainer = entryContainer;
        return true;
    }

    @Override
    public boolean load() {
        SectionNode node = entryContainer.getSource();
        String header = node.getKey();
        if (header == null) {
            Skript.error("Ability header is missing");
            return false;
        }

        header = ScriptLoader.replaceOptions(header).trim();
        if (!header.regionMatches(true, 0, "ability", 0, "ability".length())) {
            Skript.error("Invalid ability header syntax: " + header);
            return false;
        }

        String namePart = header.substring("ability".length()).trim();
        if (namePart.isEmpty()) {
            Skript.error("Invalid ability header syntax: " + header);
            return false;
        }

        abilityName = namePart;
        String elementName = entryContainer.get("element", String.class, true);
        Element element = Element.getElement(elementName);
        if (element == null) {
            Skript.error("Unknown element '" + elementName + "' for ability " + abilityName);
            return false;
        }

        Timespan cooldownSpan = entryContainer.getOptional("cooldown", Timespan.class, false);
        long cooldownMillis = cooldownSpan != null ? cooldownSpan.getMilliSeconds() : 0L;

        boolean sneak = entryContainer.getOptional("sneak ability", Boolean.class, false);
        boolean hidden = entryContainer.getOptional("hidden", Boolean.class, false);
        boolean harmless = entryContainer.getOptional("harmless", Boolean.class, false);
        boolean ignite = entryContainer.getOptional("ignite ability", Boolean.class, false);
        boolean explosive = entryContainer.getOptional("explosive ability", Boolean.class, false);
        boolean defaultPermission = entryContainer.getOptional("default permission", Boolean.class, false);

        String description = entryContainer.get("description", String.class, true);
        String instructions = entryContainer.get("instructions", String.class, true);
        String author = entryContainer.get("author", String.class, true);
        String version = entryContainer.get("version", String.class, true);

        SectionNode startSection = entryContainer.get("start", SectionNode.class, false);
        SectionNode progressSection = entryContainer.getOptional("progress", SectionNode.class, false);
        SectionNode removeSection = entryContainer.getOptional("remove", SectionNode.class, false);

        Trigger startTrigger = createTrigger(abilityName + " start", startSection);
        if (startTrigger == null) {
            return false;
        }
        Trigger progressTrigger = progressSection != null ? createTrigger(abilityName + " progress", progressSection) : null;
        Trigger removeTrigger = removeSection != null ? createTrigger(abilityName + " remove", removeSection) : null;

        if (progressSection != null && progressTrigger == null) {
            return false;
        }
        if (removeSection != null && removeTrigger == null) {
            return false;
        }

        Script script = getParser().getCurrentScript();
        SkriptAbilityDefinition.Builder builder = SkriptAbilityDefinition.builder(abilityName, element, script, startTrigger)
            .description(description)
            .instructions(instructions)
            .author(author)
            .version(version)
            .cooldownMillis(cooldownMillis)
            .sneakAbility(sneak)
            .hidden(hidden)
            .harmless(harmless)
            .ignite(ignite)
            .explosive(explosive)
            .defaultGranted(defaultPermission)
            .progressTrigger(progressTrigger)
            .removeTrigger(removeTrigger);

        try {
            registeredAbility = SkriptAbilityRegistry.registerAbility(builder.build());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            Skript.error(ex.getMessage());
            return false;
        }

        return true;
    }

    private Trigger createTrigger(String label, SectionNode section) {
        ParserInstance parser = getParser();
        parser.setCurrentEvent("ability", SkriptAbilityTriggerEvent.class);
        try {
            Trigger trigger = new Trigger(parser.getCurrentScript(), label, new SimpleEvent(), ScriptLoader.loadItems(section));
            trigger.setLineNumber(section.getLine());
            return trigger;
        } catch (Exception ex) {
            Skript.error("Failed to compile trigger for ability " + abilityName + ": " + (ex.getMessage() != null ? ex.getMessage() : ex.toString()));
            return null;
        } finally {
            parser.deleteCurrentEvent();
        }
    }

    @Override
    public void unload() {
        if (registeredAbility != null) {
            SkriptAbilityRegistry.unregisterAbility(registeredAbility.getAbilityClass());
            registeredAbility = null;
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "ability structure";
    }
}
