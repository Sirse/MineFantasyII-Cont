package minefantasy.mf2.network.packet;

import io.netty.buffer.ByteBuf;
import minefantasy.mf2.mechanics.CombatMechanics;
import minefantasy.mf2.network.NetworkUtils;
import net.minecraft.entity.player.EntityPlayer;

public class DodgeCommand extends PacketMF {
    public static final String packetName = "MF2_Command_Dodge";
    private int ID;

    public DodgeCommand(EntityPlayer user, int id) {
        this.ID = id;
    }

    public DodgeCommand() {
    }

    @Override
    public void process(ByteBuf packet, EntityPlayer player) {
        ID = packet.readInt();
        if (NetworkUtils.isServer(player)) {
            CombatMechanics.initDodge(player, ID);
        }
    }

    @Override
    public String getChannel() {
        return packetName;
    }

    @Override
    public void write(ByteBuf packet) {
        packet.writeInt(ID);
    }
}
