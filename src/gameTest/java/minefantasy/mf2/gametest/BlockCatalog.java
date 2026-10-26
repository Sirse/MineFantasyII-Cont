package minefantasy.mf2.gametest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.FMLCommonHandler;

/**
 * Every MineFantasy block with a creative tab, one strip per tab north of the spawn, each block over a sign with its
 * registry name and metadata. It reads the registry, so a new block shows here without anyone listing it.
 */
final class BlockCatalog {

    private static final Logger LOG = LogManager.getLogger("MF2 showcase");
    private static final String DOMAIN = "minefantasy2:";
    /** Blocks in one row, and the room each takes along it. */
    private static final int PER_ROW = 24, STEP = 2;
    /** A row is a block, its sign south of it, and a gap. */
    private static final int ROW_DEPTH = 3;
    /** A sign facing south, toward the player walking up from the spawn. */
    private static final int FACING_SOUTH = 0;
    private static final String NO_TAB = "No tab", ALL = "All";
    /**
     * Blocks that show nothing on their own: the world generation marker turns to air at once, and a component pile is
     * drawn from its contents, so the storage district lays out real piles instead.
     */
    private static final Set<String> HIDDEN = new HashSet<>(
            Arrays.asList(DOMAIN + "WorldGenFlag", DOMAIN + "MF_ComponentStorage"));

    private BlockCatalog() {}

    /** Clears the catalog north of {@code (x, z)} and builds it anew; returns how many blocks it placed. */
    static int build(WorldServer world, int x, int z) {
        Map<String, List<BlockKind>> tabs = blocksByTab();
        int rows = 0;
        for (List<BlockKind> blocks : tabs.values()) {
            rows += (blocks.size() + PER_ROW - 1) / PER_ROW;
        }
        int y = ShowcaseWorld.GROUND;
        Showcase.clear(world, x - 1, z - rows * ROW_DEPTH - 1, x + (PER_ROW + 1) * STEP + 1, z);

        int row = 0, placed = 0;
        for (Map.Entry<String, List<BlockKind>> tab : tabs.entrySet()) {
            List<BlockKind> blocks = tab.getValue();
            for (int i = 0; i < blocks.size(); i++) {
                int column = i % PER_ROW;
                if (column == 0) {
                    int rowZ = z - (row++) * ROW_DEPTH - 2;
                    Showcase.sign(world, x, y, rowZ + 1, FACING_SOUTH, "Tab:/" + tab.getKey());
                }
                int blockX = x + (column + 1) * STEP, blockZ = z - (row - 1) * ROW_DEPTH - 2;
                if (place(world, blockX, y, blockZ, blocks.get(i))) placed++;
            }
        }
        return placed;
    }

    /**
     * The blocks and their kinds in creative-tab order, each tab's in registry order, then the blocks no tab lists (lit
     * furnaces, cogwork parts, technical blocks) under {@link #NO_TAB}.
     */
    static Map<String, List<BlockKind>> blocksByTab() {
        Map<String, List<BlockKind>> tabs = new LinkedHashMap<>();
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
        for (Object name : Block.blockRegistry.getKeys()) {
            if (!String.valueOf(name).startsWith(DOMAIN) || HIDDEN.contains(String.valueOf(name))) continue;
            Block block = (Block) Block.blockRegistry.getObject(name);
            CreativeTabs tab = client ? block.getCreativeTabToDisplayOn() : null;
            Item item = Item.getItemFromBlock(block);
            if (tab == null || item == null) {
                tabs.get(client ? NO_TAB : ALL).add(new BlockKind(block, 0));
            } else {
                tabs.get(tab.getTabLabel()).addAll(kinds(block, item, tab));
            }
        }
        tabs.values().removeIf(List::isEmpty);
        return tabs;
    }

    /** The kinds a block lists in its tab; that list is client code, so a dedicated server takes the first only. */
    @SuppressWarnings("unchecked")
    private static List<BlockKind> kinds(Block block, Item item, CreativeTabs tab) {
        List<ItemStack> stacks = new ArrayList<>();
        if (FMLCommonHandler.instance().getSide().isClient()) {
            try {
                block.getSubBlocks(item, tab, stacks);
            } catch (RuntimeException e) {
                LOG.error("Cannot list the kinds of {}", Block.blockRegistry.getNameForObject(block), e);
            }
        }
        if (stacks.isEmpty()) stacks.add(new ItemStack(item));
        List<BlockKind> kinds = new ArrayList<>();
        for (ItemStack stack : stacks) {
            kinds.add(new BlockKind(block, item.getMetadata(stack.getItemDamage())));
        }
        return kinds;
    }

    private static boolean place(WorldServer world, int x, int y, int z, BlockKind kind) {
        Block block = kind.block;
        int meta = kind.meta;
        String name = Block.blockRegistry.getNameForObject(block).substring(DOMAIN.length());
        try {
            world.setBlock(x, y, z, block, meta, 2);
            Showcase.sign(world, x, y, z + 1, FACING_SOUTH, meta == 0 ? name : name + "/meta " + meta);
            return true;
        } catch (RuntimeException e) {
            LOG.error("Cannot place {} with meta {}", name, meta, e);
            Showcase.sign(world, x, y, z + 1, FACING_SOUTH, "FAILED/" + name);
            return false;
        }
    }

    static final class BlockKind {

        final Block block;
        final int meta;

        BlockKind(Block block, int meta) {
            this.block = block;
            this.meta = meta;
        }
    }
}
