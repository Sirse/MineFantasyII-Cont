package minefantasy.mf2.mechanics;

import java.util.ArrayList;
import java.util.Iterator;

import net.minecraft.entity.*;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.*;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import minefantasy.mf2.api.helpers.*;
import minefantasy.mf2.api.tool.IHuntingItem;
import minefantasy.mf2.config.ConfigExperiment;
import minefantasy.mf2.config.ConfigHardcore;
import minefantasy.mf2.entity.mob.EntityDragon;
import minefantasy.mf2.item.food.FoodListMF;
import minefantasy.mf2.item.list.ComponentListMF;
import minefantasy.mf2.item.list.ToolListMF;
import minefantasy.mf2.util.XSTRandom;

/** What creatures drop when they die, and what their deaths count towards. */
public class LootEvents {

    private static final XSTRandom random = new XSTRandom();

    @SubscribeEvent
    public void tryDropItems(LivingDropsEvent event) {
        if (event.entityLiving.worldObj.isRemote) {
            return;
        }
        EntityLivingBase dropper = event.entityLiving;

        if (dropper instanceof EntityChicken) {
            // Archery buff: chickens always drop feathers (base 1-4, +1 per Looting level)
            int dropCount = 1 + random.nextInt(event.lootingLevel + 4);

            for (int a = 0; a < dropCount; a++) {
                dropper.entityDropItem(new ItemStack(Items.feather), 0.0F);
            }
        }
        if (dropper.getEntityData().hasKey("MF_LootDrop")) {
            int id = dropper.getEntityData().getInteger("MF_LootDrop");
            Item drop = id == 0 ? ToolListMF.loot_sack : id == 1 ? ToolListMF.loot_sack_uc : ToolListMF.loot_sack_rare;
            dropper.entityDropItem(new ItemStack(drop), 0.0F);
        }
        if (dropper instanceof EntityAgeable && dropper.getCreatureAttribute() != EnumCreatureAttribute.UNDEAD) {
            if (random.nextFloat() * (1 + event.lootingLevel) < 0.05F) {
                dropper.entityDropItem(new ItemStack(FoodListMF.guts), 0.0F);
            }
        }
        if (dropper instanceof IAnimals && !(dropper instanceof IMob)) {
            if (ConfigHardcore.hunterKnife && !dropper.getEntityData().hasKey("hunterKill")) {
                event.setCanceled(true);
                return;
            }
            if (ConfigHardcore.lessHunt) {
                alterDrops(dropper, event);
            }
        }
        if (dropper instanceof EntityHorse) {
            int dropCount = random.nextInt(3 + event.lootingLevel);
            if (ConfigHardcore.lessHunt) {
                dropCount = 1 + random.nextInt(event.lootingLevel + 1);
            }

            Item meat = dropper.isBurning() ? FoodListMF.horse_cooked : FoodListMF.horse_raw;
            for (int a = 0; a < dropCount; a++) {
                dropper.entityDropItem(new ItemStack(meat), 0.0F);
            }
        }
        if (dropper instanceof EntityWolf) {
            int dropCount = random.nextInt(3 + event.lootingLevel);
            if (ConfigHardcore.lessHunt) {
                dropCount = 1 + random.nextInt(event.lootingLevel + 1);
            }

            Item meat = dropper.isBurning() ? FoodListMF.wolf_cooked : FoodListMF.wolf_raw;
            for (int a = 0; a < dropCount; a++) {
                dropper.entityDropItem(new ItemStack(meat), 0.0F);
            }
        }
        dropLeather(event.entityLiving, event);

        if (dropper instanceof EntitySkeleton) {
            EntitySkeleton skeleton = (EntitySkeleton) dropper;

            if ((skeleton.getHeldItem() == null || !(skeleton.getHeldItem().getItem() instanceof ItemBow))
                    && event.drops != null
                    && !event.drops.isEmpty()) {
                Iterator<EntityItem> list = event.drops.iterator();

                while (list.hasNext()) {
                    EntityItem entItem = list.next();
                    ItemStack drop = entItem.getEntityItem();

                    if (drop.getItem() == Items.arrow) {
                        list.remove();
                    }
                }
            }
        }
    }

