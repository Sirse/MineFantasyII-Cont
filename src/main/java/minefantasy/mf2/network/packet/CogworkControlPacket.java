package minefantasy.mf2.network.packet;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import io.netty.buffer.ByteBuf;
import minefantasy.mf2.entity.EntityCogwork;
import minefantasy.mf2.network.NetworkUtils;

public class CogworkControlPacket extends PacketMF {

    public static final String packetName = "MF2_CogworkCtrl";
    private static final String LAST_COGWORK_CTRL_TICK_NBT = "MF2_LastCogworkCtrl";
    private static final long CONTROL_COOLDOWN_TICKS = 1L;
    private EntityCogwork suit;
    private float forward, strafe;
    private boolean isJumping;

    public CogworkControlPacket(EntityCogwork suit) {
        this.suit = suit;
        this.forward = suit.getMoveForward();
        this.strafe = suit.getMoveStrafe();
        this.isJumping = suit.getJumpControl();
    }

    public CogworkControlPacket() {}

    @Override
    public void process(ByteBuf packet, EntityPlayer player) {
        if (!NetworkUtils.isServer(player) || !(player instanceof EntityPlayerMP)) {
            return;
        }

        // Locals, not fields: this handler instance is shared by every player through packetList
        int id = packet.readInt();
        float moveForward = packet.readFloat();
        float moveStrafe = packet.readFloat();
        boolean jumping = packet.readBoolean();
        if (Float.isNaN(moveForward) || Float.isInfinite(moveForward)
                || Float.isNaN(moveStrafe)
                || Float.isInfinite(moveStrafe)) {
            return;
        }
        moveForward = Math.max(-1.0F, Math.min(1.0F, moveForward));
        moveStrafe = Math.max(-1.0F, Math.min(1.0F, moveStrafe));
        long now = player.worldObj.getTotalWorldTime();
        long last = player.getEntityData().getLong(LAST_COGWORK_CTRL_TICK_NBT);
        if (now - last < CONTROL_COOLDOWN_TICKS) {
            return;
        }
        player.getEntityData().setLong(LAST_COGWORK_CTRL_TICK_NBT, now);

        Entity entity = player.worldObj.getEntityByID(id);

        if (entity instanceof EntityCogwork) {
            EntityCogwork target = (EntityCogwork) entity;

            if (!target.isDead && target.riddenByEntity == player
                    && player.ridingEntity == target
                    && target.worldObj == player.worldObj
                    && player.getDistanceSqToEntity(target) <= 64D) {
                target.setMoveForward(moveForward);
                target.setMoveStrafe(moveStrafe);
                target.setJumpControl(jumping);
            }
        }
    }

    @Override
    public String getChannel() {
        return packetName;
    }

    @Override
    public void write(ByteBuf packet) {
        packet.writeInt(suit.getEntityId());
        packet.writeFloat(forward);
        packet.writeFloat(strafe);
        packet.writeBoolean(isJumping);
    }
}
