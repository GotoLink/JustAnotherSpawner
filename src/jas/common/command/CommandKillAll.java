package jas.common.command;

import jas.common.JustAnotherSpawner;
import jas.common.spawner.EntityCounter;
import jas.common.spawner.EntityCounter.CountableInt;
import jas.common.spawner.creature.handler.LivingGroupRegistry;
import jas.common.spawner.creature.handler.LivingHandler;
import jas.common.spawner.creature.type.CreatureTypeRegistry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;

import com.google.common.collect.ImmutableCollection;

public class CommandKillAll extends CommandJasBase {
    @Override
    public String getCommandName() {
        return "killall";
    }

    /**
     * Return the required permission level for this command.
     */
    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getCommandUsage(ICommandSender commandSender) {
        return "commands.jaskillall.usage";
    }

    @SuppressWarnings("unchecked")
    @Override
    public void process(ICommandSender commandSender, String[] stringArgs) {
        if (stringArgs.length >= 4) {
            throw new WrongUsageException("commands.jaskillall.usage", new Object[0]);
        }

        EntityPlayerMP targetPlayer;
        if (stringArgs.length > 1) {
            targetPlayer = func_82359_c(commandSender, stringArgs[0]);
        } else {
            targetPlayer = func_82359_c(commandSender, commandSender.getCommandSenderName());
        }

        String entityCategName = stringArgs.length == 0 ? "*" : stringArgs.length == 1 ? stringArgs[0] : stringArgs[1];

        String entityFilter = stringArgs.length == 3 ? stringArgs[2] : "";

        EntityCounter deathCount = new EntityCounter();
        int totalDeaths = 0;
        Iterator<Entity> iterator = targetPlayer.worldObj.loadedEntityList.iterator();
        while (iterator.hasNext()) {
            Entity entity = iterator.next();
            if (!(entity instanceof EntityLiving)) {
                continue;
            }
            LivingGroupRegistry groupRegistry = JustAnotherSpawner.worldSettings().livingGroupRegistry();
            List<LivingHandler> handlers = JustAnotherSpawner.worldSettings().livingHandlerRegistry()
                    .getLivingHandlers((Class<? extends EntityLiving>) entity.getClass());
            if (handlers.isEmpty()) {
                if (entityCategName.equals("*") && isEntityFiltered(entity, entityFilter, groupRegistry)) {
                    entity.setDead();
                    deathCount.incrementOrPutIfAbsent(CreatureTypeRegistry.NONE, 1);
                    totalDeaths++;
                }
            } else {
                for (LivingHandler handler : handlers) {
                    if ((entityCategName.equals("*") || handler.creatureTypeID.equals(entityCategName))
                            && isEntityFiltered(entity, entityFilter, groupRegistry)) {
                        if (!handler.creatureTypeID.equals(CreatureTypeRegistry.NONE) || entity instanceof EntityLiving) {
                            entity.setDead();
                            deathCount.incrementOrPutIfAbsent(handler.creatureTypeID, 1);
                            totalDeaths++;
                            /* Break out of searching multiple GroupID such that same entity death is not counted twice */
                            break;
                        }
                    }
                }
            }
        }

        StringBuilder deathMessage = new StringBuilder();
        deathMessage.append("Total Deaths ").append(totalDeaths).append(": ");
        boolean first = true;
        for (Entry<String, CountableInt> entry : deathCount.countingHash.entrySet()) {
            if (first) {
                first = false;
            } else {
                deathMessage.append(", ");
            }
            deathMessage.append(entry.getValue().get()).append("-").append(entry.getKey());
        }
        commandSender.sendChatToPlayer(deathMessage.toString());
    }

    private boolean isEntityFiltered(Entity entity, String filter, LivingGroupRegistry groupRegistry) {
        String name = groupRegistry.EntityClasstoJASName.get(entity.getClass());
        if (filter.equals("") || name != null && name.contains(filter)) {
            return true;
        }
        return false;
    }

    /**
     * Adds the strings available in this command to the given list of tab completion options.
     */
    @Override
    public List<String> getTabCompletions(ICommandSender commandSender, String[] stringArgs) {
        List<String> tabCompletions = new ArrayList<String>();
        if (stringArgs.length == 1) {
            addPlayerUsernames(tabCompletions);
            addEntityTypes(tabCompletions);
        } else if (stringArgs.length == 2) {
            addEntityTypes(tabCompletions);
        }

        if (!tabCompletions.isEmpty()) {
            return getStringsMatchingLastWord(stringArgs, tabCompletions);
        } else {
            return tabCompletions;
        }
    }
}