package minefantasy.mf2.network.packet;

import io.netty.buffer.ByteBuf;
import minefantasy.mf2.block.tileentity.TileEntityBombBench;
import minefantasy.mf2.network.NetworkUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;

public class BombBenchPacket extends PacketMF {
    public static final String packetName = "MF2_BombBenchPkt";
    private int[] coords = new int[3];
    private float progress;
    private float maxProgress;
    private boolean hasRecipe;

    public BombBenchPacket(TileEntityBombBench tile) {
        coords = new int[]{tile.xCoord, tile.yCoord, tile.zCoord};
        progress = tile.progress;
        maxProgress = tile.maxProgress;
        hasRecipe = tile.hasRecipe;
    }

    public BombBenchPacket() {
    }

    @Override
    public void process(ByteBuf packet, EntityPlayer player) {
        if (NetworkUtils.isServer(player)) {
            return;
        }
        coords = NetworkUtils.readCoords(packet);
        TileEntity entity = player.worldObj.getTileEntity(coords[0], coords[1], coords[2]);
        float newProgress = packet.readFloat();
        float newMaxProgress = packet.readFloat();
        boolean newRecipe = packet.readBoolean();

        if (entity instanceof TileEntityBombBench) {
            TileEntityBombBench tile = (TileEntityBombBench) entity;
            tile.progress = newProgress;
            tile.maxProgress = newMaxProgress;
            tile.hasRecipe = newRecipe;
        }
    }

    @Override
    public String getChannel() {
        return packetName;
    }

    @Override
    public void write(ByteBuf packet) {
        NetworkUtils.writeCoords(packet, coords[0], coords[1], coords[2]);
        packet.writeFloat(progress);
        packet.writeFloat(maxProgress);
        packet.writeBoolean(hasRecipe);
    }
}
