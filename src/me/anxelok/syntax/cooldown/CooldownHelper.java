package me.anxelok.syntax.cooldown;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.command.CooldownCommand;
import com.projectkorra.projectkorra.util.Cooldown;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Lightweight helper that mirrors ProjectKorra's cooldown command logic for Skript.
 */
public final class CooldownHelper {

    private static final String ALL_TOKEN = "*";
    private static final Field COOLDOWNS_FIELD;

    static {
        Field field = null;
        try {
            field = CooldownCommand.class.getDeclaredField("COOLDOWNS");
            field.setAccessible(true);
        } catch (ReflectiveOperationException ignored) {
            field = null;
        }
        COOLDOWNS_FIELD = field;
    }

    private CooldownHelper() {
        // utility
    }

    public static Map<String, Cooldown> getActiveCooldowns(Player player) {
        if (player == null) {
            return Collections.emptyMap();
        }

        BendingPlayer bendingPlayer = BendingPlayer.getBendingPlayer(player);
        if (bendingPlayer == null) {
            return Collections.emptyMap();
        }

        Map<String, Cooldown> source = bendingPlayer.getCooldowns();
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }

        long now = System.currentTimeMillis();
        Map<String, Cooldown> result = new LinkedHashMap<>();
        for (Map.Entry<String, Cooldown> entry : source.entrySet()) {
            Cooldown cooldown = entry.getValue();
            if (cooldown == null) {
                continue;
            }
            if (cooldown.getCooldown() > now) {
                result.put(entry.getKey(), cooldown);
            }
        }

        if (result.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(result);
    }

    public static String resolveCooldown(String name) {
        if (name == null) {
            return null;
        }

        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        if (ALL_TOKEN.equals(trimmed) || "ALL".equalsIgnoreCase(trimmed)) {
            return ALL_TOKEN;
        }

        for (String candidate : getKnownCooldowns()) {
            if (candidate.equalsIgnoreCase(trimmed)) {
                return candidate;
            }
        }

        return null;
    }

    private static Set<String> getKnownCooldowns() {
        Set<String> known = new HashSet<>();

        if (COOLDOWNS_FIELD != null) {
            try {
                Object value = COOLDOWNS_FIELD.get(null);
                if (value instanceof Set<?>) {
                    for (Object entry : (Set<?>) value) {
                        if (entry instanceof String) {
                            known.add((String) entry);
                        }
                    }
                }
            } catch (IllegalAccessException ignored) {
                // fall back to ability names
            }
        }

        Collection<CoreAbility> abilities = CoreAbility.getAbilities();
        if (abilities != null) {
            for (CoreAbility ability : abilities) {
                if (ability != null) {
                    known.add(ability.getName());
                }
            }
        }

        return known;
    }
}
