package minefantasy.mf2.network.packet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;

import io.netty.buffer.ByteBuf;
import minefantasy.mf2.block.decor.BlockRack;
import minefantasy.mf2.block.tileentity.decor.TileEntityRack;
import minefantasy.mf2.mechanics.ProtectionHelper;
import minefantasy.mf2.network.NetworkUtils;

public class RackCommand extends PacketMF {

    public static final String packetName = "MF2_Command_Rack";
    private static final String LAST_RACK_CMD_TICK_NBT = "MF2_LastRackCmd";
    private static final long RACK_COOLDOWN_TICKS = 2L;
    private EntityPlayer user;
    private TileEntityRack rack;
    private int slot;

    public RackCommand(int slot, EntityPlayer user, TileEntityRack rack) {
        this.slot = slot;
        this.rack = rack;
        this.user = user;
    }

    public RackCommand() {}

    @Override
    public void process(ByteBuf packet, EntityPlayer player) {
        if (!NetworkUtils.isServer(player)) {
            return;
        }

        if (!NetworkUtils.hasPayload(packet, 16)) {
            return;
        }
        // Keep everything the packet carries in locals: this instance is shared through packetList
        int x = packet.readInt();
        int y = packet.readInt();
        int z = packet.readInt();
        int wantedSlot = packet.readInt();
        if (wantedSlot < 0 || wantedSlot >= 4 || y < 0 || y >= 256) {
            return;
        }

        // Rate limit before doing any work, so rejected requests cannot be spammed either
        long now = player.worldObj.getTotalWorldTime();
        long last = player.getEntityData().getLong(LAST_RACK_CMD_TICK_NBT);
        if (now - last < RACK_COOLDOWN_TICKS) {
            return;
        }
        player.getEntityData().setLong(LAST_RACK_CMD_TICK_NBT, now);

        // Distance first, then a loaded-chunk test: World.getTileEntity would otherwise load or even generate the
        // chunk for arbitrary coordinates sent by a modified client
        if (!NetworkUtils.isWithinDistanceSq(player, new int[] { x, y, z }, 64)) {
            return;
        }
        if (!player.worldObj.blockExists(x, y, z)) {
            return;
        }

        TileEntity tile = player.worldObj.getTileEntity(x, y, z);
        if (!(tile instanceof TileEntityRack)) {
            return;
        }
        TileEntityRack target = (TileEntityRack) tile;
        if (!target.isUseableByPlayer(player)) {
            return;
        }
        // This path replaces a right-click that never reached the vanilla handler, so raise the same interaction
        // event region protection relies on
        if (!ProtectionHelper.canInteract(player, player.worldObj, x, y, z)) {
            return;
        }
        BlockRack.interact(wantedSlot, player.worldObj, target, player);
    }

    @Override
    public String getChannel() {
        return packetName;
    }

    @Override
    public void write(ByteBuf packet) {
        packet.writeInt(rack.xCoord);
        packet.writeInt(rack.yCoord);
        packet.writeInt(rack.zCoord);
        packet.writeInt(slot);
    }
}
