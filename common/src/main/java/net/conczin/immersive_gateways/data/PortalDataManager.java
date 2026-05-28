package net.conczin.immersive_gateways.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.conczin.immersive_gateways.Blocks;
import net.conczin.immersive_gateways.Common;
import net.conczin.immersive_gateways.Utils;
import net.conczin.immersive_gateways.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/**
 * Manages portal data storage and searching.
 * A portal is a pair of portals, each defined as a bounding box.
 */
public class PortalDataManager {
    private static final long MAX_INHABITED_TIME = 20L * 60L;
    private static final long SEARCH_ATTEMPTS = 16;
    private static final long SEARCH_FALLBACK_ATTEMPTS = 5;
    private static final int TOO_CLOSE_CHUNKS = 2;

    private static final RandomSource random = RandomSource.createThreadSafe();

    public static long toLong(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    public static PortalDataLookup getState(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(PortalDataLookup::load, PortalDataLookup::new, "immersive_gateways");
    }

    /**
     * Searches for a portal destination at the given position, or creates a new one if none is found.
     */
    public static PortalPair search(ServerLevel level, BlockPos pos, boolean generate) {
        PortalDataLookup state = getState(level);

        // Check if a known portal is nearby
        PortalPair portal = state.search(pos);

        // Create a new draft portal if none is found
        if (portal == null && generate) {
            long t = System.currentTimeMillis();

            // Pick a random position
            BlockPos target = null;
            int attempt = 0;
            for (; attempt < SEARCH_ATTEMPTS; attempt++) {
                Config c = Config.getInstance();
                float distance = random.nextFloat() * (c.maxDistance - c.minDistance) + c.minDistance;
                double angle = random.nextFloat() * Math.PI;

                // Approximate Target position
                target = new BlockPos(
                        (int) (pos.getX() + Math.cos(angle) * distance),
                        pos.getY(),
                        (int) (pos.getZ() + Math.sin(angle) * distance)
                );

                // Real resolved position
                BlockPos realTarget = placeStructure(level, target, attempt >= SEARCH_ATTEMPTS - SEARCH_FALLBACK_ATTEMPTS, attempt != SEARCH_ATTEMPTS - 1, attempt != SEARCH_ATTEMPTS - 1);
                if (realTarget != null) {
                    target = realTarget;
                    break;
                }
            }

            // Add portal and initialize search
            portal = new PortalPair(
                    new Portal(estimateBoundingBox(level, pos), getColor(level, pos)),
                    new Portal(estimateBoundingBox(level, target), getColor(level, target))
            );
            state.add(portal);

            // Logging
            long delta = System.currentTimeMillis() - t;
            Common.LOGGER.info("Portal created in {} ms using {} attempts", delta, attempt);
        }

        return portal;
    }

    public static void addManualConnection(ServerLevel level, BlockPos first, BlockPos second) {
        PortalDataLookup state = getState(level);
        state.remove(first);
        state.remove(second);

        PortalPair pair = new PortalPair(
                new Portal(estimateBoundingBox(level, first), getColor(level, first)),
                new Portal(estimateBoundingBox(level, second), getColor(level, second))
        );

        state.add(pair);
    }

    public static BlockPos placeStructure(ServerLevel level, BlockPos pos, boolean useFallback, boolean checkInhabitedTime, boolean checkWorldBorder) {
        Registry<Structure> registry = level.registryAccess().registry(Registries.STRUCTURE).orElse(null);
        if (registry == null) {
            return null;
        }

        // Prevent generating outside the world border
        if (checkWorldBorder && !level.getWorldBorder().isWithinBounds(pos)) {
            return null;
        }

        // Prevent nuking player builds
        ChunkAccess chunk = level.getChunk(pos);
        if (checkInhabitedTime && chunk.getInhabitedTime() > MAX_INHABITED_TIME) {
            return null;
        }

        // Prevent generating in already linked areas
        PortalDataLookup state = getState(level);
        for (int x = -TOO_CLOSE_CHUNKS; x <= TOO_CLOSE_CHUNKS; x++) {
            for (int z = -TOO_CLOSE_CHUNKS; z <= TOO_CLOSE_CHUNKS; z++) {
                PortalPair pair = state.search(pos.offset(x * 16, 0, z * 16));
                if (pair != null) {
                    return null;
                }
            }
        }

        // List all valid structures for target biomes
        Holder<Biome> biome = level.getBiome(pos);
        List<Structure> structures = registry
                .stream().filter(s -> {
                    ResourceLocation key = registry.getKey(s);
                    return s.biomes().contains(biome) && key != null && key.getNamespace().equals("immersive_gateways");
                }).toList();

        // If not structure works for this biome, use default
        if (structures.isEmpty() && useFallback) {
            TagKey<Structure> structureTagKey = TagKey.create(Registries.STRUCTURE, Common.locate("plains"));
            structures = registry.getTag(structureTagKey)
                    .map(t -> t.stream().map(Holder::value).toList())
                    .orElse(List.of());

            ResourceLocation biomeName = biome.unwrapKey().map(ResourceKey::location).orElse(new ResourceLocation("minecraft:unknown"));
            Common.LOGGER.info("No structure found for biome {}, using default plains structures.", biomeName);
        }

        if (structures.isEmpty()) {
            return null;
        }

        // Find the start position
        Structure structure = structures.get(random.nextInt(structures.size()));
        ChunkGenerator chunkgenerator = level.getChunkSource().getGenerator();
        StructureStart structureStart = structure.generate(
                level.registryAccess(),
                chunkgenerator,
                chunkgenerator.getBiomeSource(),
                level.getChunkSource().randomState(),
                level.getStructureManager(),
                level.getSeed(),
                new ChunkPos(pos),
                0,
                level,
                (holder) -> true
        );

        // Generate
        if (structureStart.isValid()) {
            BoundingBox boundingbox = structureStart.getBoundingBox();
            Utils.getChunksInBoundingBox(boundingbox).forEach((chunkPos) ->
                    level.getServer().executeBlocking(() -> structureStart.placeInChunk(
                            level,
                            level.structureManager(),
                            chunkgenerator,
                            random,
                            new BoundingBox(
                                    chunkPos.getMinBlockX(),
                                    level.getMinBuildHeight(),
                                    chunkPos.getMinBlockZ(),
                                    chunkPos.getMaxBlockX(),
                                    level.getMaxBuildHeight(),
                                    chunkPos.getMaxBlockZ()
                            ),
                            chunkPos
                    ))
            );
            return findBlockInArea(level, boundingbox);
        }
        return null;
    }

    /**
     * Estimates the bounding box of the portal by searching for connected blocks.
     */
    private static BoundingBox estimateBoundingBox(ServerLevel level, BlockPos pos) {
        int minX = pos.getX(), minY = pos.getY(), minZ = pos.getZ();
        int maxX = pos.getX(), maxY = pos.getY(), maxZ = pos.getZ();

        Set<BlockPos> open = new HashSet<>();
        Set<BlockPos> closed = new HashSet<>();
        open.add(pos);
        closed.add(pos);
        while (!open.isEmpty()) {
            BlockPos current = open.iterator().next();
            open.remove(current);

            // Check if the block is a portal
            if (level.getBlockState(current).is(Blocks.GATEWAY)) {
                minX = Math.min(minX, current.getX());
                minY = Math.min(minY, current.getY());
                minZ = Math.min(minZ, current.getZ());
                maxX = Math.max(maxX, current.getX());
                maxY = Math.max(maxY, current.getY());
                maxZ = Math.max(maxZ, current.getZ());

                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            BlockPos neighbor = current.offset(x, y, z);
                            if (!closed.contains(neighbor)) {
                                open.add(neighbor);
                                closed.add(neighbor);
                            }
                        }
                    }
                }
            }
        }

        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Finds a gateway block in the area around the given position.
     * Tries to optimize the search.
     */
    private static BlockPos findBlockInArea(ServerLevel level, BoundingBox box) {
        BlockPos.MutableBlockPos gatewayPos = new BlockPos.MutableBlockPos();
        for (ChunkPos chunkPos : Utils.getChunksInBoundingBox(box).toList()) {
            LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
            for (int cy = 0; cy < chunk.getSectionsCount(); cy++) {
                LevelChunkSection section = chunk.getSection(cy);
                if (section.hasOnlyAir()) continue;
                if (!section.maybeHas(s -> s.is(Blocks.GATEWAY))) continue;
                for (int x = 0; x < 16; x++) {
                    for (int y = 0; y < 16; y++) {
                        for (int z = 0; z < 16; z++) {
                            gatewayPos.set(
                                    SectionPos.sectionToBlockCoord(chunkPos.x, x),
                                    SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(cy), y),
                                    SectionPos.sectionToBlockCoord(chunkPos.z, z)
                            );
                            if (chunk.getBlockState(gatewayPos).is(Blocks.GATEWAY)) {
                                return gatewayPos;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private static int getColor(ServerLevel level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        ResourceLocation resourceLocation = biome.unwrapKey().map(ResourceKey::location).orElse(new ResourceLocation("minecraft:plains"));
        if (!Config.getInstance().colors.containsKey(resourceLocation.toString())) {
            Common.LOGGER.info("Biome {} not found in color config, using default foliage color.", resourceLocation);
        }
        return Config.getInstance().colors.getOrDefault(resourceLocation.toString(), biome.value().getFoliageColor());
    }

    public static class PortalDataLookup extends SavedData {
        final Set<PortalPair> portals = new HashSet<>();
        final Map<Long, Set<PortalPair>> lookup = new HashMap<>();

        public static PortalDataLookup load(CompoundTag nbt) {
            PortalDataLookup c = new PortalDataLookup();
            for (String key : nbt.getAllKeys()) {
                PortalPair pair = PortalPair.load(nbt.get(key));
                c.portals.add(pair);
                c.populateLookup(pair);
            }
            return c;
        }

        @Override
        public CompoundTag save(CompoundTag nbt) {
            int index = 0;
            for (PortalPair pair : portals) {
                nbt.put(String.valueOf(index), pair.save());
                index++;
            }
            return nbt;
        }

        public synchronized void add(PortalPair data) {
            portals.add(data);
            populateLookup(data);
            setDirty();
        }

        public synchronized void remove(BlockPos pos) {
            PortalPair pair = search(pos);
            if (pair != null) {
                portals.remove(pair);
                rebuildLookup();
                setDirty();
            }
        }

        private void rebuildLookup() {
            lookup.clear();
            for (PortalPair pair : portals) {
                populateLookup(pair);
            }
        }

        private void populateLookup(PortalPair data) {
            populateLookup(data, data.first);
            populateLookup(data, data.second);
        }

        private void populateLookup(PortalPair data, Portal portal) {
            int minX = portal.boundingBox().minX() >> 4;
            int minZ = portal.boundingBox().minZ() >> 4;
            int maxX = portal.boundingBox().maxX() >> 4;
            int maxZ = portal.boundingBox().maxZ() >> 4;

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    long cellId = toLong(x, z);
                    lookup.computeIfAbsent(cellId, k -> new HashSet<>()).add(data);
                }
            }
        }

        public synchronized PortalPair search(BlockPos pos) {
            int cx = pos.getX() >> 4;
            int cz = pos.getZ() >> 4;
            long cellId = toLong(cx, cz);
            Set<PortalPair> candidates = lookup.get(cellId);
            if (candidates != null) {
                for (PortalPair candidate : candidates) {
                    if (candidate.first.boundingBox().isInside(pos) || candidate.second.boundingBox().isInside(pos)) {
                        return candidate;
                    }
                }
            }
            return null;
        }
    }

    public record Portal(BoundingBox boundingBox, int color) {
        public static final Codec<Portal> CODEC = RecordCodecBuilder.create((portal)
                -> portal.group(
                BoundingBox.CODEC.fieldOf("boundingBox").forGetter(Portal::boundingBox),
                Codec.INT.fieldOf("color").forGetter(Portal::color)
        ).apply(portal, Portal::new));

        public BlockPos getSafePosition(ServerLevel level) {
            List<BlockPos> candidates = new LinkedList<>();

            if (boundingBox.getZSpan() > 1) {
                int z = (boundingBox.minZ() + boundingBox.maxZ()) / 2;
                candidates.add(new BlockPos(boundingBox.minX() - 1, boundingBox.minY(), z));
                candidates.add(new BlockPos(boundingBox.maxX() + 1, boundingBox.minY(), z));
            }

            if (boundingBox.getXSpan() > 1) {
                int x = (boundingBox.minX() + boundingBox.maxX()) / 2;
                candidates.add(new BlockPos(x, boundingBox.minY(), boundingBox.minZ() - 1));
                candidates.add(new BlockPos(x, boundingBox.minY(), boundingBox.maxZ() + 1));
            }

            BlockPos center = boundingBox.getCenter();
            level.getChunkSource().addRegionTicket(TicketType.PORTAL, new ChunkPos(center), 3, center);

            for (int y = boundingBox.minY(); y <= level.getMaxBuildHeight(); y++) {
                for (BlockPos candidate : candidates) {
                    BlockPos pos = new BlockPos(candidate.getX(), y, candidate.getZ());
                    if (level.getBlockState(pos).isAir() && level.getBlockState(pos.offset(0, 1, 0)).isAir()) {
                        return pos;
                    }
                }
            }

            return center;
        }
    }

    public record PortalPair(Portal first, Portal second) {
        public static final Codec<PortalPair> CODEC = RecordCodecBuilder.create((pair) -> pair.group(
                Portal.CODEC.fieldOf("first").forGetter(PortalPair::first),
                Portal.CODEC.fieldOf("second").forGetter(PortalPair::second)
        ).apply(pair, PortalPair::new));

        public static PortalPair load(Tag nbt) {
            return CODEC.parse(NbtOps.INSTANCE, nbt).resultOrPartial(Common.LOGGER::error).orElseThrow();
        }

        public Tag save() {
            return CODEC.encodeStart(NbtOps.INSTANCE, this).resultOrPartial(Common.LOGGER::error).orElseThrow();
        }

        public Portal getTarget(BlockPos pos) {
            double dist1 = first.boundingBox.getCenter().distSqr(pos);
            double dist2 = second.boundingBox.getCenter().distSqr(pos);
            return dist1 < dist2 ? second : first;
        }
    }
}
