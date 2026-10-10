package minefantasy.mf2.integration.waila;

import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaDataProvider;
import mcp.mobius.waila.api.IWailaRegistrar;
import minefantasy.mf2.block.food.BlockBerryBush;
import minefantasy.mf2.block.tileentity.TileEntityAnvilMF;
import minefantasy.mf2.block.tileentity.TileEntityBigFurnace;
import minefantasy.mf2.block.tileentity.TileEntityBloomery;
import minefantasy.mf2.block.tileentity.TileEntityCarpenterMF;
import minefantasy.mf2.block.tileentity.TileEntityComponent;
import minefantasy.mf2.block.tileentity.TileEntityCrucible;
import minefantasy.mf2.block.tileentity.TileEntityFirepit;
import minefantasy.mf2.block.tileentity.TileEntityForge;
import minefantasy.mf2.block.tileentity.TileEntityKitchenBench;
import minefantasy.mf2.block.tileentity.TileEntityQuern;
import minefantasy.mf2.block.tileentity.TileEntityResearch;
import minefantasy.mf2.block.tileentity.TileEntityRoad;
import minefantasy.mf2.block.tileentity.TileEntityRoast;
import minefantasy.mf2.block.tileentity.TileEntityTanningRack;
import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFC;
import minefantasy.mf2.block.tileentity.decor.TileEntityAmmoBox;
import minefantasy.mf2.block.tileentity.decor.TileEntityRack;
import minefantasy.mf2.block.tileentity.decor.TileEntityTrough;

/** Loaded only by Waila's IMC callback, on both sides so the server can supply the targeted snapshot. */
public final class WailaProvider implements IWailaDataProvider {

    public static void register(IWailaRegistrar registrar) {
        for (String category : new String[] { "craft", "heat", "fluids", "storage", "research", "world" }) {
            String key = "minefantasy2.waila." + category;
            registrar.addConfig("MineFantasy II", key, key);
        }
        WailaProvider provider = new WailaProvider();
        registrar.registerBodyProvider(provider, BlockBerryBush.class);
        for (Class<?> tile : new Class<?>[] { TileEntityAnvilMF.class, TileEntityCarpenterMF.class,
                TileEntityKitchenBench.class, TileEntityCrucible.class, TileEntityForge.class, TileEntityFirepit.class,
                TileEntityTrough.class, TileEntityBigFurnace.class, TileEntityBloomery.class, TileEntityQuern.class,
                TileEntityRoast.class, TileEntityTanningRack.class, TileEntityBlastFC.class, TileEntityComponent.class,
                TileEntityAmmoBox.class, TileEntityRack.class, TileEntityResearch.class, TileEntityRoad.class }) {
            registrar.registerBodyProvider(provider, tile);
            registrar.registerNBTProvider(provider, tile);
        }
    }

    @Override
    public NBTTagCompound getNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world, int x,
            int y, int z) {
        tag.removeTag(WailaData.KEY);
        if (player != null && tile != null
                && world != null
                && !world.isRemote
                && player.worldObj == world
                && tile.getWorldObj() == world
                && tile.xCoord == x
                && tile.yCoord == y
                && tile.zCoord == z
                && player.getDistanceSq(x + 0.5D, y + 0.5D, z + 0.5D) <= 64D
                && world.blockExists(x, y, z)) {
            tag.setTag(WailaData.KEY, WailaData.describe(tile, player));
        }
        return tag;
    }

    @Override
    public List<String> getWailaBody(ItemStack stack, List<String> tip, IWailaDataAccessor accessor,
            IWailaConfigHandler config) {
        NBTTagCompound data = accessor.getNBTData().getCompoundTag(WailaData.KEY);
        if (accessor.getBlock() instanceof BlockBerryBush) {
            data = new NBTTagCompound();
            data.setString("Kind", "world");
            data.setBoolean("BerryReady", accessor.getMetadata() == 0);
        }
        if (enabled(data, config)) WailaTooltip.body(data, tip);
        return tip;
    }

    @Override
    public boolean hasWailaAdvancedBody(ItemStack stack, IWailaDataAccessor accessor, IWailaConfigHandler config) {
        NBTTagCompound data = accessor.getNBTData().getCompoundTag(WailaData.KEY);
        return enabled(data, config) && data.hasKey("Tool") && !data.getString("Tool").isEmpty();
    }

    @Override
    public List<String> getWailaAdvancedBody(ItemStack stack, List<String> tip, IWailaDataAccessor accessor,
            IWailaConfigHandler config) {
        NBTTagCompound data = accessor.getNBTData().getCompoundTag(WailaData.KEY);
        if (enabled(data, config)) WailaTooltip.details(data, tip);
        return tip;
    }

    private static boolean enabled(NBTTagCompound data, IWailaConfigHandler config) {
        String kind = data.getString("Kind");
        return !kind.isEmpty() && config.getConfig("minefantasy2.waila." + kind, true);
    }

    @Override
    public ItemStack getWailaStack(IWailaDataAccessor accessor, IWailaConfigHandler config) {
        return null;
    }

    @Override
    public List<String> getWailaHead(ItemStack stack, List<String> tip, IWailaDataAccessor accessor,
            IWailaConfigHandler config) {
        return tip;
    }

    @Override
    public List<String> getWailaTail(ItemStack stack, List<String> tip, IWailaDataAccessor accessor,
            IWailaConfigHandler config) {
        return tip;
    }
}
