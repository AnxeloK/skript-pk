package me.anxelok;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("[Skript-PK] Enabling Skript-PK...");

        if (getServer().getPluginManager().getPlugin("Skript") == null) {
            getLogger().severe("[Skript-PK] ERROR: Skript not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (getServer().getPluginManager().getPlugin("ProjectKorra") == null) {
            getLogger().severe("[Skript-PK] ERROR: ProjectKorra not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("[Skript-PK] Registering ability structure...");

        // Register all Skript syntax via the Skript addon system.
        getLogger().info("[Skript-PK] Registering Skript syntax...");
        SkriptAddon addon = Skript.registerAddon(this);

        try {
            // Load all classes within the "me.anxelok.syntax" package and its subpackages.
            addon.loadClasses("me.anxelok.syntax");
        } catch (IOException error) {
            error.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("[Skript-PK] Disabling Skript-PK...");
    }
}
