package me.anxelok;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import me.anxelok.ability.AbilityRegistry;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public class Main extends JavaPlugin {

    public static final String VERSION = "2.0";

    @Override
    public void onEnable() {

        getLogger().info("Enabling Skript-PK...");

        // check if Skript is installed
        if (getServer().getPluginManager().getPlugin("Skript") == null) {
            getLogger().severe("ERROR: Skript not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        // check if ProjectKorra is installed
        if (getServer().getPluginManager().getPlugin("ProjectKorra") == null) {
            getLogger().severe("ERROR: ProjectKorra not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        AbilityRegistry.init(this);

        getLogger().info("Registering ability structure...");

        // register skript syntax
        getLogger().info("Registering Skript syntax...");
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
        getLogger().info("Disabling Skript-PK...");
        AbilityRegistry.shutdown();
    }
}
