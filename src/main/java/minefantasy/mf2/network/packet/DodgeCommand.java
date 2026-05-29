package minefantasy.mf2.network.packet;

import net.minecraft.entity.player.EntityPlayer;

import io.netty.buffer.ByteBuf;
import minefantasy.mf2.mechanics.CombatMechanics;
import minefantasy.mf2.network.NetworkUtils;

public class DodgeCommand extends PacketMF {

    public static final String packetName = "MF2_Command_Dodge";
    private static final String LAST_DODGE_CMD_TICK_NBT = "MF2_LastDodgeCmd";
    private static final long DODGE_COOLDOWN_TICKS = 2L;
    private int ID;

    public DodgeCommand(EntityPlayer user, int id) {
        this.ID = id;
    }

    public DodgeCommand() {}

    @Override
    public void process(ByteBuf packet, EntityPlayer player) {
        if (!NetworkUtils.isServer(player)) {
            return;
        }
        ID = packet.readInt();
        if (ID != -1 && ID != 0 && ID != 1) {
            return;
        }
        long now = player.worldObj.getTotalWorldTime();
        long last = player.getEntityData().getLong(LAST_DODGE_CMD_TICK_NBT);
        if (now - last < DODGE_COOLDOWN_TICKS) {
            return;
        }
        player.getEntityData().setLong(LAST_DODGE_CMD_TICK_NBT, now);
        CombatMechanics.initDodge(player, ID);
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
