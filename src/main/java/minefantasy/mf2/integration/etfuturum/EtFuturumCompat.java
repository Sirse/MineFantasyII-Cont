package minefantasy.mf2.integration.etfuturum;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;
import net.minecraftforge.oredict.OreDictionary;

import cpw.mods.fml.common.Loader;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.api.BeePlantRegistry;
import ganymedes01.etfuturum.api.CompostingRegistry;
import ganymedes01.etfuturum.api.DeepslateOreRegistry;
import ganymedes01.etfuturum.api.mappings.RegistryMapping;
import ganymedes01.etfuturum.core.utils.ItemStackSet;
import ganymedes01.etfuturum.recipes.BlastFurnaceRecipes;
import ganymedes01.etfuturum.spectator.SpectatorMode;
import minefantasy.mf2.api.MineFantasyAPI;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.config.ConfigIntegration;
import minefantasy.mf2.item.food.FoodListMF;
import minefantasy.mf2.item.list.ComponentListMF;
import minefantasy.mf2.mechanics.worldGen.structure.LootTypes;
import minefantasy.mf2.util.MFLogUtil;

/**
 * One-way bridge into Et Futurum Requiem, run once at load completion when every mod has registered its content:
 * MineFantasy goes into Et Futurum's registries, and nothing is read back. Berries and woodland trimmings compost; bees
 * visit the berry bushes; the working stations are hidden from spectators (Et Futurum's own precedent for machinery,
 * like the Tinkers' smeltery — storage such as racks and boxes stays open to look into); ore refining stays out of Et
 * Futurum's blast furnace, so metals keep to the crucible and the bloom, while Et Futurum's raw ores gain their own
 * path into the big furnace as MineFantasy bars — its copper feeds the bronze line. Food, clay and cracked blocks keep
 * smelting there, where double speed is a courtesy rather than a bypass.
 */
public final class EtFuturumCompat {

    private EtFuturumCompat() {}

    public static void load() {
        if (!ConfigIntegration.etfuturumIntegration || !Loader.isModLoaded("etfuturum")) {
            return;
        }
        try {
            // Every registration here survives a repeat, for the tests that call load() again: the registries are
            // maps and sets, the bee flowers are replaced rather than duplicated, the recipes are looked up before
            // they are added, and the chest pool keeps its own once-guard, since ChestGenHooks cannot be asked what
            // it already holds.
            registerCompostables();
            hideStationsFromSpectators();
            if (ConfigIntegration.etfuturumBlastBlacklist) {
                keepRefiningOutOfTheBlastFurnace();
            }
            registerBeeFlowers();
            refineRawOresInTheBigFurnace();
            stockDwarvenForgesWithCopper();
        } catch (RuntimeException | LinkageError e) {
            MFLogUtil.warnOnce(
                    "etfuturum-bridge",
                    "Et Futurum Requiem bridge failed; some registrations may be missing: {}",
                    e.toString());
        }
    }

    private static void registerCompostables() {
        List<Object> compost = new ArrayList<Object>();
        compost.add(new ItemStack(FoodListMF.berries));
        compost.add(new ItemStack(FoodListMF.berriesJuicy));
        wild(compost, BlockListMF.sapling_yew);
        wild(compost, BlockListMF.sapling_ironbark);
        wild(compost, BlockListMF.sapling_ebony);
        wild(compost, BlockListMF.leaves_yew);
        wild(compost, BlockListMF.leaves_ironbark);
        wild(compost, BlockListMF.leaves_ebony);
        CompostingRegistry.registerCompostable(compost, 30);
    }

    private static void registerBeeFlowers() {
        for (int meta = 0; meta < 4; meta++) {
            // Replacing rather than adding keeps a repeated load() from listing the same bush twice
            BeePlantRegistry.removeFlower(BlockListMF.berryBush, meta);
            BeePlantRegistry.addFlower(BlockListMF.berryBush, meta);
        }
    }

    private static void hideStationsFromSpectators() {
        hide(
                BlockListMF.forge,
                BlockListMF.forge_active,
                BlockListMF.forge_metal,
                BlockListMF.forge_metal_active,
                BlockListMF.crucible,
                BlockListMF.crucible_active,
                BlockListMF.crucibleadv,
                BlockListMF.crucibleadv_active,
                BlockListMF.crucibleauto,
                BlockListMF.crucibleauto_active,
                BlockListMF.cruciblemythic,
                BlockListMF.cruciblemythic_active,
                BlockListMF.blast_heater,
                BlockListMF.blast_heater_active,
                BlockListMF.blast_chamber,
                BlockListMF.furnace_heater,
                BlockListMF.furnace_stone,
                BlockListMF.anvilStone,
                BlockListMF.carpenter,
                BlockListMF.kitchenBench,
                BlockListMF.tanner,
                BlockListMF.advTanner,
                BlockListMF.research,
                BlockListMF.bombPress,
                BlockListMF.firepit,
                BlockListMF.roast,
                BlockListMF.oven_stone,
                BlockListMF.bloomery,
                BlockListMF.quern);
        hide(BlockListMF.anvil);
    }

