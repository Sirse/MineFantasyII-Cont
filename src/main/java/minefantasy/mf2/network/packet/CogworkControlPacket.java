package minefantasy.mf2.network.packet;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import io.netty.buffer.ByteBuf;
import minefantasy.mf2.entity.EntityCogwork;
import minefantasy.mf2.network.NetworkUtils;

public class CogworkControlPacket extends PacketMF {

    public static final String packetName = "MF2_CogworkCtrl";
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
        if (!NetworkUtils.isServer(player)) {
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
        Entity entity = player.worldObj.getEntityByID(id);

        if (entity instanceof EntityCogwork) {
            suit = (EntityCogwork) entity;

            if (!suit.isDead && suit.riddenByEntity == player
                    && player.ridingEntity == suit
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
