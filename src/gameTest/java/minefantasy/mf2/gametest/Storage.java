package minefantasy.mf2.gametest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.world.WorldServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.FMLCommonHandler;
import minefantasy.mf2.block.decor.BlockComponent;
import minefantasy.mf2.block.decor.IPiledComponent;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.tileentity.TileEntityComponent;

/**
 * West of the spawn: a full pile of every component that can be set down in the world, then every MineFantasy item in
 * chests, a run of chests per creative tab. Both read the registry, so new items show without anyone listing them.
 * Blocks are left to the {@link BlockCatalog}.
 */
final class Storage {

    private static final Logger LOG = LogManager.getLogger("MF2 showcase");
    private static final String DOMAIN = "minefantasy2:";
    private static final String NO_TAB = "No tab", ALL = "All";
    /** Things in one column, the room each takes along it, and the room a column takes. */
    private static final int PER_COLUMN = 16, STEP = 2, COLUMN_WIDTH = 3;
    /** A sign facing east, toward the player walking up from the spawn. */
    private static final int FACING_EAST = 12;
    private static final int CHEST_SIZE = 27;
    /** How far the district reaches south of its corner, sign row included. */
    static final int DEPTH = PER_COLUMN * STEP + 1;

    private Storage() {}

    /** Clears the district west of {@code (x, z)} and builds it anew; returns how many items it holds. */
    static int build(WorldServer world, int x, int z) {
        Map<String, List<ItemStack>> tabs = itemsByTab();
        List<ItemStack> piles = new ArrayList<>();
        List<String> pileNames = new ArrayList<>();
        List<ItemStack[]> chests = new ArrayList<>();
        List<String> chestNames = new ArrayList<>();
        int items = 0;
        for (Map.Entry<String, List<ItemStack>> tab : tabs.entrySet()) {
            List<ItemStack> stacks = tab.getValue();
            for (ItemStack stack : stacks) {
                if (pileType(stack) != null) {
                    piles.add(stack);
                    pileNames.add(stack.getUnlocalizedName().replaceFirst("^(item|tile)\\.", ""));
                }
            }
            for (int from = 0, part = 1; from < stacks.size(); from += CHEST_SIZE, part++) {
                List<ItemStack> load = stacks.subList(from, Math.min(stacks.size(), from + CHEST_SIZE));
                chests.add(load.toArray(new ItemStack[0]));
                chestNames.add(tab.getKey() + "/" + part + " of " + ((stacks.size() + CHEST_SIZE - 1) / CHEST_SIZE));
            }
            items += stacks.size();
        }

        int columns = columns(piles.size()) + columns(chests.size());
        int y = ShowcaseWorld.GROUND;
        Showcase.clear(world, x - columns * COLUMN_WIDTH, z - 1, x + 1, z + DEPTH - 1);

        int slot = 0;
        for (int i = 0; i < piles.size(); i++, slot++) {
            int[] at = at(x, z, slot);
            pile(world, at[0], y, at[1], piles.get(i));
            Showcase.sign(world, at[0] + 1, y, at[1], FACING_EAST, pileNames.get(i));
        }
        slot = columns(piles.size()) * PER_COLUMN;
        for (int i = 0; i < chests.size(); i++, slot++) {
            int[] at = at(x, z, slot);
            chest(world, at[0], y, at[1], chests.get(i));
            Showcase.sign(world, at[0] + 1, y, at[1], FACING_EAST, chestNames.get(i));
        }
        return items;
    }

    private static int columns(int things) {
        return (things + PER_COLUMN - 1) / PER_COLUMN;
    }

    /** Where the {@code slot}th thing stands: columns run south, one after another westward. */
    private static int[] at(int x, int z, int slot) {
        return new int[] { x - 1 - (slot / PER_COLUMN) * COLUMN_WIDTH, z + (slot % PER_COLUMN) * STEP };
    }

    /** The items and their kinds in creative-tab order, then the items no tab lists. */
    static Map<String, List<ItemStack>> itemsByTab() {
        Map<String, List<ItemStack>> tabs = new LinkedHashMap<>();
        // Creative tabs are client code: a dedicated server lays everything out as one group
        boolean client = FMLCommonHandler.instance().getSide().isClient();
        if (client) {
            for (CreativeTabs tab : CreativeTabs.creativeTabArray) {
                tabs.put(tab.getTabLabel(), new ArrayList<>());
            }
        } else {
            tabs.put(ALL, new ArrayList<>());
        }
        tabs.put(NO_TAB, new ArrayList<>());
        for (Object name : Item.itemRegistry.getKeys()) {
            if (!String.valueOf(name).startsWith(DOMAIN)) continue;
            Item item = (Item) Item.itemRegistry.getObject(name);
            if (item instanceof ItemBlock) continue;
            CreativeTabs tab = client ? item.getCreativeTab() : null;
            tabs.get(tab != null ? tab.getTabLabel() : client ? NO_TAB : ALL).addAll(kinds(item, tab));
        }
        tabs.values().removeIf(List::isEmpty);
        return tabs;
    }

    /** The kinds an item lists in its tab; that list is client code, so a dedicated server takes the first only. */
    @SuppressWarnings("unchecked")
    private static List<ItemStack> kinds(Item item, CreativeTabs tab) {
        List<ItemStack> kinds = new ArrayList<>();
        if (tab != null && FMLCommonHandler.instance().getSide().isClient()) {
            try {
                item.getSubItems(item, tab, kinds);
            } catch (RuntimeException e) {
                LOG.error("Cannot list the kinds of {}", Item.itemRegistry.getNameForObject(item), e);
            }
        }
        if (kinds.isEmpty()) kinds.add(new ItemStack(item));
        return kinds;
    }

    private static void chest(WorldServer world, int x, int y, int z, ItemStack[] load) {
        world.setBlock(x, y, z, Blocks.chest, 0, 2);
        if (!(world.getTileEntity(x, y, z) instanceof TileEntityChest)) return;
        TileEntityChest chest = (TileEntityChest) world.getTileEntity(x, y, z);
        for (int i = 0; i < load.length; i++) {
            ItemStack stack = load[i].copy();
            stack.stackSize = Math.min(stack.getMaxStackSize(), Math.max(1, stack.stackSize));
            chest.setInventorySlotContents(i, stack);
        }
        chest.markDirty();
    }

    /** A full pile, as a player sets one down sneaking with enough in hand. */
    private static void pile(WorldServer world, int x, int y, int z, ItemStack stack) {
        IPiledComponent component = (IPiledComponent) stack.getItem();
        String type = component.getPileType();
        int max = BlockComponent.getStorageSize(type);
        world.setBlock(x, y, z, BlockListMF.components, 0, 2);
        TileEntityComponent tile = new TileEntityComponent();
        tile.setItem(stack.copy(), type, component.getPileTexture(), max, max);
        world.setTileEntity(x, y, z, tile);
        world.markBlockForUpdate(x, y, z);
    }

    /** The pile a component makes when set down, or null if it makes none. */
    private static String pileType(ItemStack stack) {
        if (!(stack.getItem() instanceof IPiledComponent)) return null;
        String type = ((IPiledComponent) stack.getItem()).getPileType();
        return BlockComponent.getStorageSize(type) > 0 ? type : null;
    }
}
