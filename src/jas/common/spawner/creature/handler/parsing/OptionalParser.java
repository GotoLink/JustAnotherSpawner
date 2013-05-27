package jas.common.spawner.creature.handler.parsing;

import jas.common.JASLog;
import jas.common.Properties;
import jas.common.spawner.creature.handler.parsing.keys.Key;
import jas.common.spawner.creature.handler.parsing.settings.OptionalSettingsBase;

import java.util.HashMap;
import java.util.logging.Level;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

//TODO: most MEthods in this Class should be refactored the appropriate KeyParser
public class OptionalParser {

    public static Integer parseSingleInteger(String[] values, Integer defaultInt, String parameter) {
        if (values.length == 2) {
            return ParsingHelper.parseFilteredInteger(values[1], defaultInt, parameter);
        } else {
            JASLog.severe("Error Parsing %s Parameter. Invalid Argument Length.", parameter);
            return null;
        }
    }

    public static int[] parseDoubleInteger(String[] values, int[] defaultInts, String parameter) {
        if (values.length == 3) {
            int[] integers = new int[2];
            integers[0] = ParsingHelper.parseFilteredInteger(values[1], defaultInts[0], "1st " + parameter);
            integers[1] = ParsingHelper.parseFilteredInteger(values[2], defaultInts[1], "2nd " + parameter);
            return integers;
        } else {
            JASLog.severe("Error Parsing %s Parameter. Invalid Argument Length.", parameter);
            return null;
        }
    }
    
    /**
     * Parses the Light Tag.
     * 
     * Format [0] Tag, [1] MinLightLevel, [2] MaxLightLevel.
     * 
     * @param values Values to be Used for Parsing
     * @param valueCache Cache used by OptionalSettings to hold values
     */
    public static int[] parseLight(String[] values) {
        if (values.length == 3) {
            int[] lights = new int[2];
            lights[0] = ParsingHelper.parseFilteredInteger(values[1], 16, "Min " + Key.light.key);
            lights[1] = ParsingHelper.parseFilteredInteger(values[2], 16, "Max " + Key.light.key);
            return lights;
        } else {
            JASLog.severe("Error Parsing deSpawn Light Parameter. Invalid Argument Length.");
            return null;
        }
    }
    
    /**
     * Parses the Block Tag.
     * 
     * @param values Values to be Used for Parsing
     * @param valueCache Cache used by OptionalSettings to hold values
     * @return Returns a ArrayListMultimap mapping BlockID to Meta values
     */
    public static ListMultimap<Integer, Integer> parseBlock(String[] values) {
        ListMultimap<Integer, Integer> blockMeta = ArrayListMultimap.create();

        for (int j = 1; j < values.length; j++) {
            int minID = -1;
            int maxID = -1;
            int minMeta = 0;
            int maxMeta = 0;
            /* Parse Scenario: 2>4-1>2 ADDS (Block,Meta)(2,1)(2,2)(3,1)(3,2)(4,1)(4,2) */
            String[] idMetaParts = values[j].split("-");
            for (int k = 0; k < idMetaParts.length; k++) {
                String[] rangeParts = idMetaParts[k].split(">");
                if (k == 0) {
                    for (int l = 0; l < rangeParts.length; l++) {
                        if (l == 0) {
                            minID = ParsingHelper.parseFilteredInteger(rangeParts[l], minID, "parseMinBlockID");
                        } else if (l == 1) {
                            maxID = ParsingHelper.parseFilteredInteger(rangeParts[l], maxID, "parseMaxBlockID");
                        } else {
                            JASLog.warning("Block entry %s contains too many > elements.", values[j]);
                        }
                    }
                } else if (k == 1) {
                    for (int l = 0; l < rangeParts.length; l++) {
                        if (l == 0) {
                            minMeta = ParsingHelper.parseFilteredInteger(rangeParts[l], minID, "parseMinMetaID");
                        } else if (l == 1) {
                            maxMeta = ParsingHelper.parseFilteredInteger(rangeParts[l], minID, "parseMaxMetaID");
                        } else {
                            JASLog.warning("Block entry %s contains too many > elements.", values[j]);
                        }
                    }
                } else {
                    JASLog.warning("Block entry %s contains too many - elements.", values[j]);
                }
            }

            /* Gaurantee Max > Min. Auxillary Purpose: Gaurantees max is not -1 if only min is Set */
            maxID = minID > maxID ? minID : maxID;
            maxMeta = minMeta > maxMeta ? minMeta : maxMeta;

            for (int id = minID; id <= maxID; id++) {
                for (int meta = minMeta; meta <= maxMeta; meta++) {
                    if (id != -1) {
                        JASLog.debug(Level.INFO, "Would be adding (%s,%s)", id, meta);
                        blockMeta.put(id, meta);
                    }
                }
            }
        }
        return !blockMeta.isEmpty() ? blockMeta : null;
    }

