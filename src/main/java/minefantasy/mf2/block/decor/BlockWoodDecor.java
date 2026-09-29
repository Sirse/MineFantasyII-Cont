package minefantasy.mf2.block.decor;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.material.CustomMaterial;
import minefantasy.mf2.block.basic.BlockTiled;
import minefantasy.mf2.block.tileentity.InventorySlots;
import minefantasy.mf2.block.tileentity.decor.TileEntityWoodDecor;

/** A wooden block made from a chosen wood, which keeps it when broken and placed again. */
public abstract class BlockWoodDecor<T extends TileEntityWoodDecor> extends BlockTiled<T> {

    private final String texture;

    public BlockWoodDecor(String texture, Class<T> type) {
        super(Material.wood, type);
        this.texture = texture;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase user, ItemStack item) {
        super.onBlockPlacedBy(world, x, y, z, user, item);
        T tile = getTile(world, x, y, z);
        if (tile != null) {
            CustomMaterial material = CustomToolHelper.getCustomPrimaryMaterial(item);
            if (material != null) {
                tile.setMaterial(material);
            }
        }
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        T tile = getTile(world, x, y, z);

        ItemStack itemstack = new ItemStack(getItemDropped(meta, world.rand, 0), 1, damageDropped(meta));

        itemstack = modifyDrop(tile, itemstack);

        if (tile != null) {
            InventorySlots.drop(world, x, y, z, itemstack);

            world.func_147453_f(x, y, z, block);
        }

        super.breakBlock(world, x, y, z, block, meta);
    }

    protected ItemStack modifyDrop(T tile, ItemStack item) {
        if (tile != null && item != null) {
            CustomMaterial.addMaterial(item, CustomToolHelper.slot_main, tile.getMaterialName());
        }
        return item;
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        ArrayList<ItemStack> ret = new ArrayList<ItemStack>();
        return ret;
    }

    public String getFullTexName() {
        return this.texture;
    }

    public ItemStack construct(String name) {
        return construct(name, 1);
    }

    public ItemStack construct(String name, int stacksize) {
        ItemStack item = new ItemStack(this, stacksize);
        CustomMaterial.addMaterial(item, CustomToolHelper.slot_main, name.toLowerCase());

        return item;
    }
}
