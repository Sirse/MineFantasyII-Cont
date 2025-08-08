package minefantasy.mf2.network.packet;

import io.netty.buffer.ByteBuf;
import minefantasy.mf2.block.decor.BlockRack;
import minefantasy.mf2.block.tileentity.decor.TileEntityRack;
import minefantasy.mf2.network.NetworkUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;

public class RackCommand extends PacketMF {
    public static final String packetName = "MF2_Command_Rack";
    private EntityPlayer user;
    private TileEntityRack rack;
    private int slot;

    public RackCommand(int slot, EntityPlayer user, TileEntityRack rack) {
        this.slot = slot;
        this.rack = rack;
        this.user = user;
    }

    public RackCommand() {
    }

    @Override
    public void process(ByteBuf packet, EntityPlayer player) {
        if (!NetworkUtils.isServer(player)) {
            return;
        }

        int x = packet.readInt();
        int y = packet.readInt();
        int z = packet.readInt();
        slot = packet.readInt();

        TileEntity tile = player.worldObj.getTileEntity(x, y, z);
        if (!(tile instanceof TileEntityRack)) {
            return;
        }

        if (!NetworkUtils.isWithinDistanceSq(player, new int[]{x, y, z}, 64)) {
            return;
        }
        rack = (TileEntityRack) tile;
        BlockRack.interact(slot, player.worldObj, rack, player);
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
