package minefantasy.mf2.block;

import static minefantasy.mf2.gametest.Assert.*;

import java.lang.reflect.Field;
import java.util.Random;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.block.crafting.BlockRepairKit;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.item.list.CustomToolListMF;
import minefantasy.mf2.item.list.ToolListMF;

/**
 * Repair kits, as the research book describes them: a damaged item used on a placed kit may be mended by the kit's
 * share of its durability, and the kit may break doing it; only an ornate kit takes enchanted items.
 */
@GameTestHolder("minefantasy2")
public class RepairKitTest {

    private RepairKitTest() {}

    /** A random source that always rolls the same float. */
    private static Random rolling(float value) {
        return new Random() {

            @Override
            public float nextFloat() {
                return value;
            }
        };
    }

    private interface Use {

        void run() throws Exception;
    }

    /** Runs the use with the kit's rolls fixed: 0 succeeds and breaks the kit, 0.99 fails and keeps it. */
    private static void rolled(BlockRepairKit kit, float roll, Use use) throws Exception {
        Field field = BlockRepairKit.class.getDeclaredField("rand");
        field.setAccessible(true);
        Object kept = field.get(kit);
        field.set(kit, rolling(roll));
        try {
            use.run();
        } finally {
            field.set(kit, kept);
        }
    }

    private static boolean use(GameTestHelper helper, BlockRepairKit kit, ItemStack held) {
        helper.setBlock(1, 1, 1, kit);
        TestPos at = helper.absolute(1, 1, 1);
        FakePlayer player = Modders.fresh(helper, Modders.SMITH);
        player.inventory.currentItem = 0;
        player.inventory.setInventorySlotContents(0, held);
        return kit.onBlockActivated(helper.getWorld(), at.x(), at.y(), at.z(), player, 1, 0.5F, 0.5F, 0.5F);
    }

    private static boolean kitStands(GameTestHelper helper, BlockRepairKit kit) {
        TestPos at = helper.absolute(1, 1, 1);
        return helper.getWorld().getBlock(at.x(), at.y(), at.z()) == kit;
    }

    private static ItemStack damaged(ItemStack stack, float share) {
        stack.setItemDamage((int) (stack.getMaxDamage() * share));
        return stack;
    }

    @GameTest
    public static void aBasicKitMendsAQuarterOfACustomTool(GameTestHelper helper) throws Exception {
        BlockRepairKit kit = (BlockRepairKit) BlockListMF.repair_basic;
        ItemStack sword = damaged(CustomToolHelper.construct(CustomToolListMF.standard_sword, "iron", "oakwood"), 0.5F);
        int before = sword.getItemDamage();
        rolled(kit, 0.99F, () -> {
            assertTrue(use(helper, kit, sword));
            assertEquals("a failed repair changed the tool", before, sword.getItemDamage());
            assertTrue("a failed repair broke the kit", kitStands(helper, kit));
        });
        rolled(kit, 0F, () -> {
            assertTrue(use(helper, kit, sword));
            assertEquals(before - (int) (sword.getMaxDamage() * 0.25F), sword.getItemDamage());
            assertFalse("the kit did not break on a roll under its chance", kitStands(helper, kit));
        });
        helper.succeed();
    }

    @GameTest
    public static void onlyAnOrnateKitTakesEnchantedItems(GameTestHelper helper) throws Exception {
        ItemStack enchanted = damaged(
                CustomToolHelper.construct(CustomToolListMF.standard_sword, "iron", "oakwood"),
                0.8F);
        enchanted.addEnchantment(Enchantment.sharpness, 1);
        BlockRepairKit advanced = (BlockRepairKit) BlockListMF.repair_advanced;
        int before = enchanted.getItemDamage();
        rolled(advanced, 0F, () -> {
            assertFalse("an advanced kit took an enchanted item", use(helper, advanced, enchanted));
            assertEquals(before, enchanted.getItemDamage());
        });
        BlockRepairKit ornate = (BlockRepairKit) BlockListMF.repair_ornate;
        rolled(ornate, 0F, () -> {
            assertTrue(use(helper, ornate, enchanted));
            assertEquals(
                    "an enchanted item is mended by half",
                    Math.max(0, before - (int) (enchanted.getMaxDamage() * 0.5F)),
                    enchanted.getItemDamage());
        });
        helper.succeed();
    }

    /** An undamaged item has nothing to mend; the kit must not be risked on it. */
    @GameTest
    public static void aKitIsNotRiskedOnAnUndamagedItem(GameTestHelper helper) throws Exception {
        BlockRepairKit kit = (BlockRepairKit) BlockListMF.repair_basic;
        rolled(kit, 0F, () -> {
            use(helper, kit, new ItemStack(Items.iron_sword));
            assertTrue("the kit broke mending a whole vanilla sword", kitStands(helper, kit));
            use(helper, kit, CustomToolHelper.construct(CustomToolListMF.standard_sword, "iron", "oakwood"));
            assertTrue("the kit broke mending a whole custom sword", kitStands(helper, kit));
        });
        helper.succeed();
    }

    @GameTest
    public static void aKitMendsTheModsOwnTools(GameTestHelper helper) throws Exception {
        BlockRepairKit kit = (BlockRepairKit) BlockListMF.repair_advanced;
        ItemStack hammer = damaged(new ItemStack(ToolListMF.hammerStone), 0.5F);
        ItemStack vanilla = damaged(new ItemStack(Items.iron_pickaxe), 0.5F);
        rolled(kit, 0.01F, () -> {
            use(helper, kit, vanilla);
            assertEquals("a vanilla tool was not mended", 0, vanilla.getItemDamage());
            use(helper, kit, hammer);
            assertEquals("the mod's own stone hammer was not mended", 0, hammer.getItemDamage());
        });
        helper.succeed();
    }
}
