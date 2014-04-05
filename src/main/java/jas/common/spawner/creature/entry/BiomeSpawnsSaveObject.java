package jas.common.spawner.creature.entry;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

import com.google.common.collect.Table;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class BiomeSpawnsSaveObject {
    public static final String ENTITY_STAT_KEY = "Weight-PassivePackMax-ChunkPackMin-ChunkPackMax";
    public static final String ENTITY_TAG_KEY = "Tags";

    // SortByBiome: <BiomeName, <CreatureType(MONSTER/AMBIENT), <CreatureName/HandlerId/LivingGroup, SpawnListEntry>>>
    // !SortByBiome:<CreatureType(MONSTER/AMBIENT), <CreatureName/HandlerId/LivingGroup, <BiomeName, SpawnListEntry>>>
    private final TreeMap<String, TreeMap<String, TreeMap<String, SpawnListEntryBuilder>>> biomeToTypeToCreature;
    private final boolean sortCreatureByBiome;

    private BiomeSpawnsSaveObject(boolean sortCreatureByBiome) {
        biomeToTypeToCreature = new TreeMap<String, TreeMap<String, TreeMap<String, SpawnListEntryBuilder>>>();
        this.sortCreatureByBiome = sortCreatureByBiome;
    }

    /**
     * 
     * @param registry
     * @param validSpawnListEntries Contains Mapping between BiomeGroupID, LivingType to valid SpawnListEntry
     */
    public BiomeSpawnsSaveObject(Table<String, String, Set<SpawnListEntry>> validSpawnListEntries,
            boolean sortCreatureByBiome) {
        this.sortCreatureByBiome = sortCreatureByBiome;
        biomeToTypeToCreature = new TreeMap<String, TreeMap<String, TreeMap<String, SpawnListEntryBuilder>>>();
        for (Entry<String, Map<String, Set<SpawnListEntry>>> biomeGroupEntry : validSpawnListEntries.rowMap()
                .entrySet()) {
            String biomeGroupId = biomeGroupEntry.getKey();
            for (Entry<String, Set<SpawnListEntry>> livingTypeEntry : biomeGroupEntry.getValue().entrySet()) {
                String livingType = livingTypeEntry.getKey();
                for (SpawnListEntry spawnListEntry : livingTypeEntry.getValue()) {
                    putEntity(biomeGroupId, livingType, spawnListEntry.livingGroupID, new SpawnListEntryBuilder(
                            spawnListEntry), biomeToTypeToCreature);
                }
            }
        }
    }

    public Set<SpawnListEntryBuilder> getBuilders() {
        Set<SpawnListEntryBuilder> builders = new HashSet<SpawnListEntryBuilder>();
        for (TreeMap<String, TreeMap<String, SpawnListEntryBuilder>> secMap : biomeToTypeToCreature.values()) {
            for (TreeMap<String, SpawnListEntryBuilder> tertMap : secMap.values()) {
                builders.addAll(tertMap.values());
            }
        }
        return builders;
    }

    private void putEntity(String biomeGroupId, String livingType, String entityName,
            SpawnListEntryBuilder spawnListEntry,
            TreeMap<String, TreeMap<String, TreeMap<String, SpawnListEntryBuilder>>> primMap) {
        String primKey = sortCreatureByBiome ? biomeGroupId : livingType;
        String secoKey = sortCreatureByBiome ? livingType : entityName;
        String tertKey = sortCreatureByBiome ? entityName : biomeGroupId;

        TreeMap<String, TreeMap<String, SpawnListEntryBuilder>> secMap = primMap.get(primKey);
        if (secMap == null) {
            secMap = new TreeMap<String, TreeMap<String, SpawnListEntryBuilder>>();
            primMap.put(primKey, secMap);
        }
        TreeMap<String, SpawnListEntryBuilder> tertMap = secMap.get(secoKey);
        if (tertMap == null) {
            tertMap = new TreeMap<String, SpawnListEntryBuilder>();
            secMap.put(secoKey, tertMap);
        }
        tertMap.put(tertKey, spawnListEntry);
    }

    public static class BiomeSpawnsSaveObjectSerializer implements JsonSerializer<BiomeSpawnsSaveObject>,
            JsonDeserializer<BiomeSpawnsSaveObject> {
        private final boolean defaultSortByBiome;

        public BiomeSpawnsSaveObjectSerializer() {
            this(false);
        }

        public BiomeSpawnsSaveObjectSerializer(boolean defaultSortByBiome) {
            this.defaultSortByBiome = defaultSortByBiome;
        }

        @Override
        public JsonElement serialize(BiomeSpawnsSaveObject object, Type type, JsonSerializationContext context) {
            JsonObject primObject = new JsonObject();
            primObject.addProperty("SortedByBiome", object.sortCreatureByBiome);
            for (Entry<String, TreeMap<String, TreeMap<String, SpawnListEntryBuilder>>> primEnts : object.biomeToTypeToCreature
                    .entrySet()) {
                String primKey = primEnts.getKey();
                JsonObject secObject = new JsonObject();
                for (Entry<String, TreeMap<String, SpawnListEntryBuilder>> secEnts : primEnts.getValue().entrySet()) {
                    String secKey = secEnts.getKey();
                    JsonObject tertObject = new JsonObject();
                    for (Entry<String, SpawnListEntryBuilder> tertEnts : secEnts.getValue().entrySet()) {
                        String tertKey = tertEnts.getKey();
                        JsonObject entityValueObject = new JsonObject();
                        SpawnListEntryBuilder builder = tertEnts.getValue();
                        String stats = statsToString(builder.getWeight(), builder.getPackSize(),
                                builder.getMinChunkPack(), builder.getMaxChunkPack());
                        entityValueObject.addProperty(ENTITY_STAT_KEY, stats);
                        entityValueObject.addProperty(ENTITY_TAG_KEY, builder.getOptionalParameters());
                        tertObject.add(tertKey, entityValueObject);
                    }
                    secObject.add(secKey, tertObject);
                }
                primObject.add(primKey, secObject);
            }
            return primObject;
        }

        @Override
        public BiomeSpawnsSaveObject deserialize(JsonElement object, Type type, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject primObject = object.getAsJsonObject();
            JsonElement isSorted = primObject.get("SortedByBiome");
            BiomeSpawnsSaveObject saveObject = new BiomeSpawnsSaveObject(isSorted != null ? isSorted.getAsBoolean()
                    : defaultSortByBiome);
            for (Entry<String, JsonElement> primEntries : primObject.entrySet()) {
                String primKey = primEntries.getKey(); // biomeKey
                if (primKey == null || primKey.trim().equals("") || primKey.equalsIgnoreCase("SortedByBiome")) {
                    continue;
                }
                TreeMap<String, TreeMap<String, SpawnListEntryBuilder>> secMap = saveObject.biomeToTypeToCreature
                        .get(primKey);
                if (secMap == null) {
                    secMap = new TreeMap<String, TreeMap<String, SpawnListEntryBuilder>>();
                    saveObject.biomeToTypeToCreature.put(primKey, secMap);
                }
                for (Entry<String, JsonElement> secEntries : primEntries.getValue().getAsJsonObject().entrySet()) {
                    String secKey = secEntries.getKey(); // livingType
                    if (secKey == null || secKey.trim().equals("")) {
                        continue;
                    }
                    TreeMap<String, SpawnListEntryBuilder> tertMap = secMap.get(secKey); // livingTypeMap
                    if (tertMap == null) {
                        tertMap = new TreeMap<String, SpawnListEntryBuilder>();
                        secMap.put(secKey, tertMap);
                    }
                    for (Entry<String, JsonElement> tertEntries : secEntries.getValue().getAsJsonObject().entrySet()) {
                        String tertKey = tertEntries.getKey(); // livingGroup
                        JsonObject entityValueObject = tertEntries.getValue().getAsJsonObject();
                        SpawnListEntryBuilder builder = new SpawnListEntryBuilder(tertKey, primKey);

                        String biomeGroupId = saveObject.sortCreatureByBiome ? primKey : tertKey;
                        String livingGroup = saveObject.sortCreatureByBiome ? tertKey : secKey;
                        builder = new SpawnListEntryBuilder(livingGroup, biomeGroupId);
                        getSetStats(builder, entityValueObject);

                        JsonElement element = entityValueObject.get(ENTITY_TAG_KEY);
                        if (element != null) {
                            builder.setOptionalParameters(element.getAsString());
                        }
                        tertMap.put(tertKey, builder);
                    }
                }
            }
            return saveObject;
        }

        private String statsToString(int weight, int packSize, int minChunk, int maxChunk) {
            return new StringBuilder().append(weight).append("-").append(packSize).append("-").append(minChunk)
                    .append("-").append(maxChunk).toString();
        }

        private void getSetStats(SpawnListEntryBuilder builder, JsonObject creatureNameObject) {
            JsonElement element = creatureNameObject.get(ENTITY_STAT_KEY);
            int[] stats = element != null ? stringToStats(element.getAsString()) : stringToStats("");
            builder.setWeight(stats[0]).setPackSize(stats[1]).setMinChunkPack(stats[2]).setMaxChunkPack(stats[3]);
        }

        private int[] stringToStats(String stats) {
            String[] parts = stats.split("-");
            int[] result = new int[4];
            for (int i = 0; i < 4; i++) {
                try {
                    result[i] = i < parts.length ? Integer.parseInt(parts[i]) : 0;
                } catch (NumberFormatException e) {
                    result[i] = 0;
                }
            }
            return result;
        }
    }
}