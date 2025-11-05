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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles registration and lifecycle management for dynamically generated PK abilities.
 */
public final class SkriptAbilityRegistry {

    private SkriptAbilityRegistry() {}

    private static JavaPlugin plugin;
    private static boolean initialized;
    private static boolean typesRegistered;

    private static final AtomicInteger CLASS_COUNTER = new AtomicInteger();
    private static final AbilityClassLoader CLASS_LOADER = new AbilityClassLoader(SkriptAbilityRegistry.class.getClassLoader());

    private static final Map<String, RegisteredAbility> REGISTERED_BY_NAME = new ConcurrentHashMap<>();
    private static final Map<Class<? extends SkriptGeneratedAbility>, RegisteredAbility> REGISTERED_BY_CLASS = new ConcurrentHashMap<>();

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

    public static synchronized RegisteredAbility registerAbility(SkriptAbilityDefinition definition) {
        ensureInitialized();
        String key = normalizeName(definition.getName());

        CoreAbility existing = CoreAbility.getAbility(definition.getName());
        if (existing instanceof SkriptGeneratedAbility) {
            SkriptGeneratedAbility existingSkriptAbility = (SkriptGeneratedAbility) existing;
            unregisterAbility(existingSkriptAbility.getClass().asSubclass(SkriptGeneratedAbility.class));
        } else if (existing != null) {
            throw new IllegalArgumentException("Ability '" + definition.getName() + "' already exists and is not managed by Skript-PK");
        }

        Class<? extends SkriptGeneratedAbility> abilityClass = generateAbilityClass(definition);
        SkriptGeneratedAbility.attachDefinition(abilityClass, definition);

        SkriptGeneratedAbility prototype;
        try {
            prototype = abilityClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            SkriptGeneratedAbility.detachDefinition(abilityClass);
            throw new IllegalStateException("Failed to instantiate generated ability class", e);
        }

        abilitiesByName.put(key, prototype);
        abilitiesByClass.put(abilityClass, prototype);

        if (!definition.isHidden() && definition.getCooldownMillis() > 0) {
            CooldownCommand.addCooldownType(definition.getName());
        }

        PermissionHandle permissionHandle = ensurePermission(definition);

        RegisteredAbility registered = new RegisteredAbility(definition, abilityClass, prototype, permissionHandle);
        REGISTERED_BY_NAME.put(key, registered);
        REGISTERED_BY_CLASS.put(abilityClass, registered);

        if (plugin != null) {
            plugin.getLogger().info("[Skript-PK] Registered ability " + definition.getName());
        }

        return registered;
    }

    public static synchronized void unregisterAbility(Class<? extends SkriptGeneratedAbility> abilityClass) {
        ensureInitialized();
        RegisteredAbility registered = REGISTERED_BY_CLASS.remove(abilityClass);
        if (registered == null) {
            return;
        }

        String key = normalizeName(registered.getDefinition().getName());
        REGISTERED_BY_NAME.remove(key);
        if (abilitiesByName != null) {
            abilitiesByName.remove(key);
        }
        if (abilitiesByClass != null) {
            abilitiesByClass.remove(registered.getAbilityClass());
        }

        CoreAbility.unloadAbility(abilityClass);
        SkriptGeneratedAbility.detachDefinition(abilityClass);

        if (registered.getPermissionHandle().created) {
            Bukkit.getPluginManager().removePermission(registered.getPermissionHandle().permission);
        }

        if (plugin != null) {
            plugin.getLogger().info("[Skript-PK] Unregistered ability " + registered.getDefinition().getName());
        }
    }

    public static synchronized void shutdown() {
        if (!initialized) {
            return;
        }

        for (Class<? extends SkriptGeneratedAbility> clazz : new ArrayList<>(REGISTERED_BY_CLASS.keySet())) {
            unregisterAbility(clazz);
        }
        REGISTERED_BY_CLASS.clear();
        REGISTERED_BY_NAME.clear();
        plugin = null;
        initialized = false;
    }

    public static RegisteredAbility getByName(String name) {
        return REGISTERED_BY_NAME.get(normalizeName(name));
    }

    public static RegisteredAbility getByClass(Class<? extends SkriptGeneratedAbility> clazz) {
        return REGISTERED_BY_CLASS.get(clazz);
    }

    private static void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("SkriptAbilityRegistry has not been initialized yet");
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

