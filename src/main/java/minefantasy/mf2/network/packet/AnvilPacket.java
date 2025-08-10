package minefantasy.mf2.network.packet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;

import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import minefantasy.mf2.block.tileentity.TileEntityAnvilMF;
import minefantasy.mf2.network.NetworkUtils;

public class AnvilPacket extends PacketMF {

    public static final String packetName = "MF2_AnvilPacket";
    private int[] coords = new int[3];
    private String toolNeeded;
    private String research;
    private float[] floats = new float[6];
    private int[] tiers = new int[2];

    public AnvilPacket(TileEntityAnvilMF tile) {
        coords = new int[] { tile.xCoord, tile.yCoord, tile.zCoord };
        toolNeeded = tile.getToolNeeded();
        research = tile.getResearchNeeded();
        floats = new float[] { tile.progress, tile.progressMax, tile.qualityBalance, tile.thresholdPosition,
                tile.leftHit, tile.rightHit };
        tiers = new int[] { tile.getToolTierNeeded(), tile.getAnvilTierNeeded() };
        if (floats[1] <= 0) {
            floats[1] = 0;
        }
    }

    public AnvilPacket() {}

    @Override
    public void process(ByteBuf packet, EntityPlayer player) {
        // Client-only sync of anvil state
        if (NetworkUtils.isServer(player)) {
            return;
        }

        coords = NetworkUtils.readCoords(packet);
        TileEntity entity = player.worldObj.getTileEntity(coords[0], coords[1], coords[2]);

        if (entity instanceof TileEntityAnvilMF) {
            floats[0] = packet.readFloat();
            floats[1] = packet.readFloat();
            floats[2] = packet.readFloat();
            floats[3] = packet.readFloat();
            floats[4] = packet.readFloat();
            floats[5] = packet.readFloat();
            tiers[0] = packet.readInt();
            tiers[1] = packet.readInt();
            toolNeeded = ByteBufUtils.readUTF8String(packet);
            research = ByteBufUtils.readUTF8String(packet);
            if (toolNeeded == null) toolNeeded = "";
            if (research == null) research = "";

            TileEntityAnvilMF anvil = (TileEntityAnvilMF) entity;
            anvil.setToolType(toolNeeded);
            anvil.progress = floats[0];
            anvil.progressMax = Math.max(0F, floats[1]);
            anvil.qualityBalance = floats[2];
            anvil.thresholdPosition = floats[3];
            anvil.leftHit = floats[4];
            anvil.rightHit = floats[5];
            anvil.setHammerUsed(tiers[0]);
            anvil.setRequiredAnvil(tiers[1]);
            anvil.setResearch(research);
        }
    }

    @Override
    public String getChannel() {
        return packetName;
    }

    @Override
    public void write(ByteBuf packet) {
        NetworkUtils.writeCoords(packet, coords[0], coords[1], coords[2]);
        for (float aFloat : floats) {
            packet.writeFloat(aFloat);
        }
        packet.writeInt(tiers[0]);
        packet.writeInt(tiers[1]);
        ByteBufUtils.writeUTF8String(packet, toolNeeded);
        ByteBufUtils.writeUTF8String(packet, research);
    }
}
