package net.conczin.immersive_gateways;

import net.conczin.immersive_gateways.config.Config;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class Common {
    public static final String MOD_ID = "immersive_gateways";
    public static final Logger LOGGER = LogManager.getLogger();

    public static ResourceLocation locate(String name) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, name);
    }

    public static void init() {
        // Load config to make sure it's generated
        //noinspection ResultOfMethodCallIgnored
        Config.getInstance();
    }

    public interface RegisterHelper<T> {
        void register(ResourceLocation name, T value);
    }
}
