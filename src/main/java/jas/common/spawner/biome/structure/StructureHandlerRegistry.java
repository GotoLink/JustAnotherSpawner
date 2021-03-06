package jas.common.spawner.biome.structure;

import jas.api.StructureInterpreter;
import jas.common.FileUtilities;
import jas.common.GsonHelper;
import jas.common.WorldProperties;
import jas.common.spawner.creature.entry.SpawnListEntry;
import jas.common.spawner.creature.entry.SpawnListEntryBuilder;
import jas.common.spawner.creature.handler.LivingHandlerRegistry;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import net.minecraft.world.World;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;

public class StructureHandlerRegistry {
    private static final ArrayList<StructureInterpreter> structureInterpreters = new ArrayList<StructureInterpreter>();
    public final LivingHandlerRegistry livingHandlerRegistry;
    public final WorldProperties worldProperties;
    private ImmutableList<StructureHandler> structureHandlers;

    public ImmutableList<StructureHandler> handlers() {
        return structureHandlers;
    }

    static {
        structureInterpreters.add(new StructureInterpreterSwamp());
        structureInterpreters.add(new StructureInterpreterNether());
        structureInterpreters.add(new StructureInterpreterOverworldStructures());
    }

    public static void registerInterpreter(StructureInterpreter structureInterpreter) {
        structureInterpreters.add(structureInterpreter);
    }

    public StructureHandlerRegistry(LivingHandlerRegistry livingHandlerRegistry, WorldProperties worldProperties) {
        this.livingHandlerRegistry = livingHandlerRegistry;
        this.worldProperties = worldProperties;
    }

    public void loadFromConfig(File configDirectory, World world) {
        ArrayList<StructureHandler> structureHandlers = new ArrayList<StructureHandler>();
        for (StructureInterpreter interpreter : structureInterpreters) {
            structureHandlers.add(new StructureHandler(interpreter));
        }

        File structureFile = StructureHandler.getFile(configDirectory,
                worldProperties.getFolderConfiguration().saveName);
        Gson gson = GsonHelper.createGson(true, new Type[] { StructureSaveObject.class },
                new Object[] { new StructureSaveObject.Serializer() });
        StructureSaveObject saveObject = GsonHelper.readOrCreateFromGson(
                FileUtilities.createReader(structureFile, true), StructureSaveObject.class, gson);
        HashMap<String, Collection<SpawnListEntryBuilder>> readSpawnLists = saveObject.createKeyToSpawnList();

        for (StructureHandler structureHandler : structureHandlers) {
            structureHandler.readFromConfig(livingHandlerRegistry, readSpawnLists, worldProperties);
        }
        this.structureHandlers = ImmutableList.<StructureHandler> builder().addAll(structureHandlers).build();
    }

    /**
     * Used to save the currently loaded settings into the Configuration Files
     * 
     * If config settings are already present, they will be overwritten
     */
    public void saveCurrentToConfig(File configDirectory) {
        File structureFile = StructureHandler.getFile(configDirectory,
                worldProperties.getFolderConfiguration().saveName);
        Gson gson = GsonHelper.createGson(true, new Type[] { StructureSaveObject.class },
                new Object[] { new StructureSaveObject.Serializer() });
        GsonHelper.writeToGson(FileUtilities.createWriter(structureFile, true), new StructureSaveObject(
                livingHandlerRegistry, structureHandlers), gson);
    }

    public Collection<SpawnListEntry> getSpawnListAt(World world, int xCoord, int yCoord, int zCoord) {
        Iterator<StructureHandler> iterator = this.handlers().iterator();
        while (iterator.hasNext()) {
            StructureHandler handler = iterator.next();
            if (handler.doesHandlerApply(world, xCoord, yCoord, zCoord)) {
                Collection<SpawnListEntry> spawnEntryList = handler
                        .getStructureSpawnList(world, xCoord, yCoord, zCoord);
                if (!spawnEntryList.isEmpty()) {
                    return spawnEntryList;
                }
            }
        }
        return Collections.emptyList();
    }
}
