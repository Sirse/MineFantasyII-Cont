package minefantasy.mf2.mechanics;

import static minefantasy.mf2.gametest.Assert.*;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.heating.Heatable;
import minefantasy.mf2.api.heating.TongsHelper;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.tileentity.decor.TileEntityTrough;
import minefantasy.mf2.fluid.FluidsMF;
import minefantasy.mf2.gametest.TestItems;
import minefantasy.mf2.item.list.CustomToolListMF;
import minefantasy.mf2.item.list.ToolListMF;
import minefantasy.mf2.item.weapon.ItemWeaponMF;

/** Oil flares under a piece past its unstable heat: the smith is scalded and set alight, and no block catches fire. */
@GameTestHolder("minefantasy2")
public class QuenchFlareTest {

    private QuenchFlareTest() {}

    /** A hot steel sword, at the share of its working heat given: above 1 overheated. */
    private static ItemStack hotSword(float heat) {
        ItemStack sword = ((ItemWeaponMF) CustomToolListMF.standard_sword).construct("Steel", "OakWood");
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag(Heatable.NBT_Item, sword.writeToNBT(new NBTTagCompound()));
        tag.setInteger(Heatable.NBT_WorkableTemp, 1000);
        tag.setInteger(Heatable.NBT_UnstableTemp, 2000);
        tag.setInteger(Heatable.NBT_CurrentTemp, (int) (1000 + 1000 * heat));
        ItemStack hot = new ItemStack(TestItems.hot);
        hot.setTagCompound(tag);
        return hot;
    }

    /** A trough of the fluid at (1, 1, 1), and a smith above it looking down, holding tongs with the piece. */
    private static EntityPlayer quench(GameTestHelper helper, net.minecraftforge.fluids.Fluid fluid, ItemStack hot) {
        helper.setBlock(1, 1, 1, BlockListMF.trough_wood);
        // Something that burns beside the air above the bath: still no fire is set
        helper.setBlock(2, 2, 1, Blocks.planks);
        TileEntityTrough trough = helper.assertTileEntityPresent(TileEntityTrough.class, 1, 1, 1);
        trough.fill(ForgeDirection.UNKNOWN, new FluidStack(fluid, 1000), true);
        TestPos at = helper.absolute(1, 1, 1);
        EntityPlayer smith = vulnerable(ProtectionFixtures.named(helper, "Smith"));
        smith.setPositionAndRotation(at.x() + 0.5, at.y() + 2, at.z() + 0.5, 0F, 90F);
        ItemStack tongs = new ItemStack(ToolListMF.tongsStone);
        smith.setCurrentItemOrArmor(0, tongs);
        assertTrue(TongsHelper.trySetHeldItem(tongs, hot));
        tongs.getItem().onItemRightClick(tongs, helper.getWorld(), smith);
        assertNull("the piece was not quenched", TongsHelper.getHeldItem(tongs));
        return smith;
    }

    private static boolean fireAbove(GameTestHelper helper) {
        TestPos at = helper.absolute(1, 2, 1);
        return helper.getWorld().getBlock(at.x(), at.y(), at.z()) == Blocks.fire;
    }

    @GameTest
    public static void anOverheatedPieceScaldsTheSmithAndSetsNoFire(GameTestHelper helper) {
        EntityPlayer smith = quench(helper, FluidsMF.seedOil, hotSword(1.5F));
        assertTrue("the smith did not catch fire", smith.isBurning());
        assertTrue("the burst did not scald", smith.getHealth() < smith.getMaxHealth());
        assertFalse("a block caught fire", fireAbove(helper));
        helper.succeed();
    }

    @GameTest
    public static void aPieceAtWorkingHeatDoesNot(GameTestHelper helper) {
        EntityPlayer smith = quench(helper, FluidsMF.seedOil, hotSword(0.5F));
        assertFalse(smith.isBurning());
        assertEquals(smith.getMaxHealth(), smith.getHealth(), 0F);
        helper.succeed();
    }

    @GameTest
    public static void waterNeverFlares(GameTestHelper helper) {
        EntityPlayer smith = quench(helper, FluidRegistry.WATER, hotSword(1.5F));
        assertFalse(smith.isBurning());
        assertEquals(smith.getMaxHealth(), smith.getHealth(), 0F);
        helper.succeed();
    }

    /** A blacksmith's apron takes most of the scald and shortens the burning. */
    @GameTest
    public static void anApronLessensTheScald(GameTestHelper helper) {
        EntityPlayer bare = vulnerable(ProtectionFixtures.named(helper, "Smith"));
        EntityPlayer aproned = vulnerable(ProtectionFixtures.named(helper, "Smith"));
        aproned.inventory.armorInventory[2] = new ItemStack(minefantasy.mf2.item.list.ArmourListMF.leatherapron);
        QuenchFlare.flare(helper.getWorld(), 0, 0, 0, bare);
        QuenchFlare.flare(helper.getWorld(), 0, 0, 0, aproned);
        float bareLoss = bare.getMaxHealth() - bare.getHealth();
        float apronLoss = aproned.getMaxHealth() - aproned.getHealth();
        assertTrue("the bare smith was not scalded", bareLoss > 0F);
        assertTrue("the apron took no scald: " + apronLoss + " of " + bareLoss, apronLoss < bareLoss);
        assertTrue("the apron took all of it", apronLoss > 0F);
        assertTrue("the apron put out the burning", aproned.isBurning());
        int bareFire = fire(bare);
        assertTrue("the apron did not shorten the burning", fire(aproned) < bareFire);
        helper.succeed();
    }

    /** A creative smith is neither scalded nor set alight. */
    @GameTest
    public static void aCreativeSmithIsSpared(GameTestHelper helper) {
        EntityPlayer smith = vulnerable(ProtectionFixtures.named(helper, "Smith"));
        smith.capabilities.isCreativeMode = true;
        QuenchFlare.flare(helper.getWorld(), 0, 0, 0, smith);
        assertFalse(smith.isBurning());
        assertEquals(smith.getMaxHealth(), smith.getHealth(), 0F);
        helper.succeed();
    }

    /** The ticks the entity still burns. */
    private static int fire(EntityPlayer player) {
        return cpw.mods.fml.relauncher.ReflectionHelper
                .getPrivateValue(net.minecraft.entity.Entity.class, player, "fire", "field_70151_c");
    }

    /** A new server player is invulnerable for a few seconds, as after respawning. */
    private static EntityPlayer vulnerable(net.minecraft.entity.player.EntityPlayerMP player) {
        cpw.mods.fml.relauncher.ReflectionHelper
                .setPrivateValue(net.minecraft.entity.player.EntityPlayerMP.class, player, 0, "field_147101_bU");
        return player;
    }
}
