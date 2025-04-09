package com.beeny.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModConfigManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("VillagesReborn/Config");
    private static ConfigHolder<ModConfig> holder;

    public static void register() {
        if (holder != null) {
            throw new IllegalStateException("Configuration already registered!");
        }

        holder = AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);

        holder.registerSaveListener((configHolder, config) -> {
            LOGGER.info("Villages Reborn configuration saved.");
            // You can add logic here to react to config saves if needed
            return net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStopping.EVENT.invoker().onServerStopping(null); // Placeholder, adjust if needed
        });

        holder.registerLoadListener((configHolder, config) -> {
            LOGGER.info("Villages Reborn configuration loaded.");
            // You can add logic here to react to config loads if needed
             return net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStarted.EVENT.invoker().onServerStarted(null); // Placeholder, adjust if needed
        });

        LOGGER.info("Villages Reborn configuration registered.");
    }

    public static ModConfig getConfig() {
        if (holder == null) {
            // This should ideally not happen if register() is called correctly
            // during mod initialization.
             LOGGER.error("Attempted to access config before registration!");
             // Return a default config instance to prevent crashes, but log error.
             return new ModConfig();
           // throw new IllegalStateException("Attempted to access config before registration!");
        }
        return holder.getConfig();
    }

     public static ConfigHolder<ModConfig> getConfigHolder() {
        if (holder == null) {
             LOGGER.error("Attempted to access config holder before registration!");
             // Handle this case gracefully, maybe return null or throw exception
             // For now, returning null, but consider implications.
             return null;
            //throw new IllegalStateException("Attempted to access config holder before registration!");
        }
        return holder;
    }
}