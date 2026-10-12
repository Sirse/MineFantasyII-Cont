package minefantasy.mf2.mechanics;

import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.mechanics.ProtectionFixtures.*;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.mining.RandomDigs;
import minefantasy.mf2.api.mining.RandomOre;
import minefantasy.mf2.gametest.DenyAll;
import minefantasy.mf2.gametest.Overrides;
import minefantasy.mf2.gametest.StubbornBlock;
import minefantasy.mf2.item.list.CustomToolListMF;

/** Trowel and handpick drops are paid only for a block really dug; gravel dug with a trowel always gives flint. */
@GameTestHolder("minefantasy2")
public class MiningDropsTest {

    private MiningDropsTest() {}

    private static EntityPlayerMP digger(GameTestHelper helper, TestPos at) {
        EntityPlayerMP player = miner(helper, at);
        player.setCurrentItemOrArmor(0, CustomToolListMF.standard_trow.construct("Iron", "OakWood"));
        return player;
    }

    @GameTest
    public static void gravelDugWithATrowelGivesFlint(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.gravel);
        TestPos at = helper.absolute(1, 1, 1);
        EntityPlayerMP player = digger(helper, at);
        assertTrue("the dig succeeds", player.theItemInWorldManager.tryHarvestBlock(at.x(), at.y(), at.z()));
        endOfTick(helper);
        assertEquals(Blocks.air, helper.getWorld().getBlock(at.x(), at.y(), at.z()));
        assertEquals("flint", 1, countItems(helper, at, Items.flint));
        assertEquals("no gravel", 0, countItems(helper, at, Item.getItemFromBlock(Blocks.gravel)));
        helper.succeed();
    }

    @GameTest
    public static void aTrowelBreakingOnItsLastDigStillGivesFlint(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.gravel);
        TestPos at = helper.absolute(1, 1, 1);
        EntityPlayerMP player = digger(helper, at);
        ItemStack trowel = player.getHeldItem();
        trowel.setItemDamage(trowel.getMaxDamage());
        assertTrue("the dig succeeds", player.theItemInWorldManager.tryHarvestBlock(at.x(), at.y(), at.z()));
        endOfTick(helper);
        assertTrue("the trowel did not break", player.getHeldItem() == null);
        assertEquals("flint", 1, countItems(helper, at, Items.flint));
        assertEquals("no gravel", 0, countItems(helper, at, Item.getItemFromBlock(Blocks.gravel)));
        helper.succeed();
    }

    @GameTest
    public static void randomDigsArePaidOnlyForARealDig(GameTestHelper helper) {
        ItemStack loot = new ItemStack(Items.diamond);
        RandomDigs dig = new RandomDigs(loot, 1000F, Blocks.dirt, OreDictionary.WILDCARD_VALUE, -1, -1, -1, false);
        try (Overrides overrides = new Overrides()
                .run(() -> RandomDigs.drops.add(dig), () -> RandomDigs.drops.remove(dig))) {
            helper.setBlock(1, 1, 1, Blocks.dirt);
            TestPos at = helper.absolute(1, 1, 1);
            EntityPlayerMP player = digger(helper, at);
            try (Overrides deny = new Overrides().listen(new DenyAll())) {
                assertFalse(player.theItemInWorldManager.tryHarvestBlock(at.x(), at.y(), at.z()));
            }
            endOfTick(helper);
            assertEquals("a refused dig pays nothing", 0, countItems(helper, at, Items.diamond));
            assertTrue(player.theItemInWorldManager.tryHarvestBlock(at.x(), at.y(), at.z()));
            endOfTick(helper);
            assertEquals("a real dig pays once", 1, countItems(helper, at, Items.diamond));
            assertEquals("the registered loot is untouched", 1, loot.stackSize);
        }
        helper.succeed();
    }

    private static EntityPlayerMP picker(GameTestHelper helper, TestPos at) {
        EntityPlayerMP player = miner(helper, at);
        player.setCurrentItemOrArmor(0, CustomToolListMF.standard_handpick.construct("Iron", "OakWood"));
        return player;
    }

    /** A dig the break event allows but the block refuses: nothing is paid, however often it is tried. */
    private static void refusedRemovalPaysNothing(GameTestHelper helper, EntityPlayerMP player, TestPos at) {
        for (int tries = 0; tries < 3; tries++) {
            assertFalse("the block refused", player.theItemInWorldManager.tryHarvestBlock(at.x(), at.y(), at.z()));
        }
        endOfTick(helper);
        assertEquals(StubbornBlock.block, helper.getWorld().getBlock(at.x(), at.y(), at.z()));
        assertEquals("a block that stayed pays nothing", 0, countItems(helper, at, Items.diamond));
    }

    @GameTest
    public static void aTrowelPaysNothingForABlockThatStays(GameTestHelper helper) {
        RandomDigs dig = new RandomDigs(
                new ItemStack(Items.diamond),
                1000F,
                StubbornBlock.block,
                OreDictionary.WILDCARD_VALUE,
                -1,
                -1,
                -1,
                false);
        try (Overrides overrides = new Overrides()
                .run(() -> RandomDigs.drops.add(dig), () -> RandomDigs.drops.remove(dig))) {
            helper.setBlock(1, 1, 1, StubbornBlock.block);
            TestPos at = helper.absolute(1, 1, 1);
            refusedRemovalPaysNothing(helper, digger(helper, at), at);
        }
        helper.succeed();
    }

    @GameTest
    public static void aHandpickPaysRandomOreOnlyForARealDig(GameTestHelper helper) {
        ItemStack loot = new ItemStack(Items.diamond);
        RandomOre ore = new RandomOre(loot, 1000F, Blocks.stone, OreDictionary.WILDCARD_VALUE, -1, -1, -1, false);
        try (Overrides overrides = new Overrides()
                .run(() -> RandomOre.drops.add(ore), () -> RandomOre.drops.remove(ore))) {
            helper.setBlock(1, 1, 1, Blocks.stone);
            TestPos at = helper.absolute(1, 1, 1);
            EntityPlayerMP player = picker(helper, at);
            try (Overrides deny = new Overrides().listen(new DenyAll())) {
                assertFalse(player.theItemInWorldManager.tryHarvestBlock(at.x(), at.y(), at.z()));
            }
            endOfTick(helper);
            assertEquals("a refused dig pays nothing", 0, countItems(helper, at, Items.diamond));
            assertTrue(player.theItemInWorldManager.tryHarvestBlock(at.x(), at.y(), at.z()));
            endOfTick(helper);
            assertEquals("a real dig pays once", 1, countItems(helper, at, Items.diamond));
            assertEquals("the registered loot is untouched", 1, loot.stackSize);
        }
        helper.succeed();
    }

    @GameTest
    public static void aHandpickPaysNothingForABlockThatStays(GameTestHelper helper) {
        RandomOre ore = new RandomOre(
                new ItemStack(Items.diamond),
                1000F,
                StubbornBlock.block,
                OreDictionary.WILDCARD_VALUE,
                -1,
                -1,
                -1,
                false);
        try (Overrides overrides = new Overrides()
                .run(() -> RandomOre.drops.add(ore), () -> RandomOre.drops.remove(ore))) {
            helper.setBlock(1, 1, 1, StubbornBlock.block);
            TestPos at = helper.absolute(1, 1, 1);
            refusedRemovalPaysNothing(helper, picker(helper, at), at);
        }
        helper.succeed();
    }
}
