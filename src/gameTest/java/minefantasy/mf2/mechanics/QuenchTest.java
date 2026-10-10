package minefantasy.mf2.mechanics;

import static minefantasy.mf2.gametest.Assert.*;

import java.util.Random;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.heating.Heatable;
import minefantasy.mf2.api.heating.Quench;
import minefantasy.mf2.api.heating.QuenchMedium;
import minefantasy.mf2.api.heating.TongsHelper;
import minefantasy.mf2.api.helpers.ItemQuality;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.tileentity.decor.TileEntityTrough;
import minefantasy.mf2.fluid.FluidsMF;
import minefantasy.mf2.gametest.TestItems;
import minefantasy.mf2.item.list.CustomToolListMF;
import minefantasy.mf2.item.weapon.ItemWeaponMF;

/**
 * Quenching hardens steel by the medium and the heat it is quenched from, cracks it sometimes, leaves bronze as it was,
 * and gives the mythic metals their characters.
 */
@GameTestHolder("minefantasy2")
public class QuenchTest {

    private QuenchTest() {}

    private static final Quench.Source OIL = new Quench.Source(QuenchMedium.OIL, 1F);
    private static final Quench.Source WATER = new Quench.Source(QuenchMedium.WATER, 1F);
    private static final Quench.Source BRINE = new Quench.Source(QuenchMedium.BRINE, 1F);

    /** A random that always rolls the value given. */
    private static Random rolling(final float value) {
        return new Random() {

            @Override
            public float nextFloat() {
                return value;
            }
        };
    }

    private static final Random NEVER_CRACKS = rolling(0.999F);
    private static final Random ALWAYS_CRACKS = rolling(0F);

    /** A hot sword of the metal, at the share of its working heat given: below 0 cold, above 1 overheated. */
    private static ItemStack hotSword(String metal, float heat) {
        ItemStack sword = ((ItemWeaponMF) CustomToolListMF.standard_sword).construct(metal, "OakWood");
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag(Heatable.NBT_Item, sword.writeToNBT(new NBTTagCompound()));
        tag.setInteger(Heatable.NBT_WorkableTemp, 1000);
        tag.setInteger(Heatable.NBT_UnstableTemp, 2000);
        tag.setInteger(Heatable.NBT_CurrentTemp, (int) (1000 + 1000 * heat));
        ItemStack hot = new ItemStack(TestItems.hot);
        hot.setTagCompound(tag);
        return hot;
    }

    private static <T> T withRuin(boolean ruin, java.util.function.Supplier<T> body) {
        boolean was = Heatable.HCCquenchRuin;
        Heatable.HCCquenchRuin = ruin;
        try {
            return body.get();
        } finally {
            Heatable.HCCquenchRuin = was;
        }
    }

    private static float quality(ItemStack item) {
        return ItemQuality.get(item);
    }

    @GameTest
    public static void steelHardensMoreTheHarsherTheMedium(GameTestHelper helper) {
        float oil = quality(Quench.cool(hotSword("Steel", 0.5F), OIL, null, NEVER_CRACKS));
        float water = quality(Quench.cool(hotSword("Steel", 0.5F), WATER, null, NEVER_CRACKS));
        float brine = quality(Quench.cool(hotSword("Steel", 0.5F), BRINE, null, NEVER_CRACKS));
        assertEquals(ItemQuality.ORDINARY + 8, oil, 0.01F);
        assertEquals(ItemQuality.ORDINARY + 15, water, 0.01F);
        assertEquals(ItemQuality.ORDINARY + 22, brine, 0.01F);
        ItemStack cooled = Quench.cool(hotSword("Steel", 0.5F), BRINE, null, NEVER_CRACKS);
        assertEquals("brine", cooled.getTagCompound().getString(Quench.NBT_MEDIUM));
        assertFalse(cooled.getTagCompound().getBoolean(Quench.NBT_CRACKED));
        helper.succeed();
    }

    @GameTest
    public static void aColdPieceDoesNotHardenAndAnOverheatedOneHalfDoes(GameTestHelper helper) {
        assertEquals(
                "a cold piece hardened",
                ItemQuality.ORDINARY,
                quality(Quench.cool(hotSword("Steel", -0.2F), WATER, null, NEVER_CRACKS)),
                0.01F);
        assertEquals(
                ItemQuality.ORDINARY + 7.5F,
                quality(Quench.cool(hotSword("Steel", 1.5F), WATER, null, NEVER_CRACKS)),
                0.01F);
        helper.succeed();
    }

