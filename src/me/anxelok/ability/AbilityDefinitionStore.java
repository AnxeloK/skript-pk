package me.anxelok.ability;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class AbilityDefinitionStore {

    private static final Map<Class<?>, AbilityDefinition> DEFINITIONS =
        Collections.synchronizedMap(new WeakHashMap<>());

    private AbilityDefinitionStore() {}

    public static void attachDefinition(Class<?> type, AbilityDefinition definition) {
        DEFINITIONS.put(type, definition);
    }

    public static void detachDefinition(Class<?> type) {
        DEFINITIONS.remove(type);
    }

    public static AbilityDefinition requireDefinition(Class<?> type) {
        AbilityDefinition definition = DEFINITIONS.get(type);
        if (definition == null) {
            throw new IllegalStateException("No PK ability definition registered for " + type.getName());
        }
        return definition;
    }
}
