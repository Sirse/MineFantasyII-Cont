package minefantasy.mf2.mechanics;

import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import minefantasy.mf2.api.helpers.*;
import minefantasy.mf2.api.weapon.*;
import minefantasy.mf2.integration.Offhand;
import minefantasy.mf2.item.weapon.*;
import minefantasy.mf2.network.packet.DodgeCommand;

/** Dodging with a raised guard: the client trigger on jumping, and the server checking the jump it saw. */
public class Dodging {

    @SubscribeEvent
    public void onJump(LivingJumpEvent event) {
        if (event.entityLiving.worldObj.isRemote && event.entityLiving instanceof EntityPlayer) {
            tryDodge((EntityPlayer) event.entityLiving);
        }
    }

    @SubscribeEvent
    public void onUpdate(LivingUpdateEvent event) {
        if (event.entityLiving instanceof EntityPlayer && !event.entityLiving.worldObj.isRemote) {
            trackDodgeWindow((EntityPlayer) event.entityLiving);
        }
    }

    /**
     * Client-sided dodge
     */
    private static void commandDodge(EntityPlayer user, int type) {
        initDodge(user, type);
        ((EntityClientPlayerMP) user).sendQueue.addToSendQueue(new DodgeCommand(user, type).generatePacket());
    }

    public static void initDodge(EntityPlayer user, int type) {
        if (!canDodge(user)) {
            return;
        }
        float bulk = ArmourCalculator.getTotalBulk(user);
        int cost = (int) ((type == 0 ? 15 : 10) * (bulk + 1));// Medium armour cost 2x more

        if (bulk <= 1.0F && ItemWeaponMF.tryPerformAbility(user, cost)) {
            float force = 1.0F - (bulk * 0.25F);// Medium armour gives 75%

            float direction = user.rotationYaw;
            if (type == 0) direction += 180;// BACK
            if (type == 1) direction -= 90;// LEFT
            if (type == -1) direction += 90;// RIGHT
            TacticalManager.leap(user, direction, force, 0.0F);
        }
    }

    /**
     * Shared by the client trigger and by the serverbound DodgeCommand, so a modified client cannot skip the
     * raised-guard requirement by sending the command on its own. Dual wielding stands in for the guard, as in
     * MineFantasy Reforged: Backhand lets no sword block while the offhand holds anything.
     */
    public static boolean canDodge(EntityPlayer user) {
        return user != null && (user.isBlocking() || dualWields(user));
    }

    /** A MineFantasy weapon in each hand (Backhand), neither of which wants the other hand free. */
    static boolean dualWields(EntityPlayer user) {
        ItemStack main = user.getHeldItem();
        ItemStack off = Offhand.item(user);
        return main != null && off != null
                && main.getItem() instanceof ItemWeaponMF
                && off.getItem() instanceof ItemWeaponMF
                && ((ItemWeaponMF) main.getItem()).allowOffhand(off)
                && ((ItemWeaponMF) off.getItem()).allowOffhand(main);
    }

    private static final String DODGE_GROUND_NBT = "MF2_DodgeGround";
    private static final String DODGE_JUMP_NBT = "MF2_DodgeJump";
    private static final String DODGE_SPENT_NBT = "MF2_DodgeSpent";
    private static final String DODGE_WANT_DIR_NBT = "MF2_DodgeWantDir";
    private static final String DODGE_WANT_TICK_NBT = "MF2_DodgeWantTick";
    /** How long a request waits for the server to see the player leave the ground. */
    private static final long DODGE_PENDING_TICKS = 3L;
    /** How long after leaving the ground a dodge is still part of that jump. */
    private static final long DODGE_JUMP_TICKS = 10L;
    /** How far under the feet to look for ground, a little more than a position packet rounds away. */
    private static final double GROUND_PROBE = 0.0625D;

