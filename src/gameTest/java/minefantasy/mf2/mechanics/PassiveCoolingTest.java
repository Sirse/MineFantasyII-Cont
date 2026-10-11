package minefantasy.mf2.mechanics;

import static minefantasy.mf2.gametest.Assert.*;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.heating.AmbientTemperature;
import minefantasy.mf2.api.heating.TongsHelper;
import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.item.heatable.ItemHeated;
import minefantasy.mf2.item.list.ToolListMF;

@GameTestHolder("minefantasy2")
public class PassiveCoolingTest {

    private static ItemStack piece(int temp) {
        ItemStack sword = new ItemStack(Items.iron_sword, 1, 17);
        sword.setStackDisplayName("Saved sword");
        sword.addEnchantment(net.minecraft.enchantment.Enchantment.sharpness, 2);
        sword.getTagCompound().setString("marker", "keep");
        return ItemHeated.createHotItem(sword, temp);
    }

    private static void assertSword(ItemStack sword) {
        assertEquals(Items.iron_sword, sword.getItem());
        assertEquals(17, sword.getItemDamage());
        assertEquals("Saved sword", sword.getDisplayName());
        assertEquals("keep", sword.getTagCompound().getString("marker"));
        assertEquals(
                2,
                net.minecraft.enchantment.EnchantmentHelper
                        .getEnchantmentLevel(net.minecraft.enchantment.Enchantment.sharpness.effectId, sword));
        assertFalse(sword.getTagCompound().hasKey("MF_QuenchCracked"));
        assertFalse(sword.getTagCompound().hasKey("MF_Quench"));
    }

    @GameTest
    public static void coldWrapperDoesNotBurnButHotPieceDoes(GameTestHelper helper) {
        FakePlayer player = Modders.fresh(helper, Modders.SMITH);
        ItemStack item = piece(20);
        player.inventory.setInventorySlotContents(5, item);
        float health = player.getHealth();
        assertFalse(PlayerTickHandlerMF.burnWithHotItems(player));
        assertFalse(player.isBurning());
        assertEquals(health, player.getHealth(), 0F);
        ItemHeated.setTemp(item, 101);
        assertTrue(PlayerTickHandlerMF.burnWithHotItems(player));
        assertTrue(player.isBurning());
        helper.succeed();
    }

    @GameTest
    public static void coolingRestoresInventoryDropsAndTongsWithoutQuenching(GameTestHelper helper) {
        helper.succeedWhen(() -> {
            if (helper.getWorld().getTotalWorldTime() % 20 != 0) return false;
            AmbientTemperature.setProvider((world, x, y, z) -> 20F);
            try {
                TestPos at = helper.absolute(1, 1, 1);
                helper.setBlock(1, 1, 1, Blocks.cauldron);
                helper.getWorld().setBlockMetadataWithNotify(at.x(), at.y(), at.z(), 3, 2);
                FakePlayer player = Modders.fresh(helper, Modders.SMITH);
                player.setPositionAndRotation(at.x() + .5, at.y() + 2, at.z() + .5, 0F, 90F);
                ItemStack inventory = piece(101);
                player.inventory.setInventorySlotContents(5, inventory);
                inventory.getItem().onUpdate(inventory, helper.getWorld(), player, 5, false);
                assertSword(player.inventory.getStackInSlot(5));
                assertFalse(PlayerTickHandlerMF.burnWithHotItems(player));
                ItemStack ingots = ItemHeated.createHotItem(new ItemStack(Items.iron_ingot, 7), 101);
                ingots.stackSize = 3;
                EntityItem drop = new EntityItem(helper.getWorld(), at.x(), at.y() + 2, at.z(), ingots);
                ingots.getItem().onEntityItemUpdate(drop);
                assertEquals(Items.iron_ingot, drop.getEntityItem().getItem());
                assertEquals(3, drop.getEntityItem().stackSize);
                ItemStack tongs = new ItemStack(ToolListMF.tongsStone);
                assertTrue(TongsHelper.trySetHeldItem(tongs, piece(101)));
                player.setCurrentItemOrArmor(0, tongs);
                tongs.getItem().onUpdate(tongs, helper.getWorld(), player, 0, true);
                assertSword(TongsHelper.getHeldItem(tongs));
                net.minecraft.util.AxisAlignedBB box = net.minecraft.util.AxisAlignedBB
                        .getBoundingBox(at.x() - 1, at.y(), at.z() - 1, at.x() + 2, at.y() + 4, at.z() + 2);
                int before = helper.getWorld().getEntitiesWithinAABB(EntityItem.class, box).size();
                tongs.getItem().onItemRightClick(tongs, helper.getWorld(), player);
                assertNull(TongsHelper.getHeldItem(tongs));
                java.util.List<EntityItem> drops = helper.getWorld().getEntitiesWithinAABB(EntityItem.class, box);
                assertEquals(before + 1, drops.size());
                assertSword(drops.get(drops.size() - 1).getEntityItem());
                tongs.getItem().onItemRightClick(tongs, helper.getWorld(), player);
                assertEquals(before + 1, helper.getWorld().getEntitiesWithinAABB(EntityItem.class, box).size());
                assertEquals(
                        "cold release consumed water",
                        3,
                        helper.getWorld().getBlockMetadata(at.x(), at.y(), at.z()));
                ItemStack legacy = piece(20);
                player.setCurrentItemOrArmor(0, legacy);
                legacy.getItem().onItemRightClick(legacy, helper.getWorld(), player);
                assertSword(legacy);
                assertEquals(3, helper.getWorld().getBlockMetadata(at.x(), at.y(), at.z()));
            } finally {
                AmbientTemperature.setProvider(null);
            }
            return true;
        });
    }
}
