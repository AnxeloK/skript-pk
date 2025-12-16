package me.anxelok.ability;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.classes.Serializer;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.registrations.EventValues;
import ch.njol.yggdrasil.Fields;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.Element.SubElement;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.command.CooldownCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.StreamCorruptedException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles registration and lifecycle management for dynamically generated PK abilities.
 */
public final class AbilityRegistry {

    private AbilityRegistry() {}

    private static JavaPlugin plugin;
    private static boolean initialized;
    private static boolean typesRegistered;

    private static final Map<String, RegisteredAbility> REGISTERED_BY_NAME = new ConcurrentHashMap<>();
    private static final Map<Class<? extends GeneratedAbility>, RegisteredAbility> REGISTERED_BY_CLASS = new ConcurrentHashMap<>();
    private static final Set<String> REGISTERED_COOLDOWN_TYPES = ConcurrentHashMap.newKeySet();
    private static final AtomicInteger CLASS_COUNTER = new AtomicInteger();
    private static final Field COOLDOWNS_FIELD = locateCooldownField();

    private static Map<String, CoreAbility> abilitiesByName;
    private static Map<Class<? extends CoreAbility>, CoreAbility> abilitiesByClass;

    public static synchronized void preload() {
        registerSkriptTypes();
    }

    public static synchronized void init(JavaPlugin owningPlugin) {
        if (initialized) {
            return;
        }
        plugin = Objects.requireNonNull(owningPlugin, "owningPlugin");
        fetchCoreAbilityMaps();
        registerSkriptTypes();
        initialized = true;
    }

    public static synchronized void ensureTypeRegistered() {
        registerSkriptTypes();
    }

    public static synchronized RegisteredAbility registerAbility(AbilityDefinition definition) {
        ensureInitialized();
        String key = AbilityNaming.key(definition.getName());

        CoreAbility existing = CoreAbility.getAbility(definition.getName());
        if (existing instanceof GeneratedAbility) {
            GeneratedAbility existingSkriptAbility = (GeneratedAbility) existing;
            unregisterAbility(existingSkriptAbility.getClass().asSubclass(GeneratedAbility.class));
        } else if (existing != null) {
            throw new IllegalArgumentException("Ability '" + definition.getName() + "' already exists and is not managed by Skript-PK");
        }

        AbilityClassLoader classLoader = new AbilityClassLoader(AbilityRegistry.class.getClassLoader());
        Class<? extends GeneratedAbility> abilityClass = generateAbilityClass(definition, classLoader);
        AbilityDefinitionStore.attachDefinition(abilityClass, definition);

        GeneratedAbility prototype;
        try {
            prototype = abilityClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            AbilityDefinitionStore.detachDefinition(abilityClass);
            throw new IllegalStateException("Failed to instantiate generated ability class", e);
        }

        Class<? extends CoreAbility> coreClass = abilityClass.asSubclass(CoreAbility.class);
        String canonicalName = definition.getName().toLowerCase(Locale.ENGLISH);
        abilitiesByName.put(canonicalName, (CoreAbility) prototype);
        abilitiesByClass.put(coreClass, (CoreAbility) prototype);

        if (!definition.isHidden() && definition.getCooldownMillis() > 0) {
            registerCooldownType(definition.getName());
        }

        PermissionHandle permissionHandle = ensurePermission(definition);

        RegisteredAbility registered = new RegisteredAbility(definition, abilityClass, prototype, permissionHandle, classLoader);
        REGISTERED_BY_NAME.put(key, registered);
        REGISTERED_BY_CLASS.put(abilityClass, registered);

        if (plugin != null) {
            plugin.getLogger().info("Registered ability " + definition.getName());
        }

        return registered;
    }

    public static synchronized void unregisterAbility(Class<? extends GeneratedAbility> abilityClass) {
        ensureInitialized();
        RegisteredAbility registered = REGISTERED_BY_CLASS.remove(abilityClass);
        if (registered == null) {
            return;
        }

        String key = AbilityNaming.key(registered.getDefinition().getName());
        REGISTERED_BY_NAME.remove(key);
        if (abilitiesByName != null) {
            String canonicalName = registered.getDefinition().getName().toLowerCase(Locale.ENGLISH);
            abilitiesByName.remove(canonicalName);
        }
        if (abilitiesByClass != null) {
            Class<? extends CoreAbility> coreClass = abilityClass.asSubclass(CoreAbility.class);
            abilitiesByClass.remove(coreClass);
        }

        CoreAbility.unloadAbility(abilityClass.asSubclass(CoreAbility.class));
        AbilityDefinitionStore.detachDefinition(abilityClass);

        if (registered.getPermissionHandle().created) {
            Bukkit.getPluginManager().removePermission(registered.getPermissionHandle().permission);
        }
        if (registered.getDefinition().getCooldownMillis() > 0) {
            removeCooldownType(registered.getDefinition().getName());
        }

        if (plugin != null) {
            plugin.getLogger().info("Unregistered ability " + registered.getDefinition().getName());
        }
    }