    /**
     * Watches for the ground to air transition, which opens the window and releases a request that arrived before the
     * server had seen it. The client's onGround alone cannot be trusted, as NetHandlerPlayServer writes it from every
     * position packet. Standing counts only where the server finds something under the player's feet too, and leaving
     * only where it finds nothing there any more: flipping the flag in mid-air, or in place on the floor, opens
     * nothing.
     */
    public static void trackDodgeWindow(EntityPlayer user) {
        if (user == null) {
            return;
        }
        NBTTagCompound data = user.getEntityData();
        long now = user.worldObj.getTotalWorldTime();
        boolean grounded = standsOnSomething(user);
        if (data.getBoolean(DODGE_GROUND_NBT) && !user.onGround && !grounded) {
            data.setLong(DODGE_JUMP_NBT, now);
            long wanted = data.getLong(DODGE_WANT_TICK_NBT);
            if (isFresh(wanted, now, DODGE_PENDING_TICKS)) {
                data.setLong(DODGE_WANT_TICK_NBT, 0L);
                spendJump(user, data.getInteger(DODGE_WANT_DIR_NBT), now);
            }
        }
        data.setBoolean(DODGE_GROUND_NBT, user.onGround && grounded);

        long wanted = data.getLong(DODGE_WANT_TICK_NBT);
        if (wanted > 0L && !isFresh(wanted, now, DODGE_PENDING_TICKS)) {
            data.setLong(DODGE_WANT_TICK_NBT, 0L);
        }
    }

    /**
     * Whether anything solid is just under the player's feet, as the server's own world has it: a thin slab under the
     * soles only, so a wall beside the player does not count as ground.
     */
    static boolean standsOnSomething(EntityPlayer user) {
        AxisAlignedBB box = user.boundingBox;
        AxisAlignedBB below = AxisAlignedBB
                .getBoundingBox(box.minX, box.minY - GROUND_PROBE, box.minZ, box.maxX, box.minY, box.maxZ);
        return !user.worldObj.getCollidingBoundingBoxes(user, below).isEmpty();
    }

    /**
     * These stamps live in the player's persistent data, so a restored save can hand back a tick from the future. Treat
     * anything that is not inside the window, in either direction, as expired.
     */
    private static boolean isFresh(long stamp, long now, long window) {
        return stamp > 0L && now >= stamp && now - stamp <= window;
    }

    /**
     * Serverbound entry point for DodgeCommand. Dodges only on a jump the server itself observed, once per jump. A
     * request that beats the position packet is held briefly rather than rejected, because the client fires from
     * LivingJumpEvent at the moment of the jump and the server can still believe the player is standing.
     */
    public static void requestDodge(EntityPlayer user, int type) {
        if (!canDodge(user)) {
            return;
        }
        long now = user.worldObj.getTotalWorldTime();
        if (spendJump(user, type, now)) {
            return;
        }
        NBTTagCompound data = user.getEntityData();
        data.setInteger(DODGE_WANT_DIR_NBT, type);
        data.setLong(DODGE_WANT_TICK_NBT, now);
    }

    /** Uses up the current jump, if there is a recent one that has not paid for a dodge yet. */
    private static boolean spendJump(EntityPlayer user, int type, long now) {
        if (!canDodge(user)) {
            return false;
        }
        NBTTagCompound data = user.getEntityData();
        long jump = data.getLong(DODGE_JUMP_NBT);
        if (!isFresh(jump, now, DODGE_JUMP_TICKS) || data.getLong(DODGE_SPENT_NBT) == jump) {
            return false;
        }
        data.setLong(DODGE_SPENT_NBT, jump);
        initDodge(user, type);
        return true;
    }

    private static void tryDodge(EntityPlayer user) {
        if (canDodge(user)) {
            float forward = user.moveForward;
            float side = user.moveStrafing;

            if (side > 0F)// LEFT
            {
                commandDodge(user, 1);
            } else if (side < 0)// RIGHT
            {
                commandDodge(user, -1);
            } else if (forward < 0)// BACK
            {
                commandDodge(user, 0);
            }
        }
    }
}
