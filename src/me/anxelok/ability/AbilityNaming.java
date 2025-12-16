package me.anxelok.ability;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Centralized helpers for producing consistent identifiers for abilities
 * across permissions, maps, and generated class names.
 */
public final class AbilityNaming {

    private static final Pattern NON_SLUG = Pattern.compile("[^a-z0-9]+");

    private AbilityNaming() {
        // utility
    }

    /**
     * Returns a lowercase slug that is safe for map keys and permission nodes.
     */
    public static String key(String name) {
        if (name == null) {
            return "ability";
        }
        String slug = NON_SLUG.matcher(name.toLowerCase(Locale.ENGLISH).trim()).replaceAll("-");
        slug = trimHyphens(slug);
        return slug.isEmpty() ? "ability" : slug;
    }

    /**
     * Builds a permission node for the provided ability name.
     */
    public static String permissionNode(String name) {
        return "bending.ability." + key(name);
    }

    private static String trimHyphens(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && value.charAt(start) == '-') {
            start++;
        }
        while (end > start && value.charAt(end - 1) == '-') {
            end--;
        }
        return value.substring(start, end);
    }
}
