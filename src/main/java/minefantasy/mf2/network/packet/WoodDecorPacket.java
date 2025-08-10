package minefantasy.mf2.network.packet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;

import io.netty.buffer.ByteBuf;
import minefantasy.mf2.block.tileentity.decor.TileEntityWoodDecor;
import minefantasy.mf2.network.NetworkUtils;

public class WoodDecorPacket extends PacketMF {

    public static final String packetName = "MF2_WdDecorPkt";
    private int[] coords = new int[3];
    private String materialName;

    public WoodDecorPacket(TileEntityWoodDecor tile) {
        this.coords = new int[] { tile.xCoord, tile.yCoord, tile.zCoord };
        this.materialName = tile.getMaterialName();
    }

    public WoodDecorPacket() {}

    @Override
    public void process(ByteBuf packet, EntityPlayer player) {
        if (NetworkUtils.isServer(player)) {
            return;
        }

        coords = NetworkUtils.readCoords(packet);
        TileEntity entity = player.worldObj.getTileEntity(coords[0], coords[1], coords[2]);
        materialName = cpw.mods.fml.common.network.ByteBufUtils.readUTF8String(packet);

        if (entity instanceof TileEntityWoodDecor) {
            TileEntityWoodDecor tile = (TileEntityWoodDecor) entity;
            tile.trySetMaterial(materialName);
        }
    }

    @Override
    public String getChannel() {
        return packetName;
    }

    @Override
    public void write(ByteBuf packet) {
        NetworkUtils.writeCoords(packet, coords[0], coords[1], coords[2]);
        cpw.mods.fml.common.network.ByteBufUtils.writeUTF8String(packet, materialName);
    }
}
