package minefantasy.mf2.network.packet;

import net.minecraft.entity.player.EntityPlayer;

import io.netty.buffer.ByteBuf;
import minefantasy.mf2.api.stamina.StaminaBar;
import minefantasy.mf2.network.NetworkUtils;

public class StaminaPacket extends PacketMF {

    public static final String packetName = "MF2_Staminabar";
    private float[] value;

    public StaminaPacket(float[] value, EntityPlayer user) {
        this.value = value;
    }

    public StaminaPacket() {}

    @Override
    public void process(ByteBuf packet, EntityPlayer player) {
        if (NetworkUtils.isServer(player)) {
            return;
        }

        value = new float[] { StaminaBar.getDefaultMax(player), StaminaBar.getDefaultMax(player), 0, 0F };
        value[0] = packet.readFloat();
        value[1] = packet.readFloat();
        value[2] = packet.readFloat();
        value[3] = packet.readFloat();

        StaminaBar.setStaminaValue(player, value[0]);
        StaminaBar.setMaxStamina(player, value[1]);
        StaminaBar.setFlashTime(player, value[2]);
        StaminaBar.setBonusStamina(player, value[3]);
    }

    @Override
    public String getChannel() {
        return packetName;
    }

    @Override
    public void write(ByteBuf packet) {
        for (float variable : value) {
            packet.writeFloat(variable);
        }
    }
}