        try {
            Classes.registerClass(new ClassInfo<>(SkriptGeneratedAbility.class, "skriptability")
                .name("PK Ability")
                .user("pk abilities?")
                .defaultExpression(new EventValueExpression<>(SkriptGeneratedAbility.class))
                .parser(new Parser<SkriptGeneratedAbility>() {
                    @Override
                    public SkriptGeneratedAbility parse(String s, ParseContext context) {
                        if (s == null) {
                            return null;
                        }
                        CoreAbility ability = CoreAbility.getAbility(s);
                        if (ability instanceof SkriptGeneratedAbility) {
                            return (SkriptGeneratedAbility) ability;
                        }
                        return null;
                    }

                    @Override
                    public String toString(SkriptGeneratedAbility ability, int flags) {
                        return ability.getName();
                    }

                    @Override
                    public String toVariableNameString(SkriptGeneratedAbility ability) {
                        return ability.getName().toLowerCase(Locale.ENGLISH);
                    }
                })
                .serializer(new Serializer<SkriptGeneratedAbility>() {
                    @Override
                    public Fields serialize(SkriptGeneratedAbility ability) throws NotSerializableException {
                        Fields fields = new Fields();
                        fields.putObject("name", ability.getName());
                        return fields;
                    }

                    @Override
                    public void deserialize(SkriptGeneratedAbility ability, Fields fields) {
                        // not used, handled by deserialize(Fields)
                    }

                    @Override
                    protected SkriptGeneratedAbility deserialize(Fields fields) throws StreamCorruptedException {
                        Object rawName = fields.getObject("name");
                        if (!(rawName instanceof String)) {
                            throw new StreamCorruptedException("Missing ability name");
                        }
                        String name = (String) rawName;
                        CoreAbility ability = CoreAbility.getAbility(name);
                        if (ability instanceof SkriptGeneratedAbility) {
                            return (SkriptGeneratedAbility) ability;
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

        EventValues.registerEventValue(SkriptAbilityTriggerEvent.class, SkriptGeneratedAbility.class, SkriptAbilityTriggerEvent::getAbility);
        EventValues.registerEventValue(SkriptAbilityTriggerEvent.class, Player.class, SkriptAbilityTriggerEvent::getPlayer);

        typesRegistered = true;
    }

    private static Class<? extends SkriptGeneratedAbility> generateAbilityClass(SkriptAbilityDefinition definition) {
        String baseName = sanitizeClassName(definition.getName());
        String binaryName = "me.anxelok.ability.generated." + baseName + "_" + CLASS_COUNTER.incrementAndGet();
        String internalName = binaryName.replace('.', '/');
        byte[] bytecode = createSubclassBytes(internalName);
        Class<?> rawClass = CLASS_LOADER.define(binaryName, bytecode);
        @SuppressWarnings("unchecked")
        Class<? extends SkriptGeneratedAbility> typed = (Class<? extends SkriptGeneratedAbility>) rawClass.asSubclass(SkriptGeneratedAbility.class);
        return typed;
    }

    private static byte[] createSubclassBytes(String internalName) {
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
            writeUtf8(out, "me/anxelok/ability/SkriptGeneratedAbility");
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

    private static PermissionHandle ensurePermission(SkriptAbilityDefinition definition) {
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

    private static String normalizeName(String name) {
        return name.toLowerCase(Locale.ENGLISH);
    }

    public static final class RegisteredAbility {
        private final SkriptAbilityDefinition definition;
        private final Class<? extends SkriptGeneratedAbility> abilityClass;
        private final SkriptGeneratedAbility prototype;
        private final PermissionHandle permissionHandle;

        private RegisteredAbility(SkriptAbilityDefinition definition,
                                  Class<? extends SkriptGeneratedAbility> abilityClass,
                                  SkriptGeneratedAbility prototype,
                                  PermissionHandle permissionHandle) {
            this.definition = definition;
            this.abilityClass = abilityClass;
            this.prototype = prototype;
            this.permissionHandle = permissionHandle;
        }

        public SkriptAbilityDefinition getDefinition() {
            return definition;
        }

        public Class<? extends SkriptGeneratedAbility> getAbilityClass() {
            return abilityClass;
        }

        public SkriptGeneratedAbility getPrototype() {
            return prototype;
        }

        PermissionHandle getPermissionHandle() {
            return permissionHandle;
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