    private static void hide(Block... blocks) {
        for (Block block : blocks) {
            if (block != null) {
                SpectatorMode.SPECTATOR_INTERACT_BLACKLIST.add(block);
            }
        }
    }

    private static void keepRefiningOutOfTheBlastFurnace() {
        ItemStackSet blacklist = BlastFurnaceRecipes.smelting().smeltingBlacklist;
        blacklist.add(new ItemStack(ComponentListMF.oreCopper, 1, OreDictionary.WILDCARD_VALUE));
        blacklist.add(new ItemStack(ComponentListMF.oreTin, 1, OreDictionary.WILDCARD_VALUE));
        blacklist.add(new ItemStack(ComponentListMF.oreIron, 1, OreDictionary.WILDCARD_VALUE));
        blacklist.add(new ItemStack(ComponentListMF.oreSilver, 1, OreDictionary.WILDCARD_VALUE));
        blacklist.add(new ItemStack(ComponentListMF.oreGold, 1, OreDictionary.WILDCARD_VALUE));
        blacklist.add(new ItemStack(BlockListMF.oreCopper, 1, OreDictionary.WILDCARD_VALUE));
        blacklist.add(new ItemStack(BlockListMF.oreTin, 1, OreDictionary.WILDCARD_VALUE));
        blacklist.add(new ItemStack(BlockListMF.oreSilver, 1, OreDictionary.WILDCARD_VALUE));
        blacklist.add(new ItemStack(ComponentListMF.coal_prep, 1, OreDictionary.WILDCARD_VALUE));
        // The deepslate twins of those ore blocks, as Et Futurum's worldgen maps them: same ore, same ban
        deepslateOutOfTheBlastFurnace(blacklist, BlockListMF.oreCopper);
        deepslateOutOfTheBlastFurnace(blacklist, BlockListMF.oreTin);
        deepslateOutOfTheBlastFurnace(blacklist, BlockListMF.oreSilver);
    }

    /**
     * Et Futurum's deepslate blobs convert ore blocks they swallow into deepslate variants, and the vanilla smelting of
     * the plain ore is copied onto them — so the twins of the banned blocks get the ban too. Nothing is blacklisted
     * when no variant was mapped, which is the case with Et Futurum's modded deepslate support off.
     */
    private static void deepslateOutOfTheBlastFurnace(ItemStackSet blacklist, Block ore) {
        RegistryMapping<Block> mapping = DeepslateOreRegistry.getOre(ore, 0);
        if (mapping != null && mapping.getObject() != null) {
            blacklist.add(new ItemStack(mapping.getObject(), 1, mapping.getMeta()));
        }
    }

    /**
     * Et Futurum's raw ores refine in the big furnace into MineFantasy bars, the same output this mod's own raw ores
     * give: its copper then feeds the bronze line, and its tin and silver (through the modded raw ores, when that
     * system is on) feed the tin and silver bars. Their vanilla smelting is left alone, so nothing is taken away.
     */
    private static void refineRawOresInTheBigFurnace() {
        bigFurnace(ModItems.RAW_ORE, 0, "Copper");
        bigFurnace(ModItems.RAW_ORE, 1, "Iron");
        bigFurnace(ModItems.RAW_ORE, 2, "Gold");
        bigFurnace(ModItems.MODDED_RAW_ORE, 1, "Tin");
        bigFurnace(ModItems.MODDED_RAW_ORE, 2, "Silver");
    }

    private static void bigFurnace(ModItems item, int meta, String metal) {
        if (item == null || !item.isEnabled()) {
            return;
        }
        ItemStack raw = item.newItemStack(1, meta);
        if (!isValid(raw)) {
            return;
        }
        ItemStack bar = ComponentListMF.bar(metal);
        if (MFRecipes.find(MFRecipes.BIG_FURNACE, raw) != null) {
            return;
        }
        MineFantasyAPI.bigFurnaceRecipe(raw, bar).register();
    }

    /**
     * ChestGenHooks cannot be asked what a pool already holds: without this, a repeated load() would double the copper
     * weights.
     */
    private static boolean lootStocked;

    /** Dwarven forges stock Et Futurum's copper too: the ingot and raw pieces of it. */
    private static void stockDwarvenForgesWithCopper() {
        if (lootStocked || !ModItems.RAW_ORE.isEnabled() || !ModItems.COPPER_INGOT.isEnabled()) {
            return;
        }
        ChestGenHooks.addItem(
                LootTypes.DWARVEN_FORGE,
                new WeightedRandomChestContent(ModItems.COPPER_INGOT.newItemStack(), 1, 4, 50));
        ChestGenHooks.addItem(
                LootTypes.DWARVEN_FORGE,
                new WeightedRandomChestContent(ModItems.RAW_ORE.newItemStack(), 1, 3, 40));
        lootStocked = true;
    }

    private static boolean isValid(ItemStack stack) {
        return stack != null && stack.getItem() != null;
    }

    private static void wild(List<Object> compost, Block block) {
        if (block != null) {
            compost.add(new ItemStack(block, 1, OreDictionary.WILDCARD_VALUE));
        }
    }
}
