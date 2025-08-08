package minefantasy.mf2.network.packet;

import io.netty.buffer.ByteBuf;
import minefantasy.mf2.block.tileentity.TileEntityTanningRack;
import minefantasy.mf2.network.NetworkUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;

public class TannerPacket extends PacketMF {
    public static final String packetName = "MF2_TannerPacket";
    private int[] coords = new int[3];
    private float animation;

    public TannerPacket(TileEntityTanningRack tile) {
        coords = new int[]{tile.xCoord, tile.yCoord, tile.zCoord};
        animation = tile.acTime;
    }

    public TannerPacket() {
    }

    @Override
    public void process(ByteBuf packet, EntityPlayer player) {
        if (NetworkUtils.isServer(player)) {
            return;
        }
        coords = NetworkUtils.readCoords(packet);
        TileEntity entity = player.worldObj.getTileEntity(coords[0], coords[1], coords[2]);
        float newAnim = packet.readFloat();

        if (entity instanceof TileEntityTanningRack) {
            TileEntityTanningRack tile = (TileEntityTanningRack) entity;
            tile.acTime = newAnim;
        }
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
