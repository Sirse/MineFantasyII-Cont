package minefantasy.mf2.mechanics;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import minefantasy.mf2.api.archery.AmmoMechanicsMF;
import minefantasy.mf2.api.archery.IFirearm;
import minefantasy.mf2.api.heating.IHotItem;
import minefantasy.mf2.api.helpers.ArmourCalculator;
import minefantasy.mf2.api.helpers.PlayerTagData;
import minefantasy.mf2.api.helpers.TacticalManager;
import minefantasy.mf2.api.knowledge.ResearchLogic;
import minefantasy.mf2.api.rpg.RPGElements;
import minefantasy.mf2.api.stamina.StaminaBar;
import minefantasy.mf2.config.ConfigHardcore;
import minefantasy.mf2.config.ConfigMobs;
import minefantasy.mf2.config.ConfigWeapon;
import minefantasy.mf2.entity.mob.EntityDragon;
import minefantasy.mf2.item.armour.ItemApron;
import minefantasy.mf2.item.food.ItemFoodMF;
import minefantasy.mf2.item.gadget.ItemCrossbow;
import minefantasy.mf2.item.list.ToolListMF;
import minefantasy.mf2.item.weapon.ItemWeaponMF;
import minefantasy.mf2.util.MFLogUtil;
import minefantasy.mf2.util.XSTRandom;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.world.EnumDifficulty;

import java.util.Iterator;
import java.util.List;

public class PlayerTickHandlerMF {
    private static ItemStack lastStack;
    private static String stepNBT = "MF_LastStep";
    private static String chunkCoords = "MF_BedPos";
    private static String resetBed = "MF_Resetbed";

    private static XSTRandom random = new XSTRandom();
    // Dragon tiers
    private static final int TIER_YOUNG = 0;
    private static final int TIER_ADULT = 1;
    private static final int TIER_MATURE = 2;
    private static final int TIER_ELDER = 3;
    private static final int TIER_ANCIENT = 4;

    private static boolean oneIn(int n) {
        return n > 0 && random.nextInt(n) == 0;
    }

    // Table-driven tier selection support
    private static class DragonTierRule {
        final int maxKills; // inclusive
        final int young;
        final int mature;
        final int adult;
        final int elder;
        final int ancient;

        DragonTierRule(int maxKills, int young, int mature, int adult, int elder, int ancient) {
            this.maxKills = maxKills;
            this.young = young;
            this.mature = mature;
            this.adult = adult;
            this.elder = elder;
            this.ancient = ancient;
        }
    }

    // Defaults matching legacy behavior
    private static final DragonTierRule[] DEFAULT_TIER_RULES = new DragonTierRule[]{
            new DragonTierRule(4, 100, 0, 0, 0, 0),
            new DragonTierRule(9, 20, 0, 80, 0, 0),
            new DragonTierRule(14, 10, 0, 90, 0, 0),
            new DragonTierRule(24, 5, 10, 85, 0, 0),
            new DragonTierRule(34, 0, 25, 75, 0, 0),
            new DragonTierRule(49, 0, 0, 100, 0, 0),
            new DragonTierRule(75, 0, 70, 10, 20, 0),
            new DragonTierRule(Integer.MAX_VALUE, 0, 49, 0, 50, 1)
    };

    private static volatile DragonTierRule[] parsedTierRules;
    private static volatile String lastTierRulesSpec;

    private static DragonTierRule[] getConfiguredTierRules() {
        String spec = ConfigMobs.dragonTierRules;
        if (parsedTierRules != null && spec != null && spec.equals(lastTierRulesSpec)) {
            return parsedTierRules;
        }

        DragonTierRule[] parsed = parseTierRules(spec);
        if (parsed == null || parsed.length == 0) {
            MFLogUtil.logDebug("Using default dragon tier rules (parse failed or empty)");
            parsed = DEFAULT_TIER_RULES;
        }
        parsedTierRules = parsed;
        lastTierRulesSpec = spec;
        return parsedTierRules;
    }

