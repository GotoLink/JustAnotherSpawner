package jas.common.spawner.creature.handler;

import jas.common.DefaultProps;
import jas.common.JASLog;
import jas.common.Properties;
import jas.common.spawner.creature.entry.SpawnListEntry;
import jas.common.spawner.creature.type.CreatureType;
import jas.common.spawner.creature.type.CreatureTypeRegistry;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.ConfigCategory;
import net.minecraftforge.common.Configuration;
import net.minecraftforge.common.Property;

import com.google.common.base.CharMatcher;

public enum CreatureHandlerRegistry {
    INSTANCE;
    private final HashMap<Class<? extends EntityLiving>, LivingHandler> livingHandlers = new HashMap<Class<? extends EntityLiving>, LivingHandler>();
    private final HashMap<Class<? extends EntityLiving>, Class<? extends LivingHandler>> handlersToAdd = new HashMap<Class<? extends EntityLiving>, Class<? extends LivingHandler>>();
    private List<BiomeGenBase> biomeList = new ArrayList<BiomeGenBase>();

    private final HashMap<String, Configuration> modConfigCache = new HashMap<String, Configuration>();
    private List<Class<? extends EntityLiving>> entityList = new ArrayList<Class<? extends EntityLiving>>();
    public static final String delimeter = "-";
    public static final String LivingHandlerCategoryComment = "Editable Format: CreatureType" + delimeter
            + "ShouldSpawn" + delimeter + "ForceDespawn" + delimeter + "UseModLocationCheck";
    public static final String SpawnListCategoryComment = "Editable Format: SpawnWeight" + delimeter + "SpawnPackSize"
            + delimeter + "MinChunkPackSize" + delimeter + "MaxChunkPackSize";

    /**
     * Searhes and Process Entities it can Find in EntityList to create default the LivingHandlers and SpawnList
     * Entries. Will Populate the Spawnlists in corresponding CreatureTypes.
     * 
     * @param configDirectory
     */
    public void findProcessEntitesForHandlers(File configDirectory, MinecraftServer minecraftServer) {
        /* Clear Various Lists As ingame whenever we switch Worlds they may already be initialized */
        modConfigCache.clear();
        livingHandlers.clear();
        Iterator<CreatureType> iterator = CreatureTypeRegistry.INSTANCE.getCreatureTypes();
        while (iterator.hasNext()) {
            CreatureType type = iterator.next();
            type.resetSpawns();
        }
        populateEntityList();
        populateBiomeList();

        for (Class<? extends EntityLiving> livingClass : entityList) {
            if (livingHandlers.containsKey(livingClass)) {
                continue;
            }
            String mobName = (String) EntityList.classToStringMapping.get(livingClass);
            Configuration masterConfig = getConfigurationFile(configDirectory, "Master", mobName);
            Configuration worldConfig = getConfigurationFile(configDirectory, minecraftServer.worldServers[0].getWorldInfo().getWorldName(), mobName);

            LivingHandler livingHandler = generateHandlerFromConfig(
                    worldConfig,
                    livingClass,
                    mobName,
                    minecraftServer.worldServers[0],
                    generateHandlerFromConfig(masterConfig, livingClass, mobName, minecraftServer.worldServers[0], null));
            // TODO: Replace LivingHandler with Custom Handler From "handlersToAdd"
            livingHandlers.put(livingClass, livingHandler);

            if (livingHandler.shouldSpawn && !livingHandler.creatureTypeID.equals(CreatureTypeRegistry.NONE)) {
                for (BiomeGenBase biomeGenBase : biomeList) {
                    SpawnListEntry spawnListEntry = generateSpawnListEntry(
                            worldConfig,
                            livingClass,
                            biomeGenBase,
                            mobName,
                            minecraftServer.worldServers[0],
                            generateSpawnListEntry(masterConfig, livingClass, biomeGenBase, mobName,
                                    minecraftServer.worldServers[0], null));
                    if (spawnListEntry.itemWeight > 0) {
                        JASLog.info("Adding SpawnListEntry %s of type %s to Biome %s", mobName,
                                spawnListEntry.getLivingHandler().creatureTypeID, spawnListEntry.biomeName);
                        CreatureTypeRegistry.INSTANCE.getCreatureType(spawnListEntry.getLivingHandler().creatureTypeID)
                                .addSpawn(spawnListEntry);
                    } else {
                        JASLog.debug(Level.INFO,
                                "Not adding Generated SpawnListEntry of %s due to Weight. Biomes: %s, ItemWeight: %s",
                                mobName, biomeGenBase.biomeName, spawnListEntry.itemWeight);
                    }
                }
            } else {
                JASLog.debug(Level.INFO,
                        "Not Generating SpawnList entries for %s. ShouldSpawn: %s, CreatureTypeID: %s", mobName,
                        livingHandler.shouldSpawn, livingHandler.creatureTypeID);
            }
        }

        for (Configuration config : modConfigCache.values()) {
            config.save();
        }
    }

