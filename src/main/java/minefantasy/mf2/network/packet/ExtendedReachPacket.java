package minefantasy.mf2.network.packet;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import io.netty.buffer.ByteBuf;
import minefantasy.mf2.network.NetworkUtils;
import mods.battlegear2.api.weapons.IExtendedReachWeapon;

public class ExtendedReachPacket extends PacketMF {

    public static final String packetName = "MF2_ExtReach";
    private static final String LAST_ATTACK_TICK_NBT = "MF2_LastExtReachAtk";
    private static final long ATTACK_COOLDOWN_TICKS = 2L;
    private static final double MAX_ALLOWED_REACH_BLOCKS = 10.0D;

    private int targetId;

    public ExtendedReachPacket(int targetId) {
        this.targetId = targetId;
    }

    public ExtendedReachPacket() {}

    @Override
    public String getChannel() {
        return packetName;
    }

    @Override
    public void write(ByteBuf out) {
        out.writeInt(targetId);
    }

    @Override
    public void process(ByteBuf in, EntityPlayer user) {
        if (!NetworkUtils.isServer(user) || !(user instanceof EntityPlayerMP)) {
            return;
        }

        if (!NetworkUtils.hasPayload(in, 4)) {
            return;
        }
        int id = in.readInt();

        // Rate limit first: canReach runs a block ray trace, so an unthrottled client could force one per packet
        long now = user.worldObj.getTotalWorldTime();
        long last = user.getEntityData().getLong(LAST_ATTACK_TICK_NBT);
        if (now - last < ATTACK_COOLDOWN_TICKS) {
            return;
        }
        user.getEntityData().setLong(LAST_ATTACK_TICK_NBT, now);

        Entity target = user.worldObj.getEntityByID(id);
        if (!(target instanceof EntityLivingBase) || !canReach((EntityPlayerMP) user, target)) {
            return;
        }

        user.attackTargetEntityWithCurrentItem(target);
    }

    private boolean canReach(EntityPlayerMP player, Entity target) {
        if (player == null || target == null || target == player || player.worldObj != target.worldObj) {
            return false;
        }
        ItemStack mainhand = player.getCurrentEquippedItem();
        if (mainhand == null || !(mainhand.getItem() instanceof IExtendedReachWeapon)) {
            return false;
        }
        float extendedReach = ((IExtendedReachWeapon) mainhand.getItem()).getReachModifierInBlocks(mainhand);
        if (extendedReach <= 0F) {
            return false;
        }

        double maxDist = Math.min(extendedReach + 4.0D, MAX_ALLOWED_REACH_BLOCKS);
        // EntityPlayer.getPosition is @SideOnly(CLIENT) and is stripped by the SideTransformer on a dedicated
        // server, so build the eye vector explicitly. EntityPlayerMP sets yOffset to 0, so unlike the client
        // player its posY sits at the feet and the full eye height has to be added.
        Vec3 start = Vec3.createVectorHelper(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        Vec3 look = player.getLook(1.0F);
        Vec3 end = start.addVector(look.xCoord * maxDist, look.yCoord * maxDist, look.zCoord * maxDist);
        MovingObjectPosition blockHit = player.worldObj.rayTraceBlocks(start, end, false);
        float border = target.getCollisionBorderSize();
        MovingObjectPosition hit = target.boundingBox.expand(border, border, border).calculateIntercept(start, end);
        if (hit == null || start.squareDistanceTo(hit.hitVec) > maxDist * maxDist) {
            return false;
        }
        return blockHit == null || start.squareDistanceTo(blockHit.hitVec) > start.squareDistanceTo(hit.hitVec);
    }
}
