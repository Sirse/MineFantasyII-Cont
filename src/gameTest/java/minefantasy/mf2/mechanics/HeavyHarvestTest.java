package minefantasy.mf2.mechanics;

import static minefantasy.mf2.gametest.Assert.*;

import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.stamina.StaminaBar;
import minefantasy.mf2.config.ConfigTools;
import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.item.list.CustomToolListMF;

/**
 * Heavy tools break more than the block they hit: a heavy pick a flat three by three across the face hit, a lumber axe
 * the rest of the tree; sneaking breaks only the one block.
 */
@GameTestHolder("minefantasy2")
public class HeavyHarvestTest {

    private HeavyHarvestTest() {}

    private static FakePlayer holding(GameTestHelper helper, Item tool) {
        FakePlayer player = Modders.fresh(helper, Modders.SMITH);
        player.inventory
                .setInventorySlotContents(player.inventory.currentItem, CustomToolHelper.construct(tool, "Steel"));
        return player;
    }

    /** Starts breaking the block at (x, y, z), hit on the given face, as the player would. */
    private static void strike(GameTestHelper helper, FakePlayer player, int x, int y, int z, int face) {
        TestPos at = helper.absolute(x, y, z);
        new HeavyHarvest().onLeftClick(
                new PlayerInteractEvent(
                        player,
                        PlayerInteractEvent.Action.LEFT_CLICK_BLOCK,
                        at.x(),
                        at.y(),
                        at.z(),
                        face,
                        helper.getWorld()));
        ItemStack tool = player.getHeldItem();
        tool.getItem().onBlockStartBreak(tool, at.x(), at.y(), at.z(), player);
    }

    private static Block at(GameTestHelper helper, int x, int y, int z) {
        TestPos pos = helper.absolute(x, y, z);
        return helper.getWorld().getBlock(pos.x(), pos.y(), pos.z());
    }

    private static void fill(GameTestHelper helper, Block block) {
        for (int x = 1; x <= 3; x++) {
            for (int y = 1; y <= 3; y++) {
                for (int z = 1; z <= 3; z++) {
                    helper.setBlock(x, y, z, block);
                }
            }
        }
    }

    private static <T> T withoutStaminaOrCrumbling(java.util.concurrent.Callable<T> body) throws Exception {
        boolean stamina = StaminaBar.isSystemActive;
        float crumble = ConfigTools.hvyDropChance;
        StaminaBar.isSystemActive = false;
        ConfigTools.hvyDropChance = 0F;
        try {
            return body.call();
        } finally {
            StaminaBar.isSystemActive = stamina;
            ConfigTools.hvyDropChance = crumble;
        }
    }

    @GameTest
    public static void aHeavyPickBreaksASquareAcrossTheFaceHit(GameTestHelper helper) throws Exception {
        withoutStaminaOrCrumbling(() -> {
            fill(helper, Blocks.stone);
            // Hit on the north face (2): the square spans x and y at z = 2
            strike(helper, holding(helper, CustomToolListMF.standard_hvypick), 2, 2, 2, 2);
            for (int x = 1; x <= 3; x++) {
                for (int y = 1; y <= 3; y++) {
                    for (int z = 1; z <= 3; z++) {
                        boolean broken = z == 2 && !(x == 2 && y == 2);
                        assertEquals(
                                "stone at " + x + "," + y + "," + z,
                                broken ? Blocks.air : Blocks.stone,
                                at(helper, x, y, z));
                    }
                }
            }
            return null;
        });
        helper.succeed();
    }

    @GameTest
    public static void sneakingBreaksOnlyTheOneBlock(GameTestHelper helper) throws Exception {
        withoutStaminaOrCrumbling(() -> {
            fill(helper, Blocks.stone);
            FakePlayer player = holding(helper, CustomToolListMF.standard_hvypick);
            player.setSneaking(true);
            strike(helper, player, 2, 2, 2, 1);
            assertEquals(Blocks.stone, at(helper, 1, 2, 1));
            assertEquals(Blocks.stone, at(helper, 3, 2, 3));
            return null;
        });
        helper.succeed();
    }

    @GameTest
    public static void aLumberAxeFellsTheRestOfTheTree(GameTestHelper helper) throws Exception {
        withoutStaminaOrCrumbling(() -> {
            for (int y = 1; y <= 5; y++) {
                helper.setBlock(2, y, 2, Blocks.log);
            }
            helper.setBlock(3, 1, 2, Blocks.stone);
            strike(helper, holding(helper, CustomToolListMF.standard_lumber), 2, 1, 2, 2);
            for (int y = 2; y <= 5; y++) {
                assertEquals("log at height " + y, Blocks.air, at(helper, 2, y, 2));
            }
            assertEquals("the axe took stone", Blocks.stone, at(helper, 3, 1, 2));
            return null;
        });
        helper.succeed();
    }

    /** A pick about to break keeps its last use for the block hit, so that block still drops. */
    @GameTest
    public static void aWornPickKeepsItsLastUseForTheBlockHit(GameTestHelper helper) throws Exception {
        withoutStaminaOrCrumbling(() -> {
            fill(helper, Blocks.stone);
            FakePlayer player = holding(helper, CustomToolListMF.standard_hvypick);
            ItemStack pick = player.getHeldItem();
            pick.setItemDamage(pick.getMaxDamage() - 1);
            strike(helper, player, 2, 2, 2, 2);
            assertSame("the pick broke on the blocks around", pick, player.getHeldItem());
            assertEquals(Blocks.stone, at(helper, 1, 1, 2));
            return null;
        });
        helper.succeed();
    }

    @GameTest
    public static void silkTouchGivesNoExperience(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.diamond_ore);
        TestPos at = helper.absolute(1, 1, 1);
        FakePlayer player = holding(helper, CustomToolListMF.standard_hvypick);
        assertTrue(
                "diamond ore gave no experience",
                ProtectionHelper.breakExperience(player, helper.getWorld(), at.x(), at.y(), at.z()) > 0);
        player.getHeldItem().addEnchantment(Enchantment.silkTouch, 1);
        assertEquals(0, ProtectionHelper.breakExperience(player, helper.getWorld(), at.x(), at.y(), at.z()));
        helper.succeed();
    }
}