    /**
     * Caches and Retrieves Configration Files for Individual modIDs. The ModID is inferred from the entity name in the
     * form ModID:EntityName
     * 
     * @param configDirectory
     * @param minecraftServer
     * @param fullMobName
     * @return
     */
    private Configuration getConfigurationFile(File configDirectory, String worldName, String fullMobName) {
        String modID;
        String[] mobNameParts = fullMobName.split("\\.");
        if (mobNameParts.length == 2) {
            String regexRetain = "qwertyuiopasdfghjklzxcvbnm0QWERTYUIOPASDFGHJKLZXCVBNM123456789";
            modID = CharMatcher.anyOf(regexRetain).retainFrom(mobNameParts[0]);
        } else {
            modID = "Vanilla";
        }

        Configuration config;
        if (modConfigCache.get(worldName + modID) == null) {
            config = new Configuration(new File(configDirectory, DefaultProps.WORLDSETTINGSDIR + worldName + "/"
                    + DefaultProps.ENTITYSUBDIR + modID + ".cfg"));
            config.load();
            setupCategories(config);
            modConfigCache.put(worldName + modID, config);
        }
        return modConfigCache.get(worldName + modID);
    }

    private void setupCategories(Configuration config) {
        ConfigCategory category = config.getCategory("CreatureSettings.LivingHandler".toLowerCase(Locale.ENGLISH));
        category.setComment(LivingHandlerCategoryComment);

        category = config.getCategory("CreatureSettings.SpawnListEntry".toLowerCase(Locale.ENGLISH));
        category.setComment(SpawnListCategoryComment);
    }

    /**
     * Search EntityList for Valid Creature Entities
     */
    @SuppressWarnings("unchecked")
    private void populateEntityList() {
        entityList.clear();
        Iterator<?> entityIterator = EntityList.stringToClassMapping.keySet().iterator();
        while (entityIterator.hasNext()) {
            Object classKey = entityIterator.next();
            if (EntityLiving.class.isAssignableFrom((Class<?>) EntityList.stringToClassMapping.get(classKey))
                    && !Modifier.isAbstract(((Class<?>) EntityList.stringToClassMapping.get(classKey)).getModifiers())) {
                JASLog.info("Found Entity %s", classKey);
                entityList.add((Class<? extends EntityLiving>) EntityList.stringToClassMapping.get(classKey));
            }
        }
    }

    /**
     * Search BiomeGenBase for Valid Biomes to Spawn In
     */
    private void populateBiomeList() {
        biomeList.clear();
        for (int i = 0; i < BiomeGenBase.biomeList.length; i++) {
            if (BiomeGenBase.biomeList[i] != null) {
                biomeList.add(BiomeGenBase.biomeList[i]);
            }
        }
    }

