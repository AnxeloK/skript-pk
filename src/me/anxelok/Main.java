package me.anxelok;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import me.anxelok.ability.SkriptAbilityRegistry;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public class Main extends JavaPlugin {

    public static final String VERSION = "1.5";

    @Override
    public void onEnable() {

        getLogger().info("[Skript-PK] Enabling Skript-PK...");

        // check if Skript is installed
        if (getServer().getPluginManager().getPlugin("Skript") == null) {
            getLogger().severe("[Skript-PK] ERROR: Skript not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        // check if ProjectKorra is installed
        if (getServer().getPluginManager().getPlugin("ProjectKorra") == null) {
            getLogger().severe("[Skript-PK] ERROR: ProjectKorra not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        SkriptAbilityRegistry.init(this);

        getLogger().info("[Skript-PK] Registering ability structure...");

        // register skript syntax
        getLogger().info("[Skript-PK] Registering Skript syntax...");
        SkriptAddon addon = Skript.registerAddon(this);

        try {
            // load syntax classes
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
        SkriptAbilityRegistry.shutdown();
    }
}
