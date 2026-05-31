package minefantasy.mf2.network.packet;

import net.minecraft.entity.player.EntityPlayer;

import io.netty.buffer.ByteBuf;
import minefantasy.mf2.block.tileentity.TileEntityQuern;
import minefantasy.mf2.network.NetworkUtils;

public class QuernPacket extends PacketMF {

    public static final String packetName = "MF2_QuernPkt";
    private int[] coords = new int[3];
    private int turnAngle;
    private int postUseTicks;

    public QuernPacket(TileEntityQuern tile) {
        this.coords = new int[] { tile.xCoord, tile.yCoord, tile.zCoord };
        this.turnAngle = tile.turnAngle;
        this.postUseTicks = tile.getPostUseTicks();
    }

    public QuernPacket() {}

    @Override
    public void process(ByteBuf packet, EntityPlayer player) {
        if (NetworkUtils.isServer(player)) {
            return;
        }

        coords = NetworkUtils.readCoords(packet);
        int newTurnAngle = packet.readInt();
        int newPostUseTicks = packet.readInt();
        TileEntityQuern tile = NetworkUtils.getTile(player.worldObj, coords, TileEntityQuern.class);
        if (tile == null) {
            return;
        }

        tile.turnAngle = Math.max(0, newTurnAngle);
        tile.setPostUseTicks(Math.max(0, newPostUseTicks));
    }

    @Override
    public String getChannel() {
        return packetName;
    }

    @Override
    public void write(ByteBuf packet) {
        NetworkUtils.writeCoords(packet, coords[0], coords[1], coords[2]);
        packet.writeInt(turnAngle);
        packet.writeInt(postUseTicks);
    }
}
