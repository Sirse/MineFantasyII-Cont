package minefantasy.mf2.network.packet;

import net.minecraft.entity.player.EntityPlayer;

import io.netty.buffer.ByteBuf;
import minefantasy.mf2.block.tileentity.TileEntityBombPress;
import minefantasy.mf2.network.NetworkUtils;

public class BombPressPacket extends PacketMF {

    public static final String packetName = "MF2_BombPressPkt";
    private int[] coords = new int[3];
    private float animation;

    public BombPressPacket(TileEntityBombPress tile) {
        this.coords = new int[] { tile.xCoord, tile.yCoord, tile.zCoord };
        this.animation = tile.animation;
    }

    public BombPressPacket() {}

    @Override
    public void process(ByteBuf packet, EntityPlayer player) {
        if (NetworkUtils.isServer(player)) {
            return;
        }

        coords = NetworkUtils.readCoords(packet);
        float newAnimation = packet.readFloat();
        TileEntityBombPress tile = NetworkUtils.getTile(player.worldObj, coords, TileEntityBombPress.class);
        if (tile == null) {
            return;
        }

        tile.animation = Math.max(0F, newAnimation);
    }

    @Override
    public String getChannel() {
        return packetName;
    }

    @Override
    public void write(ByteBuf packet) {
        NetworkUtils.writeCoords(packet, coords[0], coords[1], coords[2]);
        packet.writeFloat(animation);
    }
}
