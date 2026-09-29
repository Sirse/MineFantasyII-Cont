package minefantasy.mf2.block.tileentity;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import minefantasy.mf2.api.helpers.Tiles;
import minefantasy.mf2.api.refine.ISmokeCarrier;
import minefantasy.mf2.api.refine.SmokeMechanics;
import minefantasy.mf2.block.refining.BlockChimney;

public class TileEntityChimney extends TileEntity implements ISmokeCarrier {

    public int lastSharedInt = 0;
    protected int smokeStorage;
    private int isIndirect = -1;
    /**
     * Name of the player who placed this block; empty when unknown (pre-existing blocks)
     */
    private String ownerName = "";
    private Random rand = new Random();

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (lastSharedInt > 0) --lastSharedInt;

        if (smokeStorage > 0) {
            if (!isPipeChimney()) {
                SmokeMechanics.emitSmokeFromCarrier(worldObj, xCoord, yCoord, zCoord, this, 5);
            } else if (tryShareSmoke()) {
                lastSharedInt = 5;
            }
        }
        if (!worldObj.isRemote && smokeStorage > getMaxSmokeStorage() && rand.nextInt(500) == 0) {
            // Attribute the blast to the block's owner, so protection plugins evaluate the right permissions
            worldObj.newExplosion(getExplosionCause(), xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D, 2F, false, true);
        }
    }

    /**
     * Records the builder, so the smoke-overload blast is attributed to the owner rather than to whoever happens to
     * stand nearby and protection plugins evaluate the right permissions. Called from the block on placement.
     */
    public void setOwner(EntityPlayer player) {
        ownerName = player == null ? "" : player.getCommandSenderName();
    }

    private EntityPlayer getExplosionCause() {
        if (ownerName == null || ownerName.isEmpty()) {
            return null;
        }
        return worldObj.getPlayerEntityByName(ownerName);
    }

    public boolean isPipeChimney() {
        if (worldObj != null) {
            Block block = worldObj.getBlock(xCoord, yCoord, zCoord);
            return block instanceof BlockChimney && ((BlockChimney) block).isPipe();
        }
        return false;
    }

    private boolean tryShareSmoke() {
        if (!isPipeChimney()) return false;
        if (tryPassTo(0, 1, 0, true, false)) {
            return true;// Up First
        }
        if (tryPassTo(-1, 0, 0, false, true) || tryPassTo(1, 0, 0, false, true)
                || tryPassTo(0, 0, -1, false, true)
                || tryPassTo(0, 0, 1, false, true)) {
            return true;// Sides
        }
        if (this.getMaxSmokeStorage() - this.getSmokeValue() <= 5) {
            return tryPassTo(0, -1, 0, false, false);// Down last only when nearly full
        }
        return false;
    }

    private boolean tryPassTo(int x, int y, int z, boolean priority, boolean sideways) {
        TileEntityChimney carrier = Tiles.get(worldObj, x + xCoord, y + yCoord, z + zCoord, TileEntityChimney.class);
        if (carrier != null) {
            boolean canPass = !sideways || carrier.canAcceptSideways();

            int smoke = carrier.getSmokeValue();
            int max = carrier.getMaxSmokeStorage();
            int room_left = max - smoke;
            int limit = priority ? max : this.getSmokeValue();// When going up, it fills all, sideways follows
            // concentration gradient
            if (canPass && carrier.lastSharedInt <= 0 && room_left > 0 && smoke < limit) {
                int pass = Math.min(room_left, 5);
                carrier.setSmokeValue(smoke + pass);
                this.smokeStorage -= pass;
                return true;
            }
        }

        return false;
    }

    private boolean canAcceptSideways() {
        BlockChimney block = this.getActiveBlock();
        if (block != null) {
            return block.isWideChimney() || block.isPipe();
        }
        return false;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        nbt.setString("Owner", ownerName == null ? "" : ownerName);
        nbt.setInteger("StoredSmoke", smokeStorage);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        ownerName = nbt.getString("Owner");
        smokeStorage = nbt.getInteger("StoredSmoke");
    }

    @Override
    public int getSmokeValue() {
        return smokeStorage;
    }

    @Override
    public void setSmokeValue(int smoke) {
        smokeStorage = smoke;
    }

    @Override
    public int getMaxSmokeStorage() {
        if (this.blockType instanceof BlockChimney) {
            return ((BlockChimney) blockType).size;
        }
        return 5;
    }

    public BlockChimney getActiveBlock() {
        if (worldObj == null) return null;

        Block block = worldObj.getBlock(xCoord, yCoord, zCoord);

        if (block instanceof BlockChimney) {
            return (BlockChimney) block;
        }
        return null;
    }

    public boolean isIndirect() {
        BlockChimney block = getActiveBlock();
        return block != null && block.isIndirect;
    }

    @Override
    public boolean canAbsorbIndirect() {
        if (isIndirect == -1) {
            isIndirect = isIndirect() ? 1 : 0;
        }
        return isIndirect == 1;
    }

    public boolean canAccept(int x, int y, int z) {
        Block block = worldObj.getBlock(x + xCoord, y + yCoord, z + zCoord);
        if (block instanceof BlockChimney) {
            if (x == 0 && z == 0)// Not sideways
            {
                return true;
            } else {
                return ((BlockChimney) block).isPipe() || ((BlockChimney) block).isWideChimney();
            }
        }
        return false;
    }

    public boolean canAccept(ForgeDirection fd) {
        return canAccept(fd.offsetX, fd.offsetY, fd.offsetZ);
    }
}
