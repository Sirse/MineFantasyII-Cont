package minefantasy.mf2.api.helpers;

import static minefantasy.mf2.gametest.Assert.*;

import java.util.List;
import java.util.function.Function;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.block.tileentity.InventorySlots;

/**
 * Another mod's item decides what container it leaves. Whatever it returns, the mod takes one container per item used,
 * as a stack of its own that neither the item nor the next craft shares.
 */
@GameTestHolder("minefantasy2")
public class SafeStacksTest {

    private SafeStacksTest() {}

    /** An item of another mod whose getContainerItem does whatever the test says. */
    private static Item leaving(Function<ItemStack, ItemStack> container) {
        return new Item() {

            @Override
            public boolean hasContainerItem(ItemStack stack) {
                return true;
            }

            @Override
            public ItemStack getContainerItem(ItemStack stack) {
                return container.apply(stack);
            }
        };
    }

    @GameTest
    public static void oneUsedItemLeavesOneContainerOfItsOwn(GameTestHelper helper) {
        assertNull("no container became one", SafeStacks.containerOf(new ItemStack(leaving(s -> null))));

        for (int size : new int[] { 0, -3, 64, 127 }) {
            ItemStack odd = new ItemStack(Items.bucket);
            odd.stackSize = size;
            ItemStack left = SafeStacks.containerOf(new ItemStack(leaving(s -> odd), 16));
            assertNotNull(left);
            assertEquals("a container of " + size + " came back as " + left.stackSize, 1, left.stackSize);
        }

        // One the item keeps and hands out every time: the caller's copy must not be it
        ItemStack kept = new ItemStack(Items.bucket);
        ItemStack mine = SafeStacks.containerOf(new ItemStack(leaving(s -> kept)));
        assertNotSame(kept, mine);
        mine.stackSize = 50;
        assertEquals("changing the given container changed the kept one", 1, kept.stackSize);

        // One that returns the stack it was asked about, or changes it
        ItemStack used = new ItemStack(leaving(s -> s), 8);
        ItemStack self = SafeStacks.containerOf(used);
        assertNotSame(used, self);
        assertEquals("the used stack changed", 8, used.stackSize);
        ItemStack greedy = new ItemStack(leaving(s -> {
            s.stackSize = 0;
            return new ItemStack(Items.bucket);
        }), 8);
        SafeStacks.containerOf(greedy);
        assertEquals("the item emptied the stack it was used from", 8, greedy.stackSize);
        helper.succeed();
    }

    @GameTest
    public static void anItemThatSaysItHasNoContainerLeavesNone(GameTestHelper helper) {
        Item keeps = new Item() {

            @Override
            public boolean hasContainerItem(ItemStack stack) {
                return false;
            }

            @Override
            public ItemStack getContainerItem(ItemStack stack) {
                return new ItemStack(Items.bucket);
            }
        };
        assertNull("a container came back although the item has none", SafeStacks.containerOf(new ItemStack(keeps)));

        Item throwing = new Item() {

            @Override
            public boolean hasContainerItem(ItemStack stack) {
                return false;
            }

            @Override
            public ItemStack getContainerItem(ItemStack stack) {
                throw new IllegalStateException("asked for a container it does not have");
            }
        };
        assertNull(SafeStacks.containerOf(new ItemStack(throwing)));
        helper.succeed();
    }

    /** An item of another mod that claims a stack limit of zero or less. */
    private static Item limitedTo(int limit) {
        return new Item() {

            @Override
            public int getItemStackLimit(ItemStack stack) {
                return limit;
            }
        };
    }

    @GameTest
    public static void aStackLimitOfNothingNeitherLoopsNorMultiplies(GameTestHelper helper) {
        for (int limit : new int[] { 0, -1 }) {
            ItemStack odd = new ItemStack(limitedTo(limit), 5);
            TestPos at = helper.absolute(1, 1, 1 + 2 * (limit + 1));
            InventorySlots.drop(helper.getWorld(), at.x(), at.y(), at.z(), odd);
            List<?> dropped = helper.getWorld().getEntitiesWithinAABB(
                    EntityItem.class,
                    AxisAlignedBB.getBoundingBox(at.x(), at.y(), at.z(), at.x() + 1, at.y() + 1, at.z() + 1));
            int total = 0;
            for (Object o : dropped) {
                total += ((EntityItem) o).getEntityItem().stackSize;
                ((EntityItem) o).setDead();
            }
            assertEquals("a limit of " + limit + " dropped the wrong amount", 5, total);

            ItemStack read = new ItemStack(limitedTo(limit), 5);
            ItemStack kept = SafeStacks.withinAStack(read);
            assertTrue("a limit of " + limit + " emptied the stack", kept != null && kept.stackSize > 0);
        }
        helper.succeed();
    }

    @GameTest
    public static void aStackReadFromATagStaysWithinAStack(GameTestHelper helper) {
        for (int size : new int[] { Integer.MIN_VALUE, -1, 0 }) {
            ItemStack stack = new ItemStack(Items.arrow);
            stack.stackSize = size;
            assertNull(size + " arrows became a stack", SafeStacks.withinAStack(stack));
        }
        ItemStack many = new ItemStack(Items.arrow);
        many.stackSize = 500;
        assertEquals(64, SafeStacks.withinAStack(many).stackSize);
        assertNull(SafeStacks.withinAStack(null));
        helper.succeed();
    }
}
