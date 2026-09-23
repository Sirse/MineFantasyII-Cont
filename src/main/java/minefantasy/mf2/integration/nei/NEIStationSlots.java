package minefantasy.mf2.integration.nei;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.client.gui.Gui;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.PositionedStack;
import minefantasy.mf2.api.crafting.CustomCrafterEntry;
import minefantasy.mf2.api.tool.IToolMF;
import minefantasy.mf2.block.crafting.BlockAnvilMF;
import minefantasy.mf2.block.crafting.BlockCarpenter;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.tileentity.TileEntityAnvilMF;
import minefantasy.mf2.util.MFLogUtil;

/**
 * The tool and station a bench recipe needs, shown on its NEI page as real item slots cycling through what qualifies,
 * so they get item tooltips and open their own recipes on click. A requirement nothing can meet, or bare hands, gets no
 * slot at all.
 */
public final class NEIStationSlots {

    private static final Map<String, List<ItemStack>> TOOLS = new HashMap<String, List<ItemStack>>();
    private static final Map<Integer, List<ItemStack>> ANVILS = new HashMap<Integer, List<ItemStack>>();

    private NEIStationSlots() {}

    /** Every tool of the type at or above the tier, material variants included */
    public static List<ItemStack> tools(String toolType, int tier) {
        if (toolType == null || toolType.equalsIgnoreCase("hands") || toolType.equalsIgnoreCase("nothing")) {
            return Collections.emptyList();
        }
        String key = toolType.toLowerCase() + ":" + tier;
        List<ItemStack> found = TOOLS.get(key);
        if (found == null) {
            found = findTools(toolType, tier);
            TOOLS.put(key, found);
        }
        return found;
    }

    private static List<ItemStack> findTools(String toolType, int tier) {
        List<ItemStack> found = new ArrayList<ItemStack>();
        for (Object object : Item.itemRegistry) {
            Item item = (Item) object;
            if (!(item instanceof IToolMF)) {
                continue;
            }
            IToolMF tool = (IToolMF) item;
            List<ItemStack> variants = new ArrayList<ItemStack>();
            try {
                item.getSubItems(item, item.getCreativeTab(), variants);
            } catch (Throwable t) {
                MFLogUtil.warnOnce("nei-tool-variants-" + item, "Could not list variants of {} for NEI", item, t);
                continue;
            }
            for (ItemStack variant : variants) {
                if (variant != null && toolType.equalsIgnoreCase(tool.getToolType(variant))
                        && tool.getTier(variant) >= tier) {
                    found.add(variant);
                }
            }
        }
        for (CustomCrafterEntry entry : CustomCrafterEntry.entries.values()) {
            if (entry.itemID != null && toolType.equalsIgnoreCase(entry.type) && entry.tier >= tier) {
                found.add(new ItemStack(entry.itemID));
            }
        }
        return found;
    }

    /**
     * Every anvil for a recipe of this tier. A lower anvil still works, only with a far narrower hit window, so none is
     * left out; the lower ones carry a tooltip line with the penalty instead. The line rides in the stack's own lore so
     * it shows however NEI builds the tooltip.
     */
    public static List<ItemStack> anvils(int requiredTier) {
        List<ItemStack> found = ANVILS.get(requiredTier);
        if (found == null) {
            found = new ArrayList<ItemStack>();
            addAnvil(found, BlockListMF.anvilStone, requiredTier);
            if (BlockListMF.anvil != null) {
                for (Block anvil : BlockListMF.anvil) {
                    addAnvil(found, anvil, requiredTier);
                }
            }
            ANVILS.put(requiredTier, found);
        }
        return found;
    }

    private static void addAnvil(List<ItemStack> found, Block block, int requiredTier) {
        if (!(block instanceof BlockAnvilMF)) {
            return;
        }
        ItemStack stack = new ItemStack(block);
        if (((BlockAnvilMF) block).getTier() < requiredTier) {
            NBTTagList lore = new NBTTagList();
            lore.appendTag(
                    new NBTTagString(
                            EnumChatFormatting.RED + StatCollector.translateToLocalFormatted(
                                    "nei.minefantasy2.anvil.low_tier",
                                    requiredTier,
                                    Math.round(TileEntityAnvilMF.LOW_TIER_HIT_WINDOW * 100))));
            NBTTagCompound display = new NBTTagCompound();
            display.setTag("Lore", lore);
            NBTTagCompound tag = new NBTTagCompound();
            tag.setTag("display", display);
            stack.setTagCompound(tag);
        }
        found.add(stack);
    }

    /**
     * Tints an anvil slot red while it shows an anvil below the recipe's tier. Call from drawExtras, which runs after
     * the items are drawn.
     */
    public static void drawPenaltyTint(PositionedStack slot, int requiredTier) {
        if (slot == null || slot.item == null || slot.item.getItem() == null) {
            return;
        }
        Block block = Block.getBlockFromItem(slot.item.getItem());
        if (block instanceof BlockAnvilMF && ((BlockAnvilMF) block).getTier() < requiredTier) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            Gui.drawRect(slot.relx, slot.rely, slot.relx + 16, slot.rely + 16, 0x60FF2020);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glColor4f(1F, 1F, 1F, 1F);
        }
    }

    /** The carpenter bench, or nothing when the recipe asks for a tier no bench has (scripts can) */
    public static List<ItemStack> carpenters(int tier) {
        BlockCarpenter bench = BlockListMF.carpenter;
        return bench != null && bench.getTier() >= tier ? single(bench) : Collections.<ItemStack>emptyList();
    }

    public static List<ItemStack> single(Block block) {
        return block == null ? Collections.<ItemStack>emptyList() : Collections.singletonList(new ItemStack(block));
    }

    /** A slot inside the 20px frame at x,y, or null when nothing qualifies */
    public static PositionedStack slot(List<ItemStack> items, int x, int y) {
        if (items.isEmpty()) {
            return null;
        }
        return new PositionedStack(new ArrayList<ItemStack>(items), x + 2, y + 2, false);
    }

    /** Steps a slot to the item for this moment, at the pace NEI cycles ingredients */
    public static void cycle(PositionedStack slot, int cycleticks) {
        if (slot != null && slot.items.length > 1) {
            slot.setPermutationToRender((cycleticks / 20) % slot.items.length);
        }
    }

    public static void drawFrame(PositionedStack slot) {
        if (slot == null) {
            return;
        }
        GL11.glColor4f(1F, 1F, 1F, 1F);
        GuiDraw.changeTexture("minefantasy2:textures/gui/icons.png");
        GuiDraw.drawTexturedModalRect(slot.relx - 2, slot.rely - 2, 20, 0, 20, 20);
    }
}