    private void dropLeather(EntityLivingBase mob, LivingDropsEvent event) {
        boolean dropHide = shouldAnimalDropHide(mob);
        Item hide = getHideFor(mob);

        if (event.drops != null && !event.drops.isEmpty()) {
            Iterator<EntityItem> list = event.drops.iterator();

            while (list.hasNext()) {
                EntityItem entItem = list.next();
                ItemStack drop = entItem.getEntityItem();

                if (drop.getItem() == Items.leather) {
                    // Remove vanilla leather drop; we'll replace with hide below
                    list.remove();
                    dropHide = true;
                }
            }
        }
        if (dropHide && hide != null && !(ConfigHardcore.hunterKnife && !mob.getEntityData().hasKey("hunterKill"))) {
            mob.entityDropItem(new ItemStack(hide), 0.0F);
        }
    }

    private Item getHideFor(EntityLivingBase mob) {
        Item[] hide = new Item[] { ComponentListMF.rawhideSmall, ComponentListMF.rawhideMedium,
                ComponentListMF.rawhideLarge };
        int size = getHideSizeFor(mob);
        if (mob.isChild()) {
            size--;
        }

        if (size <= 0) {
            return null;
        }
        if (size > hide.length) {
            size = hide.length;
        }

        return hide[size - 1];
    }

    private int getHideSizeFor(EntityLivingBase mob) {
        if (mob instanceof EntityCow || mob instanceof EntityHorse) {
            return 3;
        }
        if (mob instanceof EntitySheep) {
            return 2;
        }
        if (mob instanceof EntityPig) {
            return 1;
        }

        int size = mob.myEntitySize.ordinal() + 1;
        if (size <= 1) {
            return 0;
        }
        if (size == 2) {
            return 1;
        } else if (size <= 4) {
            return 2;
        }
        return 3;
    }

