package minefantasy.mf2.network.packet;

import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import minefantasy.mf2.block.tileentity.TileEntityCarpenterMF;
import minefantasy.mf2.network.NetworkUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;

public class CarpenterPacket extends PacketMF {
    public static final String packetName = "MF2_CarpenterPacket";
    private int[] coords = new int[3];
    private String resultName;
    private String toolNeeded;
    private float[] progress = new float[2];
    private int[] tiers = new int[2];
    private String research;

    public CarpenterPacket(TileEntityCarpenterMF tile) {
        coords = new int[]{tile.xCoord, tile.yCoord, tile.zCoord};
        resultName = tile.getResultName();
        toolNeeded = tile.getToolNeeded();
        progress = new float[]{tile.progress, tile.progressMax};
        tiers = new int[]{tile.getToolTierNeeded(), tile.getCarpenterTierNeeded()};
        if (progress[1] <= 0) {
            progress[1] = 0;
        }
        research = tile.getResearchNeeded();
    }

    public CarpenterPacket() {
    }

    @Override
    public void process(ByteBuf packet, EntityPlayer player) {
        if (NetworkUtils.isServer(player)) {
            return;
        }

        coords = NetworkUtils.readCoords(packet);
        TileEntity entity = player.worldObj.getTileEntity(coords[0], coords[1], coords[2]);

        if (entity instanceof TileEntityCarpenterMF) {
            progress[0] = packet.readFloat();
            progress[1] = packet.readFloat();
            tiers[0] = packet.readInt();
            tiers[1] = packet.readInt();
            resultName = ByteBufUtils.readUTF8String(packet);
            toolNeeded = ByteBufUtils.readUTF8String(packet);
            research = ByteBufUtils.readUTF8String(packet);

            TileEntityCarpenterMF carpenter = (TileEntityCarpenterMF) entity;
            carpenter.resName = resultName;
            carpenter.setToolType(toolNeeded);
            carpenter.setResearch(research);
            carpenter.progress = progress[0];
            carpenter.progressMax = progress[1];
            carpenter.setToolTier(tiers[0]);
            carpenter.setRequiredCarpenter(tiers[1]);
        }
    }

    @Override
    public String getChannel() {
        return packetName;
    }

    @Override
    public void write(ByteBuf packet) {
        NetworkUtils.writeCoords(packet, coords[0], coords[1], coords[2]);
        packet.writeFloat(progress[0]);
        packet.writeFloat(progress[1]);
        packet.writeInt(tiers[0]);
        packet.writeInt(tiers[1]);
        ByteBufUtils.writeUTF8String(packet, resultName);
        ByteBufUtils.writeUTF8String(packet, toolNeeded);
        ByteBufUtils.writeUTF8String(packet, research);
    }
}