    /**
     * Will Naturally Generate Handlers using Config Settings for all Found Entities
     * 
     * @param configDirectory
     */
    private LivingHandler generateHandlerFromConfig(Configuration config, Class<? extends EntityLiving> livingClass,
            String mobName, WorldServer worldServer, LivingHandler defaultSettings) {
        String creatureTypeID = defaultSettings != null ? defaultSettings.creatureTypeID
                : enumCreatureTypeToLivingType(livingClass, worldServer);
        boolean shouldSpawn = defaultSettings != null ? defaultSettings.shouldSpawn : false;
        boolean forceDespawn = defaultSettings != null ? defaultSettings.forceDespawn : false;
        boolean useModLocationCheck = defaultSettings != null ? defaultSettings.useModLocationCheck : true;

        String defaultValue = creatureTypeID + delimeter + Boolean.toString(shouldSpawn) + delimeter
                + Boolean.toString(forceDespawn) + delimeter + Boolean.toString(useModLocationCheck);
        Property resultValue = config.get("CreatureSettings.LivingHandler", mobName, defaultValue);
        String[] resultParts = resultValue.value.split("\\" + delimeter);
        if (resultParts.length == 4) {
            String resultCreatureType = LivingRegsitryHelper.parseCreatureTypeID(resultParts[0], creatureTypeID,
                    "creatureTypeID");
            boolean resultShouldSpawn = LivingRegsitryHelper.parseBoolean(resultParts[1], shouldSpawn, "ShouldSpawn");
            boolean resultForceDespawn = LivingRegsitryHelper
                    .parseBoolean(resultParts[2], forceDespawn, "forceDespawn");
            boolean resultLocationCheck = LivingRegsitryHelper.parseBoolean(resultParts[3], useModLocationCheck,
                    "LocationCheck");
            return new LivingHandler(livingClass, resultCreatureType, resultLocationCheck, resultShouldSpawn,
                    resultForceDespawn);
        } else {
            JASLog.severe(
                    "LivingHandler Entry %s was invalid. Data is being ignored and loaded with default settings %s, %s, %s, %s",
                    mobName, creatureTypeID, useModLocationCheck, shouldSpawn, forceDespawn);
            return new LivingHandler(livingClass, creatureTypeID, useModLocationCheck, shouldSpawn, forceDespawn);
        }
    }

    /**
     * Determines the Default JAS Living Type from the Vanilla EnumCreatureType
     * 
     * @param livingClass
     * @return
     */
    private String enumCreatureTypeToLivingType(Class<? extends EntityLiving> livingClass, World world) {
        EntityLiving creature = LivingHelper.createCreature(livingClass, world);
        for (EnumCreatureType type : EnumCreatureType.values()) {
			boolean isType = type.getCreatureClass().isAssignableFrom(livingClass);
            if (isType && CreatureTypeRegistry.INSTANCE.getCreatureType(type.toString()) != null) {
                return type.toString();
            }
        }
        return CreatureTypeRegistry.NONE;
    }

    /**
     * Determines the Vanilla EnumCreatureType from the equivalent JAS living Type
     * 
     * @return
     */
    private EnumCreatureType livingTypeToEnumCreatureType(String creatureTypeID) {
        if (creatureTypeID.equals(CreatureTypeRegistry.MONSTER)) {
            return EnumCreatureType.monster;
        } else if (creatureTypeID.equals(CreatureTypeRegistry.AMBIENT)) {
            return EnumCreatureType.ambient;
        } else if (creatureTypeID.equals(CreatureTypeRegistry.CREATURE)) {
            return EnumCreatureType.creature;
        } else if (creatureTypeID.equals(CreatureTypeRegistry.WATERCREATURE)) {
            return EnumCreatureType.waterCreature;
        } else {
            return null;
        }
    }

