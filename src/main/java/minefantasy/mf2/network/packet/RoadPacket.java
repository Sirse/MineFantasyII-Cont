package minefantasy.mf2.network.packet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;

import io.netty.buffer.ByteBuf;
import minefantasy.mf2.block.tileentity.TileEntityRoad;
import minefantasy.mf2.network.NetworkUtils;

public class RoadPacket extends PacketMF {

    public static final String packetName = "MF2_RoadPacket";
    private int[] coords = new int[3];
    private int[] surface;
    private boolean isLocked;
    private boolean isRequest = false;

    public RoadPacket(TileEntityRoad tile) {
        this.coords = new int[] { tile.xCoord, tile.yCoord, tile.zCoord };
        this.surface = tile.surface;
        this.isLocked = tile.isLocked;
    }

    public RoadPacket() {}

    public RoadPacket request() {
        isRequest = true;
        return this;
    }

    @Override
    public void process(ByteBuf packet, EntityPlayer player) {
        this.coords = NetworkUtils.readCoords(packet);
        isRequest = packet.readBoolean();

        TileEntity entity = player.worldObj.getTileEntity(coords[0], coords[1], coords[2]);
        if (!(entity instanceof TileEntityRoad)) {
            return;
        }
        TileEntityRoad tile = (TileEntityRoad) entity;

        if (isRequest) {
            if (NetworkUtils.isServer(player)) {
                tile.sendPacketToClients();
            }
        } else {
            if (!NetworkUtils.isServer(player)) {
                int s0 = packet.readInt();
                int s1 = packet.readInt();
                this.isLocked = packet.readBoolean();
                tile.surface = new int[] { s0, s1 };
                tile.isLocked = this.isLocked;
                tile.refreshSurface();
            }
        }
    }

    @Override
    public String getChannel() {
        return packetName;
    }

    @Override
    public void write(ByteBuf packet) {
        NetworkUtils.writeCoords(packet, coords[0], coords[1], coords[2]);
        packet.writeBoolean(isRequest);

        if (!isRequest) {
            packet.writeInt(surface[0]);
            packet.writeInt(surface[1]);
            packet.writeBoolean(this.isLocked);
        }
    }
}