    private static DragonTierRule[] parseTierRules(String spec) {
        try {
            if (spec == null || spec.trim().isEmpty()) return DEFAULT_TIER_RULES;
            String[] rows = spec.split("[;\r\n]+");
            java.util.ArrayList<DragonTierRule> list = new java.util.ArrayList<DragonTierRule>(rows.length);
            for (String row : rows) {
                row = row.trim();
                // strip inline comments (# ... or // ...)
                int hash = row.indexOf('#');
                if (hash >= 0) row = row.substring(0, hash).trim();
                int slashes = row.indexOf("//");
                if (slashes >= 0) row = row.substring(0, slashes).trim();
                if (row.isEmpty()) continue;
                String[] parts = row.split(":");
                if (parts.length != 2) {
                    MFLogUtil.logDebug("Invalid dragon tier row (missing ':'): " + row);
                    continue;
                }
                int maxKills = Integer.parseInt(parts[0].trim());
                String[] probs = parts[1].split(",");
                if (probs.length != 5) {
                    MFLogUtil.logDebug("Invalid dragon tier row (need 5 probabilities): " + row);
                    continue;
                }
                int young = clampPercent(probs[0]);
                int mature = clampPercent(probs[1]);
                int adult = clampPercent(probs[2]);
                int elder = clampPercent(probs[3]);
                int ancient = clampPercent(probs[4]);
                list.add(new DragonTierRule(maxKills, young, mature, adult, elder, ancient));
            }
            if (list.isEmpty()) return DEFAULT_TIER_RULES;
            // Ensure sorted by maxKills
            list.sort(new java.util.Comparator<DragonTierRule>() {
                @Override
                public int compare(DragonTierRule a, DragonTierRule b) {
                    return Integer.compare(a.maxKills, b.maxKills);
                }
            });
            return list.toArray(new DragonTierRule[0]);
        } catch (Exception ex) {
            MFLogUtil.logDebug("Failed to parse dragon tier rules: " + ex.getMessage());
            return DEFAULT_TIER_RULES;
        }
    }

    private static int clampPercent(String s) {
        try {
            int v = Integer.parseInt(s.trim());
            if (v < 0) return 0;
            if (v > 100) return 100;
            return v;
        } catch (Exception ignore) {
            return 0;
        }
    }

    public static void spawnDragon(EntityPlayer player) {
        spawnDragon(player, 64);
    }

    public static void spawnDragon(EntityPlayer player, int offset) {
        int y = MathHelper.floor_double(player.posY + offset);
        int maxY = Math.max(1, player.worldObj.getActualHeight() - 1);
        if (y < 1) y = 1;
        if (y > maxY) y = maxY;
        if (!player.worldObj.isRemote && canDragonSpawnOnPlayer(player, y)) {
            int tier = getDragonTier(player);// Gets tier on kills
            EntityDragon dragon = new EntityDragon(player.worldObj);
            dragon.setPosition(player.posX, y, player.posZ);
            player.worldObj.spawnEntityInWorld(dragon);
            dragon.setDragon(tier);
            dragon.disengage(100);
            player.worldObj.playSoundEffect(dragon.posX, dragon.posY - 16D, dragon.posZ, "mob.enderdragon.growl", 3.0F,
                    1.5F);
            dragon.fireBreathCooldown = 200;

            if (ConfigMobs.dragonMSG) {
                player.addChatMessage(new ChatComponentText(
                        EnumChatFormatting.GOLD + StatCollector.translateToLocal("event.dragonnear.name")));

                List<?> list = player.worldObj.playerEntities;
                for (Object instance : list) {
                    if (instance instanceof EntityPlayer) {
                        if (((EntityPlayer) instance).getDistanceToEntity(player) < 256D && instance != player) {
                            ((EntityPlayer) instance).addChatMessage(new ChatComponentText(
                                    EnumChatFormatting.GOLD + StatCollector.translateToLocal("event.dragonnear.name")));
                        }
                    }
                }
            }
        }
    }

