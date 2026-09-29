package minefantasy.mf2.api.helpers;

import static minefantasy.mf2.gametest.Assert.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.MineFantasyII;
import minefantasy.mf2.gametest.Modders;

/**
 * Data the code does not expect: items without a tag or with an empty one, block metadata of any value, tiles saved and
 * loaded again. Every item and block of the mod is tried, and every failure is listed, not just the first.
 */
@GameTestHolder("minefantasy2")
public class RobustnessTest {

    private RobustnessTest() {}

    private interface Check {

        void run() throws Throwable;
    }

    private static void attempt(List<String> problems, String what, Check check) {
        try {
            check.run();
        } catch (Throwable t) {
            StringWriter trace = new StringWriter();
            t.printStackTrace(new PrintWriter(trace));
            String[] lines = trace.toString().split("\n");
            problems.add(what + ": " + t + (lines.length > 1 ? " " + lines[1].trim() : ""));
        }
    }

    private static void report(List<String> problems) {
        if (!problems.isEmpty()) {
            fail(problems.size() + " failed:\n  " + String.join("\n  ", problems));
        }
    }

    private static boolean ours(String id) {
        return id != null && id.startsWith(MineFantasyII.MODID + ":");
    }

    // region items

    /** The item as the creative menu, a command or another mod may hand it over: no tag, or an empty one. */
    private static List<ItemStack> bare(Item item) {
        List<ItemStack> stacks = new ArrayList<>();
        int metas = item.getHasSubtypes() ? 16 : 1;
        for (int meta = 0; meta < metas; meta++) {
            stacks.add(new ItemStack(item, 1, meta));
            ItemStack tagged = new ItemStack(item, 1, meta);
            tagged.setTagCompound(new NBTTagCompound());
            stacks.add(tagged);
        }
        return stacks;
    }

    @GameTest
    public static void everyItemCopesWithoutItsTag(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        World world = helper.getWorld();
        FakePlayer player = Modders.fresh(helper, Modders.SMITH);
        TestPos at = helper.absolute(1, 1, 1);
        player.setPosition(at.x() + 0.5, at.y(), at.z() + 0.5);
        for (Object o : Item.itemRegistry) {
            Item item = (Item) o;
            String id = Item.itemRegistry.getNameForObject(item);
            if (!ours(id)) continue;
            for (ItemStack stack : bare(item)) {
                String what = id + "@" + stack.getItemDamage() + (stack.hasTagCompound() ? "{}" : "");
                attempt(problems, what + " name", () -> item.getItemStackDisplayName(stack));
                attempt(problems, what + " durability", () -> {
                    item.getMaxDamage(stack);
                    item.getDisplayDamage(stack);
                    item.isDamaged(stack);
                    item.showDurabilityBar(stack);
                    item.getDurabilityForDisplay(stack);
                });
                attempt(problems, what + " use", () -> {
                    item.getItemUseAction(stack);
                    item.getMaxItemUseDuration(stack);
                    item.getItemStackLimit(stack);
                });
                attempt(problems, what + " attributes", () -> item.getAttributeModifiers(stack));
                attempt(
                        problems,
                        what + " container",
                        () -> { if (item.hasContainerItem(stack)) item.getContainerItem(stack); });
                attempt(problems, what + " held", () -> {
                    player.inventory.setInventorySlotContents(0, stack.copy());
                    player.inventory.currentItem = 0;
                    ItemStack held = player.inventory.getStackInSlot(0);
                    item.onUpdate(held, world, player, 0, true);
                });
                if (item instanceof ItemArmor) {
                    attempt(problems, what + " worn", () -> item.onArmorTick(world, player, stack.copy()));
                }
            }
        }
        player.inventory.clearInventory(null, -1);
        report(problems);
        helper.succeed();
    }

    // endregion

    // region blocks

    @GameTest(timeoutTicks = 400)
    public static void everyBlockCopesWithAnyMetadata(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        World world = helper.getWorld();
        TestPos at = helper.absolute(1, 1, 1);
        TestPos empty = helper.absolute(3, 1, 1);
        for (Object o : Block.blockRegistry) {
            Block block = (Block) o;
            String id = Block.blockRegistry.getNameForObject(block);
            if (!ours(id)) continue;
            for (int meta = 0; meta < 16; meta++) {
                int m = meta;
                String what = id + ":" + meta;
                attempt(problems, what + " drop", () -> block.damageDropped(m));
                // Harvesting asks for drops once the block and its tile are already gone
                attempt(
                        problems,
                        what + " drops without the block",
                        () -> block.getDrops(world, empty.x(), empty.y(), empty.z(), m, 0));
                attempt(problems, what + " placed", () -> {
                    world.setBlock(at.x(), at.y(), at.z(), block, m, 2);
                    block.getDrops(world, at.x(), at.y(), at.z(), m, 0);
                    world.setBlock(at.x(), at.y(), at.z(), net.minecraft.init.Blocks.air, 0, 2);
                });
            }
        }
        report(problems);
        helper.succeed();
    }

    // endregion

    // region tiles

    @GameTest
    public static void everyTileLoadsWhatItSaved(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        World world = helper.getWorld();
        TestPos at = helper.absolute(1, 1, 1);
        for (Object o : Block.blockRegistry) {
            Block block = (Block) o;
            String id = Block.blockRegistry.getNameForObject(block);
            if (!ours(id) || !block.hasTileEntity(0)) continue;
            attempt(problems, id, () -> {
                world.setBlock(at.x(), at.y(), at.z(), block, 0, 2);
                TileEntity tile = world.getTileEntity(at.x(), at.y(), at.z());
                if (tile == null) return;
                if (tile instanceof IInventory && ((IInventory) tile).getSizeInventory() > 0) {
                    ((IInventory) tile).setInventorySlotContents(0, new ItemStack(net.minecraft.init.Items.coal, 1));
                }
                NBTTagCompound saved = new NBTTagCompound();
                tile.writeToNBT(saved);
                TileEntity loaded = TileEntity.createAndLoadEntity(saved);
                assertNotNull(id + " was not loaded back", loaded);
                NBTTagCompound again = new NBTTagCompound();
                loaded.writeToNBT(again);
                assertEquals(id + " saved something else after loading", saved, again);
                world.setBlock(at.x(), at.y(), at.z(), net.minecraft.init.Blocks.air, 0, 2);
            });
            world.setBlock(at.x(), at.y(), at.z(), net.minecraft.init.Blocks.air, 0, 2);
        }
        report(problems);
        helper.succeed();
    }

    // endregion
}