    /**
     * Parses the BlockRange Tag.
     * 
     * @param values Values to be Used for Parsing
     * @param valueCache Cache used by OptionalSettings to hold values
     */
    public static void parseBlockRange(String[] values, HashMap<String, Object> valueCache) {
        if (values.length == 4) {
            valueCache.put(Key.blockRangeX.key,
                    ParsingHelper.parseFilteredInteger(values[1], OptionalSettingsBase.defaultBlockRange, "blockRangeX"));
            valueCache.put(Key.blockRangeY.key,
                    ParsingHelper.parseFilteredInteger(values[2], OptionalSettingsBase.defaultBlockRange, "blockRangeY"));
            valueCache.put(Key.blockRangeZ.key,
                    ParsingHelper.parseFilteredInteger(values[3], OptionalSettingsBase.defaultBlockRange, "blockRangeZ"));
        } else if (values.length == 2) {
            valueCache.put(Key.blockRangeX.key,
                    ParsingHelper.parseFilteredInteger(values[1], OptionalSettingsBase.defaultBlockRange, "blockRangeX"));
            valueCache.put(Key.blockRangeY.key,
                    ParsingHelper.parseFilteredInteger(values[1], OptionalSettingsBase.defaultBlockRange, "blockRangeY"));
            valueCache.put(Key.blockRangeZ.key,
                    ParsingHelper.parseFilteredInteger(values[1], OptionalSettingsBase.defaultBlockRange, "blockRangeZ"));
        } else {
            JASLog.severe("Error Parsing deSpawn block search range Parameter. Invalid Argument Length.");
        }
    }

    /**
     * Parses the SpawnRate Tag.
     * 
     * @param values Values to be Used for Parsing
     * @param valueCache Cache used by OptionalSettings to hold values
     */
    public static void parseSpawnRate(String[] values, HashMap<String, Object> valueCache) {
        if (values.length == 2) {
            valueCache.put(Key.spawnRate.key, ParsingHelper.parseFilteredInteger(values[1],
                    OptionalSettingsBase.defaultSpawnRate, Key.spawnRate.key));
        } else {
            JASLog.severe("Error Parsing deSpawn spawn rate Parameter. Invalid Argument Length.");
        }
    }

    /**
     * Parses the SpawnRange Tag.
     * 
     * @param values Values to be Used for Parsing
     * @param valueCache Cache used by OptionalSettings to hold values
     */
    public static void parseSpawnRange(String[] values, HashMap<String, Object> valueCache) {
        if (values.length == 2) {
            valueCache.put(Key.spawnRange.key,
                    ParsingHelper.parseFilteredInteger(values[1], Properties.despawnDist, Key.spawnRange.key));
        } else {
            JASLog.severe("Error Parsing spawnRange parameter. Invalid Argument Length.");
        }
    }

    @Deprecated
    public static Boolean parseSky(String[] values) {
        if (values.length == 1) {
            if (Key.sky.key.equalsIgnoreCase(values[0])) {
                return Boolean.TRUE;
            } else {
                return Boolean.FALSE;
            }
        } else {
            JASLog.severe("Error Parsing Needs Sky parameter. Invalid Argument Length.");
            return null;
        }
    }

    public static void parseEntityCap(String[] values, HashMap<String, Object> valueCache) {
        if (values.length == 2) {
            valueCache.put(Key.entityCap.key, ParsingHelper.parseFilteredInteger(values[1], 0, Key.entityCap.key));
        } else {
            JASLog.severe("Error Parsing Needs EntityCap parameter. Invalid Argument Length.");
        }
    }

    public static void parseDespawnAge(String[] values, HashMap<String, Object> valueCache) {
        if (values.length == 2) {
            valueCache.put(Key.despawnAge.key, ParsingHelper.parseFilteredInteger(values[1], 600, Key.despawnAge.key));
        } else {
            JASLog.severe("Error Parsing Needs EntityCap parameter. Invalid Argument Length.");
        }
    }

    public static Integer parseMinSpawnHeight(String[] values) {
        if (values.length == 2) {
            return ParsingHelper.parseFilteredInteger(values[1], 256, Key.minSpawnHeight.key);
        } else {
            JASLog.severe("Error Parsing Min Spawn Height parameter. Invalid Argument Length.");
            return null;
        }
    }

    public static Integer parseMaxSpawnHeight(String[] values) {
        if (values.length == 2) {
            return ParsingHelper.parseFilteredInteger(values[1], -1, Key.maxSpawnHeight.key);
        } else {
            JASLog.severe("Error Parsing Max Spawn Height parameter. Invalid Argument Length.");
            return null;
        }
    }
}
