package minefantasy.mf2.block.tileentity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

public class TileEntityBombPress extends TileEntityShown {

    public float prevAnimation = 0F;
    public float animation = 0F;

    @Override
    public void updateEntity() {
        super.updateEntity();
        prevAnimation = animation;
        if (worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord)) {
            if (animation <= 0) {
                use(null);
            }
            animation = 1.0F;
        } else {
            if (animation > 0) {
                animation -= 0.05F;
            }
        }
    }

    public void use(EntityPlayer user) {
        TileEntity under = worldObj.getTileEntity(xCoord, yCoord - 1, zCoord);
        if (animation <= 0 && under != null && under instanceof TileEntityBombBench) {
            ((TileEntityBombBench) under).tryCraft(user, true);
            animation = 1.0F;
            worldObj.playSoundEffect(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, "tile.piston.out", 1.0F, 0.75F);
            syncAnimation();
        }
    }

    /** The press starts: it goes out once, then runs down on its own on every side. */
    private void syncAnimation() {
        NBTTagCompound moment = new NBTTagCompound();
        moment.setFloat("Press", animation);
        sendMoment(moment);
    }

    @Override
    public void show(NBTTagCompound state) {
        animation = Math.max(0F, state.getFloat("Press"));
    }
}