    private static boolean canDragonSpawnOnPlayer(EntityPlayer player, int y) {
        int baseX = MathHelper.floor_double(player.posX);
        int baseZ = MathHelper.floor_double(player.posZ);
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                if (!player.worldObj.canBlockSeeTheSky(baseX + x, y, baseZ + z)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static int getDragonTier(EntityPlayer player) {
        int kills = getDragonEnemyPoints(player);
        DragonTierRule[] rules = getConfiguredTierRules();
        for (DragonTierRule rule : rules) {
            if (kills <= rule.maxKills) {
                int roll = random.nextInt(100) + 1; // 1..100
                int r = roll;
                if ((r -= rule.young) <= 0) return TIER_YOUNG;
                if ((r -= rule.mature) <= 0) return TIER_MATURE;
                if ((r -= rule.adult) <= 0) return TIER_ADULT;
                if ((r -= rule.elder) <= 0) return TIER_ELDER;
                return TIER_ANCIENT;
            }
        }
        return TIER_YOUNG; // fallback
    }

    public static void addDragonKill(EntityPlayer player) {
        addDragonEnemyPts(player, 1);
    }

    public static void addDragonEnemyPts(EntityPlayer player, int i) {
        setDragonEnemyPts(player, getDragonEnemyPoints(player) + i);
    }

    public static void setDragonEnemyPts(EntityPlayer player, int i) {
        if (i < 0) {
            i = 0;
        }
        NBTTagCompound nbt = PlayerTagData.getPersistedData(player);
        nbt.setInteger("MF_DragonKills", i);
    }

    public static int getDragonEnemyPoints(EntityPlayer player) {
        NBTTagCompound nbt = PlayerTagData.getPersistedData(player);
        return nbt.getInteger("MF_DragonKills");
    }

    public static void wakeUp(EntityPlayer player) {
        if (StaminaBar.isSystemActive) {
            StaminaBar.setStaminaValue(player, StaminaBar.getBaseMaxStamina(player));
        }
        if (player.getEntityData().hasKey(chunkCoords + "_x")) {
            player.getEntityData().setBoolean(resetBed, true);
        }
    }

    public static void readyToResetBedPosition(EntityPlayer player) {
        ChunkCoordinates coords = player.getBedLocation(player.dimension);
        if (coords != null) {
            player.getEntityData().setInteger(chunkCoords + "_x", coords.posX);
            player.getEntityData().setInteger(chunkCoords + "_y", coords.posY);
            player.getEntityData().setInteger(chunkCoords + "_z", coords.posZ);
        }
    }

    // SPRINT JUMPING
    // DEFAULT:= 0:22 (50seconds till starve, 35s till nosprint) (16m in MC time for
    // 4 missing bars)
    // SLOW=5: = 2:20 (5mins till starve, 3:30 till nosprint) (1h 40m in MC time for
    // 4 missing bars)
    // EXHAUSTION SCALE = 3.0F = 1hunger
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ItemStack held = event.player.getHeldItem();
            if (held != null) {
                int parry = ItemWeaponMF.getParry(held);
                if (parry > 0) {
                    ItemWeaponMF.setParry(held, parry - 1);
                }
            }

            // COMMON
            TacticalManager.applyArmourWeight(event.player);

            if (event.player.worldObj.isRemote) {
                if (isNextStep(event.player)) {
                    onStep(event.player, event.player.getEntityData().getInteger(stepNBT) % 2 == 0);
                }
            }
            /*
             * if(RPGElements.isSystemActive) { if(event.player.isSprinting() &&
             * event.player.ticksExisted % 10 == 0) {
             * SkillList.athletics.addXP(event.player, 1); } else
             * if(event.player.isSneaking() && TacticalManager.isEntityMoving(event.player)
             * && event.player.ticksExisted % 10 == 0) { SkillList.sneak.addXP(event.player,
             * 1); } }
             */
            // DRAGON EVENT
            if (!event.player.worldObj.isRemote) {
                tickDragonSpawner(event.player);
            }
            if (!event.player.worldObj.isRemote) {
                tryResetBed(event.player);
            }
            // updatePitch(event.player); (This keeps track of player camera angles, could
            // make a mechanic based on swinging the camera)
        }

        if (event.phase == TickEvent.Phase.START) {
            applyBalance(event.player);
            ItemFoodMF.onTick(event.player);

            if (!event.player.worldObj.isRemote
                    && !(!ConfigHardcore.HCChotBurn && ItemApron.isUserProtected(event.player))
                    && event.player.ticksExisted % 100 == 0) {
                for (int a = 0; a < event.player.inventory.getSizeInventory(); a++) {
                    ItemStack item = event.player.inventory.getStackInSlot(a);
                    if (item != null && item.getItem() instanceof IHotItem) {
                        event.player.setFire(5);
                        event.player.attackEntityFrom(DamageSource.onFire, 1.0F);
                        break;
                    }
                }
            }
            if (event.player.worldObj.isRemote) {
                ItemStack item = event.player.getHeldItem();
                if (lastStack == null && item != null) {
                    if (item.getItem() instanceof IFirearm) {
                        NBTTagCompound nbt = AmmoMechanicsMF.getNBT(item);
                        if (nbt.hasKey(ItemCrossbow.useTypeNBT)
                                && nbt.getString(ItemCrossbow.useTypeNBT).equalsIgnoreCase("fire")) {
                            nbt.setString(ItemCrossbow.useTypeNBT, "null");
                        }
                    }
                }
                if (lastStack != null && (item == null || item != lastStack)) {
                    if (lastStack.getItem() instanceof IFirearm) {
                        NBTTagCompound nbt = AmmoMechanicsMF.getNBT(lastStack);
                        if (nbt.hasKey(ItemCrossbow.useTypeNBT)
                                && nbt.getString(ItemCrossbow.useTypeNBT).equalsIgnoreCase("fire")) {
                            nbt.setString(ItemCrossbow.useTypeNBT, "null");
                        }
                    }
                }
                lastStack = item;
            }

            float weight = ArmourCalculator.getTotalWeightOfWorn(event.player, false);
            if (weight > 100F) {
                if (event.player.isInWater()) {
                    event.player.motionY -= (weight / 20000F);
                }
            }
        }
    }

