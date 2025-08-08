package minefantasy.mf2.network.packet;

import io.netty.buffer.ByteBuf;
import minefantasy.mf2.mechanics.CombatMechanics;
import minefantasy.mf2.network.NetworkUtils;
import net.minecraft.entity.player.EntityPlayer;

public class ParryPacket extends PacketMF {
    public static final String packetName = "MF2_ParryPacket";
    private int value;

    public ParryPacket(int value, EntityPlayer user) {
        this.value = value;
    }

    public ParryPacket() {
    }

    @Override
    public void process(ByteBuf packet, EntityPlayer player) {
        if (NetworkUtils.isServer(player)) {
            return;
        }

        value = packet.readInt();
        CombatMechanics.setParryCooldown(player, value);
    }

    @Override
    public String getChannel() {
        return packetName;
    }

    @Override
    public void write(ByteBuf packet) {
        packet.writeInt(value);
    }
}
