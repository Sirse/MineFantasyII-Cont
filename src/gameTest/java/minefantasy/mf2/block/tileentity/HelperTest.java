package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.gametest.TestItems.*;

import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.oredict.OreDictionary;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.MineFantasyII;
import minefantasy.mf2.api.crafting.RecipePattern;
import minefantasy.mf2.api.helpers.ItemQuality;
import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.item.list.ToolListMF;

/** The shared pattern reader of the native registration methods, and the slot helpers of station inventories. */
@GameTestHolder("minefantasy2")
public class HelperTest {

    private HelperTest() {}

    // region recipe patterns

    @GameTest
    public static void patternRowsAsStringsOrAnArrayReadTheSame(GameTestHelper helper) throws Exception {
        RecipePattern loose = RecipePattern.shaped("AB", "BA", 'A', seed, 'B', new ItemStack(bar, 1, 3));
        RecipePattern array = RecipePattern
                .shaped(new String[] { "AB", "BA" }, 'A', seed, 'B', new ItemStack(bar, 1, 3));
        for (RecipePattern pattern : new RecipePattern[] { loose, array }) {
            assertEquals(2, pattern.width);
            assertEquals(2, pattern.height);
            assertEquals(seed, pattern.cells[0].getItem());
            assertEquals(
                    "an item key takes any metadata",
                    OreDictionary.WILDCARD_VALUE,
                    pattern.cells[0].getItemDamage());
            assertEquals(bar, pattern.cells[1].getItem());
            assertEquals("a stack key keeps its metadata", 3, pattern.cells[1].getItemDamage());
            assertEquals(bar, pattern.cells[2].getItem());
            assertEquals(seed, pattern.cells[3].getItem());
        }
        helper.succeed();
    }

    @GameTest
    public static void raggedRowsAndUnkeyedCharactersAreEmptyCells(GameTestHelper helper) throws Exception {
        RecipePattern pattern = RecipePattern.shaped("A", "A A", "x", 'A', Blocks.stone);
        assertEquals("the widest row sets the width", 3, pattern.width);
        assertEquals(3, pattern.height);
        assertEquals(Item.getItemFromBlock(Blocks.stone), pattern.cells[0].getItem());
        assertNull("a short row ends in empty cells", pattern.cells[1]);
        assertNull(pattern.cells[2]);
        assertNull("a space is empty", pattern.cells[4]);
        assertNotNull(pattern.cells[5]);
        assertNull("a character without a key is empty", pattern.cells[6]);
        helper.succeed();
    }

    @GameTest
    public static void patternCellsAreCopies(GameTestHelper helper) throws Exception {
        ItemStack key = new ItemStack(bar);
        RecipePattern pattern = RecipePattern.shaped("AA", 'A', key);
        assertNotSame(key, pattern.cells[0]);
        assertNotSame(pattern.cells[0], pattern.cells[1]);
        pattern.cells[0].stackSize = 9;
        assertEquals(1, pattern.cells[1].stackSize);
        assertEquals(1, key.stackSize);
        helper.succeed();
    }

    @GameTest
    public static void shapelessItemsTakeMetadataZero(GameTestHelper helper) throws Exception {
        ItemStack[] stacks = RecipePattern.shapeless(seed, Blocks.stone, new ItemStack(bar, 2, 5));
        assertEquals(3, stacks.length);
        assertEquals(0, stacks[0].getItemDamage());
        assertEquals(0, stacks[1].getItemDamage());
        assertEquals(5, stacks[2].getItemDamage());
        assertEquals(2, stacks[2].stackSize);
        helper.succeed();
    }

    @GameTest
    public static void patternsRefuseWhatTheyCannotRead(GameTestHelper helper) throws Exception {
        assertThrows(IllegalArgumentException.class, () -> RecipePattern.shaped('A', seed));
        assertThrows(IllegalArgumentException.class, () -> RecipePattern.shaped("A", "A", seed));
        assertThrows(IllegalArgumentException.class, () -> RecipePattern.shaped("A", 'A'));
        assertThrows(IllegalArgumentException.class, () -> RecipePattern.shaped("A", 'A', "ingotIron"));
        assertThrows(IllegalArgumentException.class, () -> RecipePattern.shapeless(seed, "ingotIron"));
        helper.succeed();
    }

    // endregion

    @GameTest
    public static void fakePlayersBearTheMakersNames(GameTestHelper helper) throws Exception {
        for (String name : new String[] { Modders.SCHOLAR, Modders.NOVICE, Modders.SMITH }) {
            assertTrue(name, MineFantasyII.isNameModder(name));
        }
        helper.succeed();
    }

