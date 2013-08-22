package jas.common.command;

import jas.common.spawner.EntityCounter;
import jas.common.spawner.EntityCounter.CountableInt;
import jas.common.spawner.creature.handler.CreatureHandlerRegistry;
import jas.common.spawner.creature.handler.LivingHandler;
import jas.common.spawner.creature.type.CreatureType;
import jas.common.spawner.creature.type.CreatureTypeRegistry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatMessageComponent;

public class CommandComposition extends CommandBase {
    public String getCommandName() {
        return "jascomposition";
    }

    /**
     * Return the required permission level for this command.
     */
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getCommandUsage(ICommandSender commandSender) {
        return "commands.jascomposition.usage";
    }

    @Override
    public void processCommand(ICommandSender commandSender, String[] stringArgs) {

        if (stringArgs.length >= 3) {
            throw new WrongUsageException("commands.jascomposition.usage", new Object[0]);
        }

        EntityPlayerMP targetPlayer;
        if (stringArgs.length == 2) {
            targetPlayer = func_82359_c(commandSender, stringArgs[0]);
        } else {
            targetPlayer = func_82359_c(commandSender, commandSender.getCommandSenderName());
        }

        String entityCategName = stringArgs.length == 0 ? "*" : stringArgs.length == 1 ? stringArgs[0] : stringArgs[1];

        StringBuilder countedContents = new StringBuilder();

        boolean foundMatch = false;
        Iterator<CreatureType> creatureTypes = CreatureTypeRegistry.INSTANCE.getCreatureTypes();
        while (creatureTypes.hasNext()) {
            CreatureType creatureType = creatureTypes.next();
            if (entityCategName.equals("*") || entityCategName.equals(creatureType.typeID)) {
                foundMatch = true;
                EntityCounter despawnTypeCount = new EntityCounter();
                EntityCounter totalTypeCount = new EntityCounter();
                EntityCounter creatureCount = new EntityCounter();
                EntityCounter despawnCreatureCount = new EntityCounter();
                foundMatch = true;
                @SuppressWarnings("unchecked")
                Iterator<? extends Entity> creatureIterator = targetPlayer.worldObj.loadedEntityList.iterator();
                while (creatureIterator.hasNext()) {
                    Entity entity = creatureIterator.next();
                    LivingHandler livingHandler = CreatureHandlerRegistry.INSTANCE.getLivingHandler(entity.getClass());
                    if (livingHandler != null && entity instanceof EntityLiving
                            && livingHandler.creatureTypeID.equals(creatureType.typeID)) {
                        creatureCount.incrementOrPutIfAbsent(entity.getClass().getSimpleName(), 1);
                        totalTypeCount.incrementOrPutIfAbsent(creatureType.typeID, 1);
                        if (livingHandler.canDespawn((EntityLiving) entity)) {
                            despawnTypeCount.incrementOrPutIfAbsent(creatureType.typeID, 1);
                            despawnCreatureCount.incrementOrPutIfAbsent(entity.getClass().getSimpleName(), 1);
                        }
                    }
                }

                countedContents.append(" \u00A71").append(creatureType.typeID).append("\u00A7r: ");
                int despawnable = despawnTypeCount.getOrPutIfAbsent(creatureType.typeID, 0).get();
                int total = totalTypeCount.getOrPutIfAbsent(creatureType.typeID, 0).get();
                if (despawnable == total) {
                    countedContents.append("\u00A72").append(despawnable);
                } else {
                    countedContents.append("\u00A74").append(despawnable).append(" of ").append(total);
                }
                countedContents.append("\u00A7r").append(" despawnable. ");

                Iterator<Entry<String, CountableInt>> iterator = creatureCount.countingHash.entrySet().iterator();
                while (iterator.hasNext()) {
                    Entry<String, CountableInt> entry = iterator.next();
                    int creatureAmount = entry.getValue().get();
                    int despawnAmount = despawnCreatureCount.getOrPutIfAbsent(entry.getKey(), 0).get();
                    if (despawnAmount == creatureAmount) {
                        countedContents.append("[\u00A72").append(creatureAmount);
                    } else {
                        countedContents.append("[\u00A74").append(despawnAmount).append("/").append(creatureAmount);
                    }
                    countedContents.append("\u00A7r]").append(entry.getKey());

                    if (iterator.hasNext()) {
                        countedContents.append(", ");
                    }
                }
            }
        }

        if (!foundMatch) {
            throw new WrongUsageException("commands.jascomposition.typenotfound", new Object[0]);
        } else {
            commandSender.sendChatToPlayer(new ChatMessageComponent().func_111079_a(countedContents.toString()));
        }
    }

    /**
     * Adds the strings available in this command to the given list of tab completion options.
     */
    @Override
    @SuppressWarnings("rawtypes")
    public List addTabCompletionOptions(ICommandSender commandSender, String[] stringArgs) {
        if (stringArgs.length == 1) {
            List<String> values = new ArrayList<String>();
            Iterator<CreatureType> iterator = CreatureTypeRegistry.INSTANCE.getCreatureTypes();
            while (iterator.hasNext()) {
                CreatureType creatureType = iterator.next();
                values.add(creatureType.typeID);
            }

            String[] combinedValues = new String[values.size() + MinecraftServer.getServer().getAllUsernames().length];
            int index = 0;
            for (String username : MinecraftServer.getServer().getAllUsernames()) {
                combinedValues[index] = username;
                index++;
            }

            for (String typeName : values) {
                combinedValues[index] = typeName;
                index++;
            }

            return getListOfStringsMatchingLastWord(stringArgs, combinedValues);
        } else if (stringArgs.length == 2) {
            List<String> values = new ArrayList<String>();
            Iterator<CreatureType> iterator = CreatureTypeRegistry.INSTANCE.getCreatureTypes();
            while (iterator.hasNext()) {
                CreatureType entityType = iterator.next();
                values.add(entityType.typeID);
            }
            return getListOfStringsMatchingLastWord(stringArgs, values.toArray(new String[values.size()]));
        } else if (stringArgs.length == 3) {
            return getListOfStringsMatchingLastWord(stringArgs, new String[] { "true", "false" });
        } else {
            return null;
        }
    }
}