package minefantasy.mf2.network.packet;

import net.minecraft.entity.player.EntityPlayer;

import io.netty.buffer.ByteBuf;
import minefantasy.mf2.block.tileentity.TileEntityBellows;
import minefantasy.mf2.network.NetworkUtils;

public class BellowsPacket extends PacketMF {

    public static final String packetName = "MF2_BellowsPkt";
    private int[] coords = new int[3];
    private int press;

    public BellowsPacket(TileEntityBellows tile) {
        this.coords = new int[] { tile.xCoord, tile.yCoord, tile.zCoord };
        this.press = tile.press;
    }

    public BellowsPacket() {}

    @Override
    public void process(ByteBuf packet, EntityPlayer player) {
        if (NetworkUtils.isServer(player)) {
            return;
        }

        coords = NetworkUtils.readCoords(packet);
        int newPress = packet.readInt();
        TileEntityBellows tile = NetworkUtils.getTile(player.worldObj, coords, TileEntityBellows.class);
        if (tile == null) {
            return;
        }

        tile.press = Math.max(0, newPress);
    }

    @Override
    public String getChannel() {
        return packetName;
    }

    @Override
    public void write(ByteBuf packet) {
        NetworkUtils.writeCoords(packet, coords[0], coords[1], coords[2]);
        packet.writeInt(press);
    }
}
