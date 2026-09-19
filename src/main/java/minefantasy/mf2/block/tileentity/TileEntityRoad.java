package minefantasy.mf2.block.tileentity;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;

import minefantasy.mf2.network.NetworkUtils;

public class TileEntityRoad extends TileEntity {

    public int[] surface = new int[] { 0, 0 };
    public boolean isLocked = false;

    public TileEntityRoad() {}

    /**
     * Vanilla pushes this to every player entering tracking range, so no periodic resync polling is needed.
     */
    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        writeToNBT(nbt);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, nbt);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
        readFromNBT(packet.func_148857_g());
        refreshSurface();
    }

    public void setSurface(Block id, int meta) {
        if (worldObj == null) {
            return;
        }
        if (id == Blocks.grass) {
            id = Blocks.dirt;
        }
        worldObj.playSoundEffect(xCoord, yCoord, zCoord, "dig.grass", 0.5F, 1.0F);
        surface[0] = Block.getIdFromBlock(id);
        surface[1] = meta;
        sendPacketToClients();
        refreshSurface();
    }

    public void sendPacketToClients() {
        if (worldObj.isRemote) return;

        NetworkUtils.sendToWatchers(getDescriptionPacket(), (WorldServer) worldObj, this.xCoord, this.zCoord);
    }

    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        nbt.setIntArray("surface", surface);
        nbt.setBoolean("isLocked", isLocked);
    }

    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        // getIntArray returns an empty array for a missing or wrongly typed tag, and the renderer and
        // getBaseBlock both index surface[0]/surface[1] unchecked
        int[] saved = nbt.getIntArray("surface");
        surface = new int[] { saved.length > 0 ? saved[0] : 0, saved.length > 1 ? saved[1] : 0 };
        isLocked = nbt.getBoolean("isLocked");
    }

    public int[] getSurface() {
        return surface;
    }

    public boolean canBuild() {
        if (worldObj == null) {
            return false;
        }
        return true;
    }

    public void refreshSurface() {
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    public Block getBaseBlock() {
        if (surface[0] <= 0) {
            return Blocks.dirt;
        }
        Block block = Block.getBlockById(surface[0]);
        return block != null ? block : Blocks.dirt;
    }
}
