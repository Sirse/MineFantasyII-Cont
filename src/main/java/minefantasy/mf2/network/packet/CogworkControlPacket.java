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

        int id = packet.readInt();
        forward = packet.readFloat();
        strafe = packet.readFloat();
        isJumping = packet.readBoolean();
        if (Float.isNaN(forward) || Float.isInfinite(forward) || Float.isNaN(strafe) || Float.isInfinite(strafe)) {
            return;
        }
        forward = Math.max(-1.0F, Math.min(1.0F, forward));
        strafe = Math.max(-1.0F, Math.min(1.0F, strafe));
        long now = player.worldObj.getTotalWorldTime();
        long last = player.getEntityData().getLong(LAST_COGWORK_CTRL_TICK_NBT);
        if (now - last < CONTROL_COOLDOWN_TICKS) {
            return;
        }
        player.getEntityData().setLong(LAST_COGWORK_CTRL_TICK_NBT, now);

        Entity entity = player.worldObj.getEntityByID(id);

        if (entity instanceof EntityCogwork) {
            suit = (EntityCogwork) entity;

            if (!suit.isDead && suit.riddenByEntity == player
                    && player.ridingEntity == suit
                    && suit.worldObj == player.worldObj
                    && player.getDistanceSqToEntity(suit) <= 64D) {
                suit.setMoveForward(forward);
                suit.setMoveStrafe(strafe);
                suit.setJumpControl(isJumping);
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