    /** Water cracks steel 8% of the time: a roll under it cracks, over it does not; overheating triples it. */
    @GameTest
    public static void aCrackMakesThePieceInferiorAndWearsIt(GameTestHelper helper) {
        withRuin(true, () -> {
            ItemStack cracked = Quench.cool(hotSword("Steel", 0.5F), WATER, null, rolling(0.07F));
            assertTrue(
                    "a roll under the chance did not crack",
                    cracked.getTagCompound().getBoolean(Quench.NBT_CRACKED));
            assertSame(ItemQuality.Grade.INFERIOR, ItemQuality.getGrade(cracked));
            assertEquals((int) (cracked.getMaxDamage() * Quench.CRACK_WEAR), cracked.getItemDamage());
            assertEquals("a crack added quality", ItemQuality.ORDINARY, quality(cracked), 0.01F);

            assertFalse(
                    "a roll over the chance cracked",
                    Quench.cool(hotSword("Steel", 0.5F), WATER, null, rolling(0.09F)).getTagCompound()
                            .getBoolean(Quench.NBT_CRACKED));
            assertTrue(
                    "overheating did not raise the risk",
                    Quench.cool(hotSword("Steel", 1.5F), WATER, null, rolling(0.2F)).getTagCompound()
                            .getBoolean(Quench.NBT_CRACKED));
            assertTrue(
                    "open water is no riskier",
                    Quench.cool(
                            hotSword("Steel", 0.5F),
                            new Quench.Source(QuenchMedium.WATER, 1.5F),
                            null,
                            rolling(0.1F)).getTagCompound().getBoolean(Quench.NBT_CRACKED));
            return null;
        });
        helper.succeed();
    }

    @GameTest
    public static void nothingCracksWithTheHardcoreRuleOff(GameTestHelper helper) {
        withRuin(false, () -> {
            ItemStack cooled = Quench.cool(hotSword("PigIron", 1.5F), BRINE, null, ALWAYS_CRACKS);
            assertFalse(cooled.getTagCompound().getBoolean(Quench.NBT_CRACKED));
            assertEquals(0, cooled.getItemDamage());
            return null;
        });
        helper.succeed();
    }

    @GameTest
    public static void bronzeJustCools(GameTestHelper helper) {
        withRuin(true, () -> {
            ItemStack cooled = Quench.cool(hotSword("Bronze", 1.5F), BRINE, null, ALWAYS_CRACKS);
            assertEquals(ItemQuality.ORDINARY, quality(cooled), 0.01F);
            assertEquals(0, cooled.getItemDamage());
            assertFalse("bronze was marked as quenched", cooled.getTagCompound().hasKey(Quench.NBT_MEDIUM));
            return null;
        });
        helper.succeed();
    }

    @GameTest
    public static void pigIronOnlyCracks(GameTestHelper helper) {
        withRuin(true, () -> {
            ItemStack cooled = Quench.cool(hotSword("PigIron", 0.5F), WATER, null, NEVER_CRACKS);
            assertEquals("pig iron hardened", ItemQuality.ORDINARY, quality(cooled), 0.01F);
            assertTrue(
                    "pig iron did not crack at 35%",
                    Quench.cool(hotSword("PigIron", 0.5F), WATER, null, rolling(0.3F)).getTagCompound()
                            .getBoolean(Quench.NBT_CRACKED));
            return null;
        });
        helper.succeed();
    }

    @GameTest
    public static void adamantiumTakesOnlyOil(GameTestHelper helper) {
        withRuin(true, () -> {
            assertEquals(
                    ItemQuality.ORDINARY + 25,
                    quality(Quench.cool(hotSword("Adamantium", 0.5F), OIL, null, NEVER_CRACKS)),
                    0.01F);
            assertTrue(
                    "adamantium survived water",
                    Quench.cool(hotSword("Adamantium", 0.5F), WATER, null, NEVER_CRACKS).getTagCompound()
                            .getBoolean(Quench.NBT_CRACKED));
            return null;
        });
        helper.succeed();
    }

