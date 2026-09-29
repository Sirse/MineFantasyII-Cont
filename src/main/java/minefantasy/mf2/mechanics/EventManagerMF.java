package minefantasy.mf2.mechanics;

import net.minecraft.entity.*;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.player.*;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import minefantasy.mf2.MineFantasyII;
import minefantasy.mf2.api.heating.IHotItem;
import minefantasy.mf2.api.heating.TongsHelper;
import minefantasy.mf2.api.helpers.*;
import minefantasy.mf2.api.helpers.Cooldowns;
import minefantasy.mf2.api.rpg.LevelupEvent;
import minefantasy.mf2.api.rpg.SyncSkillEvent;
import minefantasy.mf2.api.stamina.StaminaBar;
import minefantasy.mf2.api.tool.ISmithTongs;
import minefantasy.mf2.config.ConfigHardcore;
import minefantasy.mf2.entity.EntityCogwork;
import minefantasy.mf2.entity.EntityItemUnbreakable;
import minefantasy.mf2.item.list.ComponentListMF;
import minefantasy.mf2.network.packet.LevelupPacket;
import minefantasy.mf2.network.packet.SkillPacket;
import minefantasy.mf2.util.MFLogUtil;
import minefantasy.mf2.util.XSTRandom;

/** Living and player events: injuries and hit timers, hot items and tongs, skills, respawning. */
public class EventManagerMF {

    public static final String hitspeedNBT = "MF_HitCooldown";
    public static final String injuredNBT = "MF_Injured";
    private static final XSTRandom random = new XSTRandom();

    public static void tickHitSpeeds(EntityLivingBase user) {
        Cooldowns.tick(user, hitspeedNBT);
    }

    public static void setHitTime(EntityLivingBase user, int time) {
        Cooldowns.set(user, hitspeedNBT, time);
    }

    public static int getHitspeedTime(Entity entity) {
        return Cooldowns.left(entity, hitspeedNBT);
    }

    public static int getInjuredTime(Entity entity) {
        return Cooldowns.left(entity, injuredNBT);
    }

    @SubscribeEvent
    public void spawnEntity(EntityJoinWorldEvent event) {
        if (event.entity.isDead) {
            return;
        }
        if (event.world.isRemote) {
            return;
        }
        if (event.entity instanceof EntityItem && !(event.entity instanceof EntityItemUnbreakable)) {
            EntityItem eitem = (EntityItem) event.entity;
            if (eitem.getEntityItem() != null) {
                // MF_Persist marks new mythic gear; pre-migration worlds only carry vanilla Unbreakable
                NBTTagCompound itemNBT = eitem.getEntityItem().getTagCompound();
                if (itemNBT != null
                        && (itemNBT.hasKey(EntityItemUnbreakable.persistNBT) || itemNBT.hasKey("Unbreakable"))) {
                    EntityItem newEntity = new EntityItemUnbreakable(event.world, eitem);
                    event.world.spawnEntityInWorld(newEntity);
                    eitem.setDead();
                }
                if (isDragonforge(eitem.getEntityItem())) {
                    MFLogUtil.logDebug("Found dragon heart");
                    EntityItem newEntity = new EntityItemUnbreakable(event.world, eitem);
                    event.world.spawnEntityInWorld(newEntity);
                    eitem.setDead();
                }
            }
        }
    }

    private boolean isDragonforge(ItemStack itemstack) {
        return itemstack.getItem() == ComponentListMF.dragon_heart;
    }

