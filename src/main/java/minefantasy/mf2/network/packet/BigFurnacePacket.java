package minefantasy.mf2.network.packet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;

import io.netty.buffer.ByteBuf;
import minefantasy.mf2.block.tileentity.TileEntityBigFurnace;
import minefantasy.mf2.network.NetworkUtils;

public class BigFurnacePacket extends PacketMF {

    public static final String packetName = "MF2_BigfurnPkt";
    private int[] coords = new int[3];
    private int fuel, progress, burn, doorAngle;

    public BigFurnacePacket(TileEntityBigFurnace tile) {
        coords = new int[] { tile.xCoord, tile.yCoord, tile.zCoord };
        fuel = tile.fuel;
        progress = tile.progress;
        burn = tile.isBurning() ? 1 : 0;
        doorAngle = tile.doorAngle;
    }

    public BigFurnacePacket() {}

    @Override
    public void process(ByteBuf packet, EntityPlayer player) {
        if (NetworkUtils.isServer(player)) {
            return;
        }

        coords = NetworkUtils.readCoords(packet);
        fuel = packet.readInt();
        progress = packet.readInt();
        burn = packet.readInt();
        doorAngle = packet.readInt();

        TileEntity entity = player.worldObj.getTileEntity(coords[0], coords[1], coords[2]);

        if (entity instanceof TileEntityBigFurnace) {
            TileEntityBigFurnace tile = (TileEntityBigFurnace) entity;

            tile.fuel = fuel;
            tile.progress = progress;
            tile.doorAngle = doorAngle;

            if (tile.getWorldObj().isRemote) {
                tile.isBurningClient = (burn == 1);
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
        packet.writeInt(fuel);
        packet.writeInt(progress);
        packet.writeInt(burn);
        packet.writeInt(doorAngle);
    }
}
