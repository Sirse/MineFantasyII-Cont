package minefantasy.mf2.gametest;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * A block that lets the break event pass but refuses to be removed, as some other mods' blocks do: a break of it is
 * never confirmed, so nothing may be paid for it.
 */
public final class StubbornBlock extends Block {

    public static Block block;

    private StubbornBlock() {
        super(Material.ground);
        setBlockName(GameTestMod.MODID + ".stubborn");
        setHardness(0.5F);
    }

    static void register() {
        block = new StubbornBlock();
        GameRegistry.registerBlock(block, "stubborn");
    }

    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
        return false;
    }
}
