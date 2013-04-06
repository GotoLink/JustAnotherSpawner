package jas.common.spawner.biome;

import jas.api.BiomeInterpreter;
import jas.common.spawner.creature.handler.ReflectionHelper;

import java.util.ArrayList;
import java.util.Collection;

import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.BiomeGenSwamp;
import net.minecraft.world.biome.SpawnListEntry;
import net.minecraft.world.gen.ChunkProviderGenerate;
import net.minecraft.world.gen.feature.MapGenScatteredFeature;

public class BiomeInterpreterSwamp implements BiomeInterpreter {

    @Override
    public Collection<String> getStructureKeys() {
        Collection<String> collection = new ArrayList<>();
        collection.add("WitchHut");
        return collection;
    }

    @Override
    public Collection<SpawnListEntry> getStructureSpawnList(String structureKey) {
        Collection<SpawnListEntry> collection = new ArrayList<>();
        if (structureKey.equals("WitchHut")) {
            collection.add(new SpawnListEntry(EntityWitch.class, 1, 1, 1));
        }
        return collection;
    }

    @Override
    // TODO: Compile List of Fields we could make public for Coremod
    public String areCoordsStructure(World world, int xCoord, int yCoord, int zCoord) {
        BiomeGenBase biome = world.getBiomeGenForCoords(xCoord, zCoord);
        ChunkProviderGenerate chunkProviderGenerate = BiomeInterpreterHelper.getInnerChunkProvider(world,
                ChunkProviderGenerate.class);

        if (biome instanceof BiomeGenSwamp && chunkProviderGenerate != null) {
            MapGenScatteredFeature mapGenScatteredFeature;
            if (ReflectionHelper.isUnObfuscated(ChunkProviderGenerate.class, "ChunkProviderGenerate")) {
                mapGenScatteredFeature = ReflectionHelper.getFieldFromReflection("scatteredFeatureGenerator",
                        chunkProviderGenerate, MapGenScatteredFeature.class);
            } else {
                mapGenScatteredFeature = ReflectionHelper.getFieldFromReflection("field_73233_x",
                        chunkProviderGenerate, MapGenScatteredFeature.class);
            }
            if (mapGenScatteredFeature != null && mapGenScatteredFeature.hasStructureAt(xCoord, yCoord, zCoord)) {
                return "WitchHut";
            }
        }
        return null;
    }

    @Override
    public boolean shouldUseHandler(World world, BiomeGenBase biomeGenBase) {
        return biomeGenBase.biomeName.equals(BiomeGenBase.swampland.biomeName);
    }
}