    private boolean shouldAnimalDropHide(EntityLivingBase mob) {
        if (mob instanceof EntityWolf || mob instanceof EntityCow
                || mob instanceof EntityPig
                || mob instanceof EntitySheep
                || mob instanceof EntityHorse) {
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (!event.entityLiving.worldObj.isRemote && event.entityLiving instanceof EntityDragon
                && event.source != null
                && event.source.getEntity() != null
                && event.source.getEntity() instanceof EntityPlayer) {
            PlayerTickHandlerMF.addDragonKill((EntityPlayer) event.source.getEntity());
        }
        if (!event.entityLiving.worldObj.isRemote && event.entityLiving instanceof EntityPlayer
                && event.source != null
                && event.source.getEntity() != null
                && event.source.getEntity() instanceof EntityDragon) {
            PlayerTickHandlerMF.addDragonEnemyPts((EntityPlayer) event.entityLiving, -1);
        }
        Entity dropper = event.entity;

        if (dropper != null && ConfigExperiment.stickArrows && !dropper.worldObj.isRemote) {
            ArrayList<ItemStack> stuckArrows = (ArrayList<ItemStack>) ArrowEffectsMF.getStuckArrows(dropper);
            if (!stuckArrows.isEmpty()) {

                for (ItemStack arrow : stuckArrows) {
                    if (arrow != null) {
                        dropper.entityDropItem(arrow, 0.0F);
                    }
                }
            }
        }

    }

    public void alterDrops(EntityLivingBase dropper, LivingDropsEvent event) {
        ArrayList<ItemStack> meats = new ArrayList<ItemStack>();

        if (event.drops != null && !event.drops.isEmpty()) {
            Iterator<EntityItem> list = event.drops.iterator();

            while (list.hasNext()) {
                EntityItem entItem = list.next();
                ItemStack drop = entItem.getEntityItem();
                boolean dropItem = true;

                if (drop.getItem() instanceof ItemFood) {
                    // Remove original food drop from the drop list; we'll re-add deduped items
                    list.remove();

                    if (!meats.isEmpty()) {
                        for (ItemStack compare : meats) {
                            if (drop.isItemEqual(compare)) {
                                dropItem = false;
                                break;
                            }
                        }
                    }
                    if (dropItem) {
                        drop.stackSize = 1;
                        if (event.lootingLevel > 0) {
                            drop.stackSize += dropper.getRNG().nextInt(event.lootingLevel + 1);
                        }
                        meats.add(drop.copy());
                    }
                }
            }

            for (int a = 0; a < meats.size(); a++) {
                ItemStack meat = meats.get(a);
                dropper.entityDropItem(meat, 0.0F);
            }
        }
    }

    @SubscribeEvent
    public void killEntity(LivingDeathEvent event) {
        // killsCount
        EntityLivingBase dead = event.entityLiving;
        EntityLivingBase hunter;
        ItemStack weapon = null;
        DamageSource source = event.source;

        if (dead instanceof EntityWitch) {
            dropBook(dead, 0);
        }
        if (dead instanceof EntityVillager) {
            dropBook(dead, 1);
        }
        if (dead instanceof EntityZombie) {
            dropBook(dead, 2);
        }
        if (source != null && source.getEntity() != null) {
            if (source.getEntity() instanceof EntityLivingBase) {
                hunter = (EntityLivingBase) source.getEntity();
                weapon = hunter.getHeldItem();
                if (hunter instanceof EntityPlayer) {
                    addKill((EntityPlayer) hunter, dead);
                }
            }
        }
        if (weapon != null) {
            String type = ToolHelper.getCrafterTool(weapon);
            if (weapon.getItem() instanceof IHuntingItem) {
                if (((IHuntingItem) weapon.getItem()).canRetrieveDrops(weapon)) {
                    dead.getEntityData().setBoolean("hunterKill", true);
                }
            } else if (type != null && type.equalsIgnoreCase("knife")) {
                dead.getEntityData().setBoolean("hunterKill", true);
            }
        }
    }

    private void dropBook(EntityLivingBase dead, int id) {
        if (dead.worldObj.isRemote) return;
        Item book = null;
        if (id == 0) {
            float chance = random.nextFloat();
            if (chance > 0.75F) {
                book = ToolListMF.skillbook_engineering;
            } else {
                book = ToolListMF.skillbook_provisioning;
            }
        } else if (id == 1 && random.nextInt(5) == 0) {
            float chance = random.nextFloat();
            if (chance > 0.9F) {
                book = ToolListMF.skillbook_engineering;
            } else if (chance > 0.6F) {
                book = ToolListMF.skillbook_artisanry;
            } else if (chance > 0.3F) {
                book = ToolListMF.skillbook_construction;
            } else {
                book = ToolListMF.skillbook_provisioning;
            }
        } else if (id == 2 && random.nextInt(25) == 0) {
            float chance = random.nextFloat();
            if (chance > 0.9F) {
                book = ToolListMF.skillbook_engineering;
            } else if (chance > 0.6F) {
                book = ToolListMF.skillbook_artisanry;
            } else if (chance > 0.3F) {
                book = ToolListMF.skillbook_construction;
            } else {
                book = ToolListMF.skillbook_provisioning;
            }
        }
        if (book != null) {
            dead.entityDropItem(new ItemStack(book), 0F);
        }
    }

    private void addKill(EntityPlayer hunter, EntityLivingBase dead) {
        addKillTo(hunter, "killsCount");
        if (dead instanceof IMob) {
            addKillTo(hunter, "killsCountMob");
        } else if (dead instanceof IAnimals) {
            addKillTo(hunter, "killsCountAnimal");
        }
        if (dead instanceof EntityPlayer) {
            addKillTo(hunter, "killsCountPlayer");
        }
    }

    private void addKillTo(EntityPlayer hunter, String type) {
        int kills = hunter.getEntityData().hasKey(type) ? hunter.getEntityData().getInteger(type) : 0;
        kills++;
        hunter.getEntityData().setInteger(type, kills);
    }
}