    // region quality

    @GameTest
    public static void anItemIsOrdinaryInferiorOrSuperior(GameTestHelper helper) throws Exception {
        ItemStack hammer = new ItemStack(ToolListMF.hammerStone);
        assertEquals(ItemQuality.Grade.ORDINARY, ItemQuality.getGrade(hammer));
        assertEquals(ItemQuality.ORDINARY, ItemQuality.get(hammer), 0F);
        ItemQuality.setGrade(hammer, ItemQuality.Grade.INFERIOR);
        assertTrue(hammer.getTagCompound().getBoolean(ItemQuality.INFERIOR_KEY));
        assertEquals(5F, ItemQuality.getGrade(hammer).scale(10F, 2F), 0F);
        ItemQuality.setGrade(hammer, ItemQuality.Grade.SUPERIOR);
        assertEquals(20F, ItemQuality.getGrade(hammer).scale(10F, 2F), 0F);
        ItemQuality.setGrade(hammer, ItemQuality.Grade.ORDINARY);
        assertFalse("an ordinary item kept a mark", hammer.getTagCompound().hasKey(ItemQuality.INFERIOR_KEY));
        helper.succeed();
    }

    @GameTest
    public static void stackingItemsHaveNoQuality(GameTestHelper helper) throws Exception {
        ItemStack stack = new ItemStack(ore, 4);
        ItemQuality.set(stack, 150F);
        ItemQuality.setGrade(stack, ItemQuality.Grade.SUPERIOR);
        assertFalse(stack.hasTagCompound());
        assertEquals(ItemQuality.ORDINARY, ItemQuality.get(stack), 0F);
        helper.succeed();
    }

    // endregion

    // region inventory slots

    @GameTest
    public static void takingPartOfASlotLeavesTheRest(GameTestHelper helper) throws Exception {
        ItemStack[] slots = { new ItemStack(ore, 5), null };
        ItemStack taken = InventorySlots.take(slots, 0, 2);
        assertEquals(2, taken.stackSize);
        assertEquals(3, slots[0].stackSize);
        assertNotSame(taken, slots[0]);
        assertNull("an empty slot gives nothing", InventorySlots.take(slots, 1, 2));
        helper.succeed();
    }

    @GameTest
    public static void takingAllOrMoreEmptiesTheSlot(GameTestHelper helper) throws Exception {
        ItemStack stack = new ItemStack(ore, 3);
        ItemStack[] slots = { stack };
        assertSame("the whole stack is handed over", stack, InventorySlots.take(slots, 0, 5));
        assertNull(slots[0]);
        slots[0] = new ItemStack(bar);
        assertEquals(bar, InventorySlots.takeAll(slots, 0).getItem());
        assertNull(slots[0]);
        assertNull(InventorySlots.takeAll(slots, 0));
        helper.succeed();
    }

    @GameTest
    public static void savedSlotsLoadBackWithTheirGaps(GameTestHelper helper) throws Exception {
        ItemStack[] slots = { new ItemStack(ore, 2), null, new ItemStack(bar, 7) };
        NBTTagCompound nbt = new NBTTagCompound();
        InventorySlots.write(nbt, "Items", slots);
        assertEquals("only filled slots are saved", 2, nbt.getTagList("Items", 10).tagCount());
        ItemStack[] loaded = InventorySlots.read(nbt, "Items", 3);
        assertEquals(2, loaded[0].stackSize);
        assertNull(loaded[1]);
        assertEquals(bar, loaded[2].getItem());
        assertEquals(7, loaded[2].stackSize);
        helper.succeed();
    }

    @GameTest
    public static void loadingDropsSlotsTheInventoryNoLongerHas(GameTestHelper helper) throws Exception {
        NBTTagCompound nbt = new NBTTagCompound();
        InventorySlots.write(nbt, "Items", new ItemStack[] { new ItemStack(ore), null, null, new ItemStack(bar) });
        ItemStack[] smaller = InventorySlots.read(nbt, "Items", 2);
        assertEquals(2, smaller.length);
        assertEquals(ore, smaller[0].getItem());
        assertEquals("a missing key loads an empty inventory", 3, InventorySlots.read(nbt, "Other", 3).length);
        NBTTagCompound corrupt = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        NBTTagCompound negative = new NBTTagCompound();
        negative.setByte("Slot", (byte) -1);
        new ItemStack(ore).writeToNBT(negative);
        list.appendTag(negative);
        corrupt.setTag("Items", list);
        assertNull("a slot number out of range is dropped", InventorySlots.read(corrupt, "Items", 2)[0]);
        helper.succeed();
    }

    // endregion
}
