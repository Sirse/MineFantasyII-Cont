package minefantasy.mf2.network.packet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import minefantasy.mf2.block.tileentity.TileEntityKitchenBench;
import minefantasy.mf2.network.NetworkUtils;

/**
 * Kitchen bench state for the GUI and the in-world HUD. The container also tracks progress and dirt, but only while the
 * GUI is open, and the HUD needs them without it.
 */
public class KitchenBenchPacket extends PacketMF {

    public static final String packetName = "MF2_KitchenBenchPacket";
    private int[] coords = new int[3];
    private String toolNeeded;
    private String research;
    private ItemStack result;
    private float[] floats = new float[4];

    public KitchenBenchPacket(TileEntityKitchenBench tile) {
        coords = new int[] { tile.xCoord, tile.yCoord, tile.zCoord };
        toolNeeded = tile.getToolNeeded();
        research = tile.getResearchNeeded();
        result = tile.getShownResult();
        floats = new float[] { tile.progress, tile.progressMax, tile.dirtyProgress, tile.getDirtyMax() };
    }

    public KitchenBenchPacket() {}

    @Override
    public void process(ByteBuf packet, EntityPlayer player) {
        if (NetworkUtils.isServer(player)) {
            return;
        }

        coords = NetworkUtils.readCoords(packet);
        TileEntity entity = player.worldObj.getTileEntity(coords[0], coords[1], coords[2]);

        if (entity instanceof TileEntityKitchenBench) {
            toolNeeded = ByteBufUtils.readUTF8String(packet);
            research = ByteBufUtils.readUTF8String(packet);
            result = ByteBufUtils.readItemStack(packet);
            for (int i = 0; i < floats.length; i++) {
                floats[i] = packet.readFloat();
            }
            if (toolNeeded == null) toolNeeded = "";
            if (research == null) research = "";

            TileEntityKitchenBench bench = (TileEntityKitchenBench) entity;
            bench.setToolType(toolNeeded);
            bench.setResearch(research);
            bench.setClientResult(result);
            bench.progress = floats[0];
            bench.progressMax = Math.max(0F, floats[1]);
            bench.dirtyProgress = floats[2];
            bench.setDirtyMax(floats[3]);
        }
    }

    @Override
    public String getChannel() {
        return packetName;
    }

    @Override
    public void write(ByteBuf packet) {
        NetworkUtils.writeCoords(packet, coords[0], coords[1], coords[2]);
        ByteBufUtils.writeUTF8String(packet, toolNeeded == null ? "" : toolNeeded);
        ByteBufUtils.writeUTF8String(packet, research == null ? "" : research);
        ByteBufUtils.writeItemStack(packet, result);
        for (float value : floats) {
            packet.writeFloat(value);
        }
    }
}
