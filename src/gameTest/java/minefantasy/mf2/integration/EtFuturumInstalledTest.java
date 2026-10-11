package minefantasy.mf2.integration;

import static minefantasy.mf2.gametest.Assert.*;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.api.CompostingRegistry;
import ganymedes01.etfuturum.recipes.BlastFurnaceRecipes;
import ganymedes01.etfuturum.spectator.SpectatorMode;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.config.ConfigIntegration;
import minefantasy.mf2.integration.etfuturum.EtFuturumCompat;
import minefantasy.mf2.item.food.FoodListMF;
import minefantasy.mf2.item.list.ComponentListMF;

/**
 * With Et Futurum Requiem installed, the bridge fills its registries: berries compost, the working stations are hidden
 * from spectators, ore refining stays out of the blast furnace, and Et Futurum's raw ores refine into MineFantasy bars
 * in the big furnace. Skipped in runs without the mod, like the EnviroMine tests.
 */
@GameTestHolder(value = "minefantasy2", requiredMods = "etfuturum")
public class EtFuturumInstalledTest {

    private EtFuturumInstalledTest() {}

    @GameTest
    public static void theBridgeRegistersContent(GameTestHelper helper) {
        boolean integration = ConfigIntegration.etfuturumIntegration;
        boolean blacklist = ConfigIntegration.etfuturumBlastBlacklist;
        try {
            ConfigIntegration.etfuturumIntegration = true;
            ConfigIntegration.etfuturumBlastBlacklist = true;
            EtFuturumCompat.load();

            assertTrue("berries do not compost", CompostingRegistry.isCompostable(new ItemStack(FoodListMF.berries)));
            assertTrue(
                    "the berries compost at the wrong share",
                    CompostingRegistry.getCompostChance(new ItemStack(FoodListMF.berries)) == 30);
            assertTrue(
                    "the forge is not hidden from spectators",
                    SpectatorMode.SPECTATOR_INTERACT_BLACKLIST.contains(BlockListMF.forge));
            assertTrue(
                    "the lit forge is not hidden from spectators",
                    SpectatorMode.SPECTATOR_INTERACT_BLACKLIST.contains(BlockListMF.forge_active));
            assertTrue(
                    "raw copper still refines in the blast furnace",
                    BlastFurnaceRecipes.smelting().smeltingBlacklist
                            .contains(new ItemStack(ComponentListMF.oreCopper, 1, OreDictionary.WILDCARD_VALUE)));
            assertTrue(
                    "deepslate copper still refines in the blast furnace",
                    BlastFurnaceRecipes.smelting().smeltingBlacklist
                            .contains(new ItemStack(ModBlocks.DEEPSLATE_COPPER_ORE.get())));
            assertTrue(
                    "raw copper does not refine in the big furnace",
                    refinesToBar(ModItems.RAW_ORE.newItemStack(1, 0), "Copper"));
            assertTrue(
                    "raw iron does not refine in the big furnace",
                    refinesToBar(ModItems.RAW_ORE.newItemStack(1, 1), "Iron"));
        } finally {
            ConfigIntegration.etfuturumIntegration = integration;
            ConfigIntegration.etfuturumBlastBlacklist = blacklist;
        }
        helper.succeed();
    }

    /** Whether the big furnace refines the stack into the named metal's bar. */
    private static boolean refinesToBar(ItemStack raw, String metal) {
        return MFRecipes.find(MFRecipes.BIG_FURNACE, raw) != null
                && MFRecipes.find(MFRecipes.BIG_FURNACE, raw).getRecipe().getOutput().getItem()
                        == ComponentListMF.bar(metal).getItem();
    }
}
