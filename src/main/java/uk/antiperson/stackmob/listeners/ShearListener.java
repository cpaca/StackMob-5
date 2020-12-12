package uk.antiperson.stackmob.listeners;

import org.bukkit.Material;
import org.bukkit.block.Dispenser;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockShearEntityEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.loot.LootContext;
import org.bukkit.material.Wool;
import uk.antiperson.stackmob.StackMob;
import uk.antiperson.stackmob.entity.Drops;
import uk.antiperson.stackmob.entity.StackEntity;

import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;

@ListenerMetadata(config = "events.shear.enabled")
public class ShearListener implements Listener {

    private final StackMob sm;

    public ShearListener(StackMob sm) {
        this.sm = sm;
    }

    @EventHandler
    public void onShearSheep(PlayerShearEntityEvent event) {
        if (event.isCancelled()) {
            return;
        }
        EquipmentSlot equipmentSlot = findShears(event.getPlayer());
        if (equipmentSlot == null) {
            sm.getLogger().info("A player just managed to shear an entity while not holding shears.");
            return;
        }
        ItemStack is = shearLogic((LivingEntity) event.getEntity(), event.getPlayer().getInventory().getItem(equipmentSlot));
        if (is == null) {
            return;
        }
        event.getPlayer().getInventory().setItem(equipmentSlot, is);
    }

    @EventHandler
    public void onShearSheep(BlockShearEntityEvent event) {
        if (event.isCancelled()) {
            return;
        }
        ItemStack is = shearLogic((LivingEntity) event.getEntity(), event.getTool());
        if (is == null) {
            return;
        }
        sm.getServer().getScheduler().runTask(sm, () -> {
            Dispenser dispenser = (Dispenser) event.getBlock().getState();
            dispenser.getInventory().setItem(dispenser.getInventory().first(event.getTool()), is);
        });
    }

    private EquipmentSlot findShears(Player player) {
        for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
            if (equipmentSlot != EquipmentSlot.HAND && equipmentSlot != EquipmentSlot.OFF_HAND) {
                continue;
            }
            if (player.getInventory().getItem(equipmentSlot).getType() != Material.SHEARS) {
                continue;
            }
            return equipmentSlot;
        }
        return null;
    }

    private ItemStack shearLogic(LivingEntity entity, ItemStack item) {
        if (!((entity instanceof Sheep) || (entity instanceof MushroomCow))) {
            return null;
        }
        StackEntity stackEntity = sm.getEntityManager().getStackEntity(entity);
        if (stackEntity == null || stackEntity.isSingle()) {
            return null;
        }
        ListenerMode shear = sm.getMainConfig().getListenerMode(entity.getType(), "shear");
        if (shear == ListenerMode.SPLIT) {
            StackEntity slice = stackEntity.slice();
            if (slice.getEntity() instanceof Sheep) {
                ((Sheep) slice.getEntity()).setSheared(false);
            }
            return null;
        }
        Damageable damageable = (Damageable) item.getItemMeta();
        int health = item.getType().getMaxDurability() - damageable.getDamage();
        stackEntity.splitIfNotEnough(health);
        int damage = health - stackEntity.getSize();
        if (damage > 0) {
            damageable.setDamage(damageable.getDamage() + stackEntity.getSize());
            item.setItemMeta((ItemMeta) damageable);
        } else {
            item = new ItemStack(Material.AIR);
        }
        if (entity instanceof Sheep) {
            Sheep sheared = (Sheep) entity;
            LootContext lootContext = new LootContext.Builder(sheared.getLocation()).lootedEntity(sheared).build();
            Collection<ItemStack> loot = sheared.getLootTable().populateLoot(ThreadLocalRandom.current(), lootContext);
            for (ItemStack itemStack : loot) {
                if (itemStack.getData() instanceof Wool) {
                    int woolAmount = (int) Math.round(stackEntity.getSize() * ThreadLocalRandom.current().nextDouble(1, 2));
                    Drops.dropItem(sheared.getLocation(), itemStack, woolAmount);
                }
            }
            return item;
        }
        MushroomCow mushroomCow = (MushroomCow) entity;
        ItemStack mushrooms = new ItemStack(Material.RED_MUSHROOM, 1);
        Drops.dropItem(mushroomCow.getLocation(), mushrooms, (stackEntity.getSize() - 1) * 5);
        // Spawn separate normal cow for the rest of the stack.
        Entity cow = mushroomCow.getWorld().spawnEntity(mushroomCow.getLocation(), EntityType.COW);
        StackEntity stackCow = sm.getEntityManager().getStackEntity((LivingEntity) cow);
        stackCow.setSize(stackEntity.getSize() - 1);
        return item;
    }

}