    @GameTest
    public static void mithrilNeedsNoQuench(GameTestHelper helper) {
        withRuin(true, () -> {
            ItemStack cooled = Quench.cool(hotSword("Mithril", 1.5F), BRINE, null, ALWAYS_CRACKS);
            assertEquals(ItemQuality.ORDINARY, quality(cooled), 0.01F);
            assertFalse(cooled.getTagCompound().hasKey(Quench.NBT_MEDIUM));
            return null;
        });
        helper.succeed();
    }

    @GameTest
    public static void ignotumiteCracksOutsideTheMiddleOfItsHeat(GameTestHelper helper) {
        withRuin(true, () -> {
            assertEquals(
                    ItemQuality.ORDINARY + 42,
                    quality(Quench.cool(hotSword("Ignotumite", 0.5F), BRINE, null, NEVER_CRACKS)),
                    0.01F);
            assertTrue(
                    "ignotumite survived a quench off the middle",
                    Quench.cool(hotSword("Ignotumite", 0.2F), OIL, null, NEVER_CRACKS).getTagCompound()
                            .getBoolean(Quench.NBT_CRACKED));
            return null;
        });
        helper.succeed();
    }

    /** A trough of oil quenches in oil and loses a unit of it. */
    @GameTest
    public static void aTroughOfOilQuenchesInOil(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, BlockListMF.trough_wood);
        TileEntityTrough trough = helper.assertTileEntityPresent(TileEntityTrough.class, 1, 1, 1);
        trough.fill(ForgeDirection.UNKNOWN, new FluidStack(FluidsMF.seedOil, 250), true);
        com.gtnewhorizons.horizonqa.api.TestPos at = helper.absolute(1, 1, 1);
        Quench.Source source = TongsHelper.findQuench(helper.getWorld(), at.x(), at.y(), at.z());
        assertNotNull(source);
        assertSame(QuenchMedium.OIL, source.medium);
        assertEquals(3, trough.fill);
        assertSame("the oil turned to something else", FluidsMF.seedOil, trough.getFluid());
        helper.succeed();
    }

    /** A trough of a fluid taken out of quenching does not quench, and keeps its fluid. */
    @GameTest
    public static void aTroughOfAFluidThatNoLongerQuenchesKeepsIt(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, BlockListMF.trough_wood);
        TileEntityTrough trough = helper.assertTileEntityPresent(TileEntityTrough.class, 1, 1, 1);
        trough.fill(ForgeDirection.UNKNOWN, new FluidStack(FluidsMF.seedOil, 250), true);
        com.gtnewhorizons.horizonqa.api.TestPos at = helper.absolute(1, 1, 1);
        QuenchMedium was = QuenchMedium.registered(FluidsMF.seedOil.getName());
        QuenchMedium.register(FluidsMF.seedOil.getName(), null);
        try {
            assertNull(TongsHelper.findQuench(helper.getWorld(), at.x(), at.y(), at.z()));
        } finally {
            QuenchMedium.register(FluidsMF.seedOil.getName(), was);
        }
        assertEquals(4, trough.fill);
        helper.succeed();
    }

    /** A metal set to lose quality in a quench loses it, down to the least there is. */
    @GameTest
    public static void aNegativeBonusLowersTheQuality(GameTestHelper helper) {
        minefantasy.mf2.api.heating.QuenchResponse was = minefantasy.mf2.api.heating.QuenchResponse.registered("Steel");
        try {
            minefantasy.mf2.api.heating.QuenchResponse
                    .register("Steel", minefantasy.mf2.api.heating.QuenchResponse.of(-20, 0, -20, 0, -500, 0));
            assertEquals(
                    ItemQuality.ORDINARY - 20,
                    quality(Quench.cool(hotSword("Steel", 0.5F), WATER, null, NEVER_CRACKS)),
                    0.01F);
            assertEquals(
                    ItemQuality.MIN,
                    quality(Quench.cool(hotSword("Steel", 0.5F), BRINE, null, NEVER_CRACKS)),
                    0.01F);
        } finally {
            minefantasy.mf2.api.heating.QuenchResponse.register("Steel", was);
        }
        helper.succeed();
    }
}
