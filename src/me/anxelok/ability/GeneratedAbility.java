package me.anxelok.ability;

import com.projectkorra.projectkorra.ability.AddonAbility;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Marker interface for addon-generated abilities exposed to Skript.
 */
public interface GeneratedAbility extends AddonAbility {

    /**
     * Returns the immutable definition associated with this generated ability.
     */
    AbilityDefinition getDefinition();

    /**
     * Updates the logical anchor location for this ability instance.
     */
    void setCurrentLocation(Location location);

    /**
     * Ends this ability instance.
     */
    void remove();

    /**
     * @return the owning player of this ability instance.
     */
    Player getPlayer();

    /**
     * @return the current logical location of this ability.
     */
    Location getLocation();

    /**
     * @return the ProjectKorra element associated with this ability.
     */
    com.projectkorra.projectkorra.Element getElement();

    /**
     * @return ability name exposed to ProjectKorra.
     */
    String getName();
}