    public static synchronized void shutdown() {
        if (!initialized) {
            return;
        }

        for (Class<? extends GeneratedAbility> clazz : new ArrayList<>(REGISTERED_BY_CLASS.keySet())) {
            unregisterAbility(clazz);
        }
        REGISTERED_BY_CLASS.clear();
        REGISTERED_BY_NAME.clear();
        REGISTERED_COOLDOWN_TYPES.clear();
        plugin = null;
        initialized = false;
    }

    public static RegisteredAbility getByName(String name) {
        return REGISTERED_BY_NAME.get(AbilityNaming.key(name));
    }

    public static RegisteredAbility getByClass(Class<? extends GeneratedAbility> clazz) {
        return REGISTERED_BY_CLASS.get(clazz);
    }

    private static void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("AbilityRegistry has not been initialized yet");
        }
    }

    private static void fetchCoreAbilityMaps() {
        try {
            Field byName = CoreAbility.class.getDeclaredField("ABILITIES_BY_NAME");
            byName.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, CoreAbility> nameMap = (Map<String, CoreAbility>) byName.get(null);
            abilitiesByName = nameMap;

            Field byClass = CoreAbility.class.getDeclaredField("ABILITIES_BY_CLASS");
            byClass.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<Class<? extends CoreAbility>, CoreAbility> classMap = (Map<Class<? extends CoreAbility>, CoreAbility>) byClass.get(null);
            abilitiesByClass = classMap;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("Unable to access CoreAbility registry", e);
        }
    }

    private static void registerSkriptTypes() {
        if (typesRegistered) {
            return;
        }

        registerAbilityType("ability");
        registerAbilityType("pkability");
        registerElementType();

        EventValues.registerEventValue(AbilityTriggerEvent.class, GeneratedAbility.class, AbilityTriggerEvent::getAbility);
        EventValues.registerEventValue(AbilityTriggerEvent.class, Player.class, AbilityTriggerEvent::getPlayer);

        typesRegistered = true;
    }

    private static Class<? extends GeneratedAbility> generateAbilityClass(AbilityDefinition definition,
                                                                               AbilityClassLoader loader) {
        String baseName = sanitizeClassName(definition.getName());
        String binaryName = "me.anxelok.ability.generated." + baseName + "_" + CLASS_COUNTER.incrementAndGet();
        String internalName = binaryName.replace('.', '/');
        String superName = resolveSuperclassInternalName(definition.getElement());
        byte[] bytecode = createSubclassBytes(internalName, superName);
        Class<?> rawClass = loader.define(binaryName, bytecode);
        @SuppressWarnings("unchecked")
        Class<? extends GeneratedAbility> typed = (Class<? extends GeneratedAbility>) rawClass.asSubclass(GeneratedAbility.class);
        return typed;
    }

    private static byte[] createSubclassBytes(String internalName, String superInternalName) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);

            final int constantPoolCount = 13;

            out.writeInt(0xCAFEBABE);
            out.writeShort(0);      // minor version
            out.writeShort(49);     // major version (Java 5)
            out.writeShort(constantPoolCount);

            // 1
            writeUtf8(out, internalName);
            // 2
            writeClass(out, 1);
            // 3
            writeUtf8(out, superInternalName);
            // 4
            writeClass(out, 3);
            // 5
            writeUtf8(out, "<init>");
            // 6
            writeUtf8(out, "()V");
            // 7
            writeNameAndType(out, 5, 6);
            // 8
            writeMethodRef(out, 4, 7);
            // 9
            writeUtf8(out, "Code");
            // 10
            writeUtf8(out, "(Lorg/bukkit/entity/Player;)V");
            // 11
            writeNameAndType(out, 5, 10);
            // 12
            writeMethodRef(out, 4, 11);

            out.writeShort(0x0021); // access flags (public | super)
            out.writeShort(2);      // this_class
            out.writeShort(4);      // super_class

            out.writeShort(0);      // interfaces count
            out.writeShort(0);      // fields count
            out.writeShort(2);      // methods count

            // Default constructor ()V
            out.writeShort(0x0001); // public
            out.writeShort(5);      // name_index -> "<init>"
            out.writeShort(6);      // descriptor_index -> ()V
            out.writeShort(1);      // attributes_count
            out.writeShort(9);      // attribute_name_index -> Code
            out.writeInt(17);       // attribute_length
            out.writeShort(1);      // max_stack
            out.writeShort(1);      // max_locals
            out.writeInt(5);        // code_length
            out.writeByte(0x2A);    // aload_0
            out.writeByte(0xB7);    // invokespecial
            out.writeShort(8);      // methodref to super.<init> ()V
            out.writeByte(0xB1);    // return
            out.writeShort(0);      // exception_table_length
            out.writeShort(0);      // attributes_count

            // Player constructor (Lorg/bukkit/entity/Player;)V
            out.writeShort(0x0001); // public
            out.writeShort(5);      // name_index -> "<init>"
            out.writeShort(10);     // descriptor_index -> (Player)V
            out.writeShort(1);      // attributes_count
            out.writeShort(9);      // attribute_name_index -> Code
            out.writeInt(18);       // attribute_length
            out.writeShort(2);      // max_stack
            out.writeShort(2);      // max_locals
            out.writeInt(6);        // code_length
            out.writeByte(0x2A);    // aload_0
            out.writeByte(0x2B);    // aload_1
            out.writeByte(0xB7);    // invokespecial
            out.writeShort(12);     // methodref to super.<init>(Player)
            out.writeByte(0xB1);    // return
            out.writeShort(0);      // exception_table_length
            out.writeShort(0);      // attributes_count

            out.writeShort(0);      // class attributes count

            out.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to generate ability subclass", e);
        }
    }

    private static void writeUtf8(DataOutputStream out, String value) throws IOException {
        out.writeByte(1);
        out.writeUTF(value);
    }

    private static void writeClass(DataOutputStream out, int nameIndex) throws IOException {
        out.writeByte(7);
        out.writeShort(nameIndex);
    }

    private static void writeNameAndType(DataOutputStream out, int nameIndex, int descriptorIndex) throws IOException {
        out.writeByte(12);
        out.writeShort(nameIndex);
        out.writeShort(descriptorIndex);
    }

    private static void writeMethodRef(DataOutputStream out, int classIndex, int nameAndTypeIndex) throws IOException {
        out.writeByte(10);
        out.writeShort(classIndex);
        out.writeShort(nameAndTypeIndex);
    }

    private static String resolveSuperclassInternalName(Element element) {
        Element base = element;
        if (element instanceof Element.SubElement) {
            SubElement sub = (SubElement) element;
            if (sub.getParentElement() != null) {
                base = sub.getParentElement();
            }
        }
        if (base == Element.WATER) {
            return "me/anxelok/ability/generated/GeneratedWaterAbility";
        } else if (base == Element.EARTH) {
            return "me/anxelok/ability/generated/GeneratedEarthAbility";
        } else if (base == Element.FIRE) {
            return "me/anxelok/ability/generated/GeneratedFireAbility";
        } else if (base == Element.AIR) {
            return "me/anxelok/ability/generated/GeneratedAirAbility";
        } else if (base == Element.CHI) {
            return "me/anxelok/ability/generated/GeneratedChiAbility";
        }
        return "me/anxelok/ability/generated/GeneratedAvatarAbility";
    }

    private static PermissionHandle ensurePermission(AbilityDefinition definition) {
        PluginManager manager = Bukkit.getPluginManager();
        String node = definition.getPermissionNode();
        Permission existing = manager.getPermission(node);
        if (existing != null) {
            return new PermissionHandle(existing, false);
        }

        Permission created = new Permission(node);
        Permission parent = manager.getPermission("bending.player");
        if (parent != null) {
            created.addParent(parent, definition.isDefaultGranted());
        }
        manager.addPermission(created);
        return new PermissionHandle(created, true);
    }

    private static String sanitizeClassName(String value) {
        StringBuilder builder = new StringBuilder();
        for (char c : value.toCharArray()) {
            if (Character.isJavaIdentifierPart(c)) {
                builder.append(c);
            }
        }
        if (builder.length() == 0 || !Character.isJavaIdentifierStart(builder.charAt(0))) {
            builder.insert(0, 'A');
        }
        return builder.toString();
    }

    private static void registerAbilityType(String codeName) {
        try {
            Classes.registerClass(new ClassInfo<>(GeneratedAbility.class, codeName)
                .name("PK Ability")
                .user("pk abilities?")
                .defaultExpression(new EventValueExpression<>(GeneratedAbility.class))
                .parser(new Parser<GeneratedAbility>() {
                    @Override
                    public GeneratedAbility parse(String s, ParseContext context) {
                        if (s == null) {
                            return null;
                        }
                        CoreAbility ability = CoreAbility.getAbility(s);
                        if (ability instanceof GeneratedAbility) {
                            return (GeneratedAbility) ability;
                        }
                        // Defer resolution until runtime so abilities declared in the same
                        // script can still be parsed before they are registered.
                        return new LazyAbilityReference(s);
                    }

                    @Override
                    public String toString(GeneratedAbility ability, int flags) {
                        return ability.getName();
                    }

                    @Override
                    public String toVariableNameString(GeneratedAbility ability) {
                        return ability.getName().toLowerCase(Locale.ENGLISH);
                    }
                })
                .serializer(new Serializer<GeneratedAbility>() {
                    @Override
                    public Fields serialize(GeneratedAbility ability) throws NotSerializableException {
                        Fields fields = new Fields();
                        fields.putObject("name", ability.getName());
                        return fields;
                    }

                    @Override
                    public void deserialize(GeneratedAbility ability, Fields fields) {
                        // not used, handled by deserialize(Fields)
                    }

                    @Override
                    protected GeneratedAbility deserialize(Fields fields) throws StreamCorruptedException {
                        Object rawName = fields.getObject("name");
                        if (!(rawName instanceof String)) {
                            throw new StreamCorruptedException("Missing ability name");
                        }
                        String name = (String) rawName;
                        CoreAbility ability = CoreAbility.getAbility(name);
                        if (ability instanceof GeneratedAbility) {
                            return (GeneratedAbility) ability;
                        }
                        throw new StreamCorruptedException("Ability '" + name + "' is not available");
                    }

                    @Override
                    public boolean mustSyncDeserialization() {
                        return true;
                    }

                    @Override
                    protected boolean canBeInstantiated() {
                        return false;
                    }
                })
            );
        } catch (IllegalArgumentException ignored) {
            // Already registered
        }
    }

    private static void registerElementType() {
        try {
            Classes.registerClass(new ClassInfo<>(Element.class, "element")
                .name("PK Element")
                .user("pk elements?")
                .parser(new Parser<Element>() {
                    @Override
                    public Element parse(String s, ParseContext parseContext) {
                        return s == null ? null : Element.getElement(s);
                    }

                    @Override
                    public String toString(Element element, int flags) {
                        return element.getName();
                    }

                    @Override
                    public String toVariableNameString(Element element) {
                        return element.getName().toLowerCase(Locale.ENGLISH);
                    }
                })
            );
        } catch (IllegalArgumentException ignored) {
            // Already registered
        }
    }

    private static void registerCooldownType(String abilityName) {
        CooldownCommand.addCooldownType(abilityName);
        REGISTERED_COOLDOWN_TYPES.add(abilityName);
    }

    @SuppressWarnings("unchecked")
    private static void removeCooldownType(String abilityName) {
        REGISTERED_COOLDOWN_TYPES.remove(abilityName);
        if (COOLDOWNS_FIELD == null) {
            return;
        }
        try {
            Object value = COOLDOWNS_FIELD.get(null);
            if (value instanceof Set<?>) {
                ((Set<Object>) value).remove(abilityName);
            }
        } catch (IllegalAccessException ignored) {
            // ignore cleanup failure
        }
    }

    private static Field locateCooldownField() {
        try {
            Field field = CooldownCommand.class.getDeclaredField("COOLDOWNS");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    public static final class RegisteredAbility {
        private final AbilityDefinition definition;
        private final Class<? extends GeneratedAbility> abilityClass;
        private final GeneratedAbility prototype;
        private final PermissionHandle permissionHandle;
        private final AbilityClassLoader classLoader;

        private RegisteredAbility(AbilityDefinition definition,
                                  Class<? extends GeneratedAbility> abilityClass,
                                  GeneratedAbility prototype,
                                  PermissionHandle permissionHandle,
                                  AbilityClassLoader classLoader) {
            this.definition = definition;
            this.abilityClass = abilityClass;
            this.prototype = prototype;
            this.permissionHandle = permissionHandle;
            this.classLoader = classLoader;
        }

        public AbilityDefinition getDefinition() {
            return definition;
        }

        public Class<? extends GeneratedAbility> getAbilityClass() {
            return abilityClass;
        }

        public GeneratedAbility getPrototype() {
            return prototype;
        }

        PermissionHandle getPermissionHandle() {
            return permissionHandle;
        }

        AbilityClassLoader getClassLoader() {
            return classLoader;
        }
    }

    private static final class PermissionHandle {
        private final Permission permission;
        private final boolean created;

        private PermissionHandle(Permission permission, boolean created) {
            this.permission = permission;
            this.created = created;
        }
    }

    private static final class AbilityClassLoader extends ClassLoader {
        private AbilityClassLoader(ClassLoader parent) {
            super(parent);
        }

        Class<?> define(String name, byte[] bytecode) {
            return defineClass(name, bytecode, 0, bytecode.length);
        }
    }
}