    /**
     * Will use already generated livingHandlers to generate Biome Specific SpawnList Entries to Populate the each
     * CreatureType biomeSpawnLists.
     * 
     * @param configDirectory
     */
    public SpawnListEntry generateSpawnListEntry(Configuration config, Class<? extends EntityLiving> livingClass,
            BiomeGenBase biomeGenBase, String mobName, WorldServer worldServer, SpawnListEntry defaultSettings) {
        int spawnWeight = defaultSettings != null ? defaultSettings.itemWeight : 0;
        int packSize = defaultSettings != null ? defaultSettings.packSize : 4;
        int minChunkPack = defaultSettings != null ? defaultSettings.minChunkPack : 0;
        int maxChunkPack = defaultSettings != null ? defaultSettings.maxChunkPack : 4;

        EnumCreatureType creatureType = livingTypeToEnumCreatureType(livingHandlers.get(livingClass).creatureTypeID);
        if (defaultSettings != null && creatureType != null) {
            @SuppressWarnings("unchecked")
            List<net.minecraft.world.biome.SpawnListEntry> spawnListEntries = biomeGenBase
                    .getSpawnableList(creatureType);
            for (net.minecraft.world.biome.SpawnListEntry spawnListEntry : spawnListEntries) {
                if (spawnListEntry.entityClass.equals(livingClass)) {
                    spawnWeight = spawnListEntry.itemWeight;
                    minChunkPack = spawnListEntry.minGroupCount;
                    maxChunkPack = spawnListEntry.maxGroupCount;
                }
            }
        }
        
        String defaultValue = Integer.toString(spawnWeight) + delimeter + Integer.toString(packSize) + delimeter
                + Integer.toString(minChunkPack) + delimeter + Integer.toString(maxChunkPack);
        boolean sortByBiome = Properties.sortCreatureByBiome;

        Property resultValue;
        String categoryKey;
        if (sortByBiome) {
            categoryKey = "CreatureSettings.SpawnListEntry." + biomeGenBase.biomeName;
            resultValue = config.get(categoryKey, mobName, defaultValue);
        } else {
            categoryKey = "CreatureSettings.SpawnListEntry." + mobName;
            resultValue = config.get(categoryKey, biomeGenBase.biomeName, defaultValue);
        }
        ConfigCategory category = config.getCategory(categoryKey.toLowerCase(Locale.ENGLISH));
        category.setComment(SpawnListCategoryComment);

        String[] resultParts = resultValue.value.split("\\" + delimeter);
        if (resultParts.length == 4) {
            int resultSpawnWeight = LivingRegsitryHelper.parseInteger(resultParts[0], packSize, "spawnWeight");
            int resultPackSize = LivingRegsitryHelper.parseInteger(resultParts[1], packSize, "packSize");
            int resultMinChunkPack = LivingRegsitryHelper.parseInteger(resultParts[2], packSize, "minChunkPack");
            int resultMaxChunkPack = LivingRegsitryHelper.parseInteger(resultParts[3], packSize, "maxChunkPack");
            return new SpawnListEntry(livingClass, biomeGenBase.biomeName, resultSpawnWeight, resultPackSize,
                    resultMinChunkPack, resultMaxChunkPack);
        } else {
            JASLog.severe(
                    "SpawnListEntry %s was invalid. Data is being ignored and loaded with default settings %s, %s",
                    mobName, packSize, spawnWeight);
            return new SpawnListEntry(livingClass, biomeGenBase.biomeName, spawnWeight, packSize, minChunkPack,
                    maxChunkPack);
        }
    }
    
    

    /**
     * Registers a Living Handler to be initialized by the System.
     * 
     * @param handlerID
     * @param handler
     * @return Returns False if Handler is replaced during registration
     */
    public boolean registerHandler(Class<? extends EntityLiving> livingEntity,
            Class<? extends LivingHandler> livingHandler) {
        boolean isReplaced = false;
        if (!handlersToAdd.containsKey(livingEntity)) {
            JASLog.warning("Custom Living Handler %s which was to be registered will be replaced with %s",
                    handlersToAdd.containsKey(livingEntity), livingHandler);
            isReplaced = true;
        }
        handlersToAdd.put(livingEntity, livingHandler);
        return !isReplaced;
    }

    /**
     * Gets the Appropriate LivingHandler from the Provided Key
     * 
     * @param handlerID
     * @return
     */
    public LivingHandler getLivingHandler(Class<? extends Entity> entityClass) {
        return livingHandlers.get(entityClass);
    }
}