    private void onStep(EntityPlayer player, boolean alternateStep) {
        if (ArmourCalculator.getTotalWeightOfWorn(player, false) >= 50) {
            player.playSound("mob.irongolem.throw", 1.0F, 1.0F);
        }
    }

    private boolean isNextStep(EntityPlayer player) {
        int prevStep = player.getEntityData().getInteger(stepNBT);
        int stepcount = (int) player.distanceWalkedOnStepModified;
        player.getEntityData().setInteger(stepNBT, stepcount);

        return prevStep != stepcount;
    }

    /*
     * private static String lastPitchNBT = "MF_last_AimPitch"; public static void
     * setLastPitch(EntityPlayer user, float value) {
     * user.getEntityData().setFloat(lastPitchNBT, value); } public static void
     * updatePitch(EntityPlayer user) { user.getEntityData().setFloat(lastPitchNBT,
     * user.rotationPitch); } public static float getPitchMovement(EntityPlayer
     * user) { float lastPitch = user.getEntityData().getFloat(lastPitchNBT) +
     * 1000F; float nowPitch = user.rotationPitch + 1000F;
     *
     * return nowPitch - lastPitch; }
     */
    private void tickDragonSpawner(EntityPlayer player) {
        if (player.worldObj.difficultySetting != EnumDifficulty.PEACEFUL && player.dimension == 0) {
            int i = ConfigMobs.dragonInterval;
            float chance = ConfigMobs.dragonChance;

            if (PlayerTickHandlerMF.getDragonEnemyPoints(player) >= 100) {
                i /= 2;// twice as frequent
            }
            if (PlayerTickHandlerMF.getDragonEnemyPoints(player) >= 50) {
                chance *= 2;// twice the chance
            }
            i = Math.max(1, i); // guard against division by zero
            if (!player.worldObj.isRemote && player.worldObj.getTotalWorldTime() % i == 0
                    && random.nextFloat() * 100F < chance) {
                spawnDragon(player);
            }
        }
    }

