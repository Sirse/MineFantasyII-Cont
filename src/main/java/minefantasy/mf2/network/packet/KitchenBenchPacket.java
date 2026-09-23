package minefantasy.mf2.network.packet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import minefantasy.mf2.block.tileentity.TileEntityKitchenBench;
import minefantasy.mf2.network.NetworkUtils;

/**
 * Recipe details the kitchen bench GUI shows. Progress and dirt already travel through the container, so only what the
 * server's recipe lookup decides is sent here.
 */
public class KitchenBenchPacket extends PacketMF {

    public static final String packetName = "MF2_KitchenBenchPacket";
    private int[] coords = new int[3];
    private String toolNeeded;
    private String research;
    private ItemStack result;

    public KitchenBenchPacket(TileEntityKitchenBench tile) {
        coords = new int[] { tile.xCoord, tile.yCoord, tile.zCoord };
        toolNeeded = tile.getToolNeeded();
        research = tile.getResearchNeeded();
        result = tile.getShownResult();
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
            if (toolNeeded == null) toolNeeded = "";
            if (research == null) research = "";

            TileEntityKitchenBench bench = (TileEntityKitchenBench) entity;
            bench.setToolType(toolNeeded);
            bench.setResearch(research);
            bench.setClientResult(result);
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
    }
}
