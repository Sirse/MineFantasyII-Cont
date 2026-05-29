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

        int id = in.readInt();
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

        double maxDist = extendedReach + 4.0D;
        Vec3 start = player.getPosition(1.0F);
        Vec3 look = player.getLook(1.0F);
        Vec3 end = start.addVector(look.xCoord * maxDist, look.yCoord * maxDist, look.zCoord * maxDist);
        MovingObjectPosition blockHit = player.worldObj.rayTraceBlocks(start, end, false, true, false);
        float border = target.getCollisionBorderSize();
        MovingObjectPosition hit = target.boundingBox.expand(border, border, border).calculateIntercept(start, end);
        if (hit == null || start.squareDistanceTo(hit.hitVec) > maxDist * maxDist) {
            return false;
        }
        return blockHit == null || start.squareDistanceTo(blockHit.hitVec) > start.squareDistanceTo(hit.hitVec);
    }
}