    private void applyBalance(EntityPlayer entityPlayer) {
        float weight = 2.0F;
        float pitchBalance = entityPlayer.getEntityData().hasKey("MF_Balance_Pitch")
                ? entityPlayer.getEntityData().getFloat("MF_Balance_Pitch")
                : 0F;
        float yawBalance = entityPlayer.getEntityData().hasKey("MF_Balance_Yaw")
                ? entityPlayer.getEntityData().getFloat("MF_Balance_Yaw")
                : 0F;

        if (pitchBalance > 0) {
            if (pitchBalance < 1.0F && pitchBalance > -1.0F)
                weight = pitchBalance;
            pitchBalance -= weight;

            if (ConfigWeapon.useBalance) {
                entityPlayer.rotationPitch += pitchBalance > 0 ? weight : -weight;
            }

            if (pitchBalance < 0)
                pitchBalance = 0;
        }
        if (yawBalance > 0) {
            if (yawBalance < 1.0F && yawBalance > -1.0F)
                weight = yawBalance;
            yawBalance -= weight;

            if (ConfigWeapon.useBalance) {
                entityPlayer.rotationYaw += yawBalance > 0 ? weight : -weight;
            }

            if (yawBalance < 0)
                yawBalance = 0;
        }
        entityPlayer.getEntityData().setFloat("MF_Balance_Pitch", pitchBalance);
        entityPlayer.getEntityData().setFloat("MF_Balance_Yaw", yawBalance);
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        onPlayerEnterWorld(event.player);
    }

    @SubscribeEvent
    public void onPlayerTravel(PlayerEvent.PlayerChangedDimensionEvent event) {
        onPlayerEnterWorld(event.player);
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        onPlayerEnterWorld(event.player);
    }

    public void onPlayerEnterWorld(EntityPlayer player) {
        if (player.worldObj.isRemote)
            return;

        NBTTagCompound persist = PlayerTagData.getPersistedData(player);
        MFLogUtil.logDebug("Sync data");
        ResearchLogic.syncData(player);

        if (!persist.hasKey("MF_HasBook")) {
            persist.setBoolean("MF_HasBook", true);
            if (player.capabilities.isCreativeMode)
                return;

            player.inventory.addItemStackToInventory(new ItemStack(ToolListMF.researchBook));
        }
        if (RPGElements.isSystemActive) {
            RPGElements.initSkills(player);
        }
    }

    private void tryResetBed(EntityPlayer player) {
        if (player.getEntityData().hasKey(resetBed)) {
            player.getEntityData().removeTag(resetBed);
            resetBedPosition(player);
        }
    }

    private void resetBedPosition(EntityPlayer player) {
        if (player.getEntityData().hasKey(chunkCoords + "_x")) {
            MFLogUtil.logDebug("Reset bed data for " + player.getCommandSenderName());
            int x = player.getEntityData().getInteger(chunkCoords + "_x");
            int y = player.getEntityData().getInteger(chunkCoords + "_y");
            int z = player.getEntityData().getInteger(chunkCoords + "_z");
            ChunkCoordinates coords = new ChunkCoordinates(x, y, z);

            player.getEntityData().removeTag(chunkCoords + "_x");
            player.getEntityData().removeTag(chunkCoords + "_y");
            player.getEntityData().removeTag(chunkCoords + "_z");

            player.setSpawnChunk(coords, false);
        }
    }
}