    @SubscribeEvent
    public void updateEntity(LivingUpdateEvent event) {
        if (event.entity instanceof EntityCogwork) {
            return;
        }
        EntityLivingBase entity = event.entityLiving;

        float lowHp = entity.getMaxHealth() / 5F;
        int injury = getInjuredTime(entity);

        if (ConfigHardcore.critLimp && (entity instanceof EntityLiving
                || !(entity instanceof EntityPlayer && ((EntityPlayer) entity).capabilities.isCreativeMode))) {
            if (entity.getHealth() <= lowHp || injury > 0) {
                if (entity.getRNG().nextInt(10) == 0 && entity.onGround && !entity.isRiding()) {
                    entity.motionX = 0F;
                    entity.motionZ = 0F;
                }
                if (entity.ticksExisted % 15 == 0) {
                    entity.limbSwing = 2.0F;
                    float x = (float) (entity.posX + (random.nextFloat() - 0.5F) / 4F);
                    float y = (float) (entity.posY + entity.getEyeHeight() + (random.nextFloat() - 0.5F) / 4F);
                    float z = (float) (entity.posZ + (random.nextFloat() - 0.5F) / 4F);
                    entity.worldObj.spawnParticle("reddust", x, y, z, 0F, 0F, 0F);
                }
            }
            if (!entity.worldObj.isRemote && entity.getHealth() <= (lowHp / 2) && entity.getRNG().nextInt(200) == 0) {
                entity.addPotionEffect(new PotionEffect(Potion.confusion.id, 100, 50));
            }
        }
        if (!entity.worldObj.isRemote) {
            Cooldowns.tick(entity, injuredNBT);
        }
        if (StaminaBar.isSystemActive && StaminaBar.doesAffectEntity(entity)) {
            StaminaMechanics.tickEntity(event.entityLiving);
        }
        tickHitSpeeds(event.entityLiving);
    }

    @SubscribeEvent
    public void clonePlayer(PlayerEvent.Clone event) {
        EntityPlayer origin = event.original;
        EntityPlayer spawn = event.entityPlayer;
        if (origin != null && spawn != null) {
            EntityHelper.cloneNBT(origin, spawn);
        }
    }

    @SubscribeEvent
    public void startUseItem(PlayerUseItemEvent.Start event) {
        EntityPlayer player = event.entityPlayer;
        if (event.item != null && event.item.getItemUseAction() == EnumAction.block) {
            if ((StaminaBar.isSystemActive && TacticalManager.shouldStaminaBlock
                    && !StaminaBar.isAnyStamina(player, false)) || !Parrying.isParryAvailable(player)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void levelup(LevelupEvent event) {
        EntityPlayer player = event.thePlayer;
        if (!player.worldObj.isRemote && player instanceof EntityPlayerMP) {
            EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
            MineFantasyII.packetHandler.sendPacketToPlayer(
                    new LevelupPacket(player, event.theSkill, event.theLevel).generatePacket(),
                    serverPlayer);
            MineFantasyII.packetHandler
                    .sendPacketToPlayer(new SkillPacket(player, event.theSkill).generatePacket(), serverPlayer);
        }
    }

    @SubscribeEvent
    public void syncSkill(SyncSkillEvent event) {
        EntityPlayer player = event.thePlayer;
        if (!player.worldObj.isRemote && player instanceof EntityPlayerMP) {
            MineFantasyII.packetHandler.sendPacketToPlayer(
                    new SkillPacket(player, event.theSkill).generatePacket(),
                    (EntityPlayerMP) player);
        }
    }

    @SubscribeEvent
    public void itemEvent(EntityItemPickupEvent event) {
        EntityPlayer player = event.entityPlayer;

        EntityItem drop = event.item;
        ItemStack item = drop.getEntityItem();
        ItemStack held = player.getHeldItem();

        if (held != null && held.getItem() instanceof ISmithTongs) {
            if (!TongsHelper.hasHeldItem(held)) {
                if (isHotItem(item)) {
                    if (TongsHelper.trySetHeldItem(held, item)) {
                        drop.setDead();

                        if (event.isCancelable()) {
                            event.setCanceled(true);
                        }
                        return;
                    }
                }
            }
        }
        {
            if (ConfigHardcore.HCChotBurn && item != null && isHotItem(item)) {
                if (event.isCancelable()) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public void wakeUp(PlayerWakeUpEvent event) {
        PlayerTickHandlerMF.wakeUp(event.entityPlayer);
    }

    private boolean isHotItem(ItemStack item) {
        return item != null && (item.getItem() instanceof IHotItem);
    }
}
