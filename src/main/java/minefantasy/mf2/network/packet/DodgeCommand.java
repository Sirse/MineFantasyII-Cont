package minefantasy.mf2.network.packet;

import net.minecraft.entity.player.EntityPlayer;

import io.netty.buffer.ByteBuf;
import minefantasy.mf2.mechanics.CombatMechanics;
import minefantasy.mf2.network.NetworkUtils;

public class DodgeCommand extends PacketMF {

    public static final String packetName = "MF2_Command_Dodge";
    private static final String LAST_DODGE_CMD_TICK_NBT = "MF2_LastDodgeCmd";
    // Pure anti-flood; one impulse per jump is enforced by the armed dodge in CombatMechanics
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
        if (!NetworkUtils.hasPayload(packet, 4)) {
            return;
        }
        // Local, not a field: this handler instance is shared by every player through packetList
        int dodgeId = packet.readInt();
        if (dodgeId != -1 && dodgeId != 0 && dodgeId != 1) {
            return;
        }
        long now = player.worldObj.getTotalWorldTime();
        long last = player.getEntityData().getLong(LAST_DODGE_CMD_TICK_NBT);
        if (now - last < DODGE_COOLDOWN_TICKS) {
            return;
        }
        player.getEntityData().setLong(LAST_DODGE_CMD_TICK_NBT, now);
        CombatMechanics.requestDodge(player, dodgeId);
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
