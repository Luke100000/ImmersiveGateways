package net.conczin.immersive_gateways.config;

import net.conczin.immersive_gateways.Common;

import java.util.Map;

public final class Config extends JsonConfig {
    private static final Config INSTANCE = loadOrCreate(new Config(), Config.class);

    public Config() {
        super(Common.MOD_ID);
    }

    public static Config getInstance() {
        return INSTANCE;
    }

    @Override
    int getVersion() {
        return 0;
    }

    public int minDistance = 1024;
    public int maxDistance = 8096;
    public boolean onlyPlayersCanTeleport = true;

    public Map<String, Integer> colors = Map.<String, Integer>ofEntries(
            Map.entry("minecraft:plains", 0x7fb238),
            Map.entry("minecraft:desert", 0xf7e9a3),
            Map.entry("minecraft:forest", 0x599011),
            Map.entry("minecraft:taiga", 0x4d694b),
            Map.entry("minecraft:swamp", 0x6db015),
            Map.entry("minecraft:jungle", 0x337322),
            Map.entry("minecraft:savanna", 0xd87f33),
            Map.entry("minecraft:badlands", 0xba6d2c),
            Map.entry("minecraft:snowy_tundra", 0xa0a0ff),
            Map.entry("minecraft:mountains", 0x4f684e),
            Map.entry("minecraft:beach", 0xf7e9a3),
            Map.entry("minecraft:ocean", 0x4a80ff),
            Map.entry("minecraft:river", 0x5cdbd5),
            Map.entry("minecraft:nether_wastes", 0x8e2020),
            Map.entry("minecraft:the_end", 0x8a8adc),
            Map.entry("minecraft:mushroom_fields", 0x7f3653),
            Map.entry("minecraft:dark_forest", 0x0a1601),
            Map.entry("minecraft:birch_forest", 0x4e6b3b),
            Map.entry("minecraft:snowy_mountains", 0xa0a0ff),
            Map.entry("minecraft:flower_forest", 0xf27fa5),
            Map.entry("minecraft:lukewarm_ocean", 0x6699d8),
            Map.entry("minecraft:cold_ocean", 0x5884ba),
            Map.entry("minecraft:deep_ocean", 0x24357d),
            Map.entry("minecraft:wooded_badlands_plateau", 0xba6d2c),
            Map.entry("minecraft:sunflower_plains", 0xfaee4d),
            Map.entry("minecraft:the_void", 0x2f2a47),
            Map.entry("minecraft:snowy_plains", 0xe7f0ff),
            Map.entry("minecraft:ice_spikes", 0x9ed4ff),
            Map.entry("minecraft:mangrove_swamp", 0x2f8f46),
            Map.entry("minecraft:pale_garden", 0xf2ffd1),
            Map.entry("minecraft:old_growth_birch_forest", 0x6fbf7a),
            Map.entry("minecraft:old_growth_pine_taiga", 0x244a32),
            Map.entry("minecraft:old_growth_spruce_taiga", 0x1f3a2a),
            Map.entry("minecraft:snowy_taiga", 0xc5e3d7),
            Map.entry("minecraft:savanna_plateau", 0xf0a94c),
            Map.entry("minecraft:windswept_hills", 0x86b2a1),
            Map.entry("minecraft:windswept_gravelly_hills", 0xa8a8b2),
            Map.entry("minecraft:windswept_forest", 0x39572c),
            Map.entry("minecraft:windswept_savanna", 0xe3a834),
            Map.entry("minecraft:sparse_jungle", 0x58a73e),
            Map.entry("minecraft:bamboo_jungle", 0x2dd47c),
            Map.entry("minecraft:eroded_badlands", 0xe8893a),
            Map.entry("minecraft:wooded_badlands", 0xd47834),
            Map.entry("minecraft:meadow", 0x7fe38e),
            Map.entry("minecraft:cherry_grove", 0xff9ecf),
            Map.entry("minecraft:grove", 0xc0eed8),
            Map.entry("minecraft:snowy_slopes", 0xf4fbff),
            Map.entry("minecraft:frozen_peaks", 0xc6f2ff),
            Map.entry("minecraft:jagged_peaks", 0x9ec9e8),
            Map.entry("minecraft:stony_peaks", 0xbec5d1),
            Map.entry("minecraft:frozen_river", 0x7cd0ff),
            Map.entry("minecraft:snowy_beach", 0xf9f5de),
            Map.entry("minecraft:stony_shore", 0x8b8f96),
            Map.entry("minecraft:warm_ocean", 0x2edce0),
            Map.entry("minecraft:deep_lukewarm_ocean", 0x4e96cc),
            Map.entry("minecraft:deep_cold_ocean", 0x3b6fa5),
            Map.entry("minecraft:frozen_ocean", 0x8ac8ff),
            Map.entry("minecraft:deep_frozen_ocean", 0x5684d1),
            Map.entry("minecraft:dripstone_caves", 0xc08c5e),
            Map.entry("minecraft:lush_caves", 0x3cdc6b),
            Map.entry("minecraft:deep_dark", 0x0d2030),
            Map.entry("minecraft:warped_forest", 0x20e0c3),
            Map.entry("minecraft:crimson_forest", 0xd73644),
            Map.entry("minecraft:soul_sand_valley", 0x9a877b),
            Map.entry("minecraft:basalt_deltas", 0x5e5e66),
            Map.entry("minecraft:end_highlands", 0xd4d4ff),
            Map.entry("minecraft:end_midlands", 0xc0c0f2),
            Map.entry("minecraft:small_end_islands", 0xaaaade),
            Map.entry("minecraft:end_barrens", 0x8d8dc4)
    );
}
