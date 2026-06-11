package net.conczin.immersive_gateways.compat;

import net.conczin.immersive_gateways.Common;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public class StructurifyCompat {
    private static final boolean LOADED;
    private static Method getConfig;
    private static Field disableAllStructures;
    private static Method getStructureData;
    private static Method isDisabled;

    static {
        boolean loaded = false;
        try {
            Class<?> cls = Class.forName("com.faboslav.structurify.common.Structurify");
            getConfig = cls.getMethod("getConfig");
            Class<?> configClass = getConfig.getReturnType();
            disableAllStructures = configClass.getField("disableAllStructures");
            getStructureData = configClass.getMethod("getStructureData");
            Class<?> structureDataClass = Class.forName("com.faboslav.structurify.common.config.data.StructureData");
            isDisabled = structureDataClass.getMethod("isDisabled");
            loaded = true;
        } catch (Exception e) {
            Common.LOGGER.info("Structurify not detected, skipping compatibility check.");
        }
        LOADED = loaded;
    }

    public static boolean areAllStructuresDisabled() {
        if (!LOADED) return false;
        try {
            return disableAllStructures.getBoolean(getConfig.invoke(null));
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public static boolean isStructureDisabled(ResourceLocation id) {
        if (!LOADED) return false;
        try {
            Object config = getConfig.invoke(null);
            Map<String, Object> structureData = (Map<String, Object>) getStructureData.invoke(config);
            Object data = structureData.get(id.toString());
            if (data != null) {
                return (boolean) isDisabled.invoke(data);
            }
        } catch (Exception e) {
            Common.LOGGER.warn("Failed to check Structurify config for structure {}", id, e);
        }
        return false;
    }
}
