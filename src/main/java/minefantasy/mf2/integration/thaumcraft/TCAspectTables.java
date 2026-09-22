package minefantasy.mf2.integration.thaumcraft;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.material.CustomMaterial;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.item.food.FoodListMF;
import minefantasy.mf2.item.list.ComponentListMF;
import minefantasy.mf2.material.BaseMaterialMF;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * What MF resources are worth in aspects. Data only.
 * <p>
 * Values are set against the recipe, not the name, because Thaumcraft pays per item and MF recipes make batches: over
 * eight per craft is left out, five to eight gets one point, two to four at most two points. An integer aspect cannot
 * go below one, so a batch always gains a little.
 */
public class TCAspectTables {

    public static List<TCAspectEntry> buildItemTable() {
        List<TCAspectEntry> table = new ArrayList<TCAspectEntry>();
        addFuel(table);
        addHides(table);
        addAlchemy(table);
        addOreChunks(table);
        addStoneAndClay(table);
        addMetalParts(table);
        addIngots(table);
        addFood(table);
        addPlants(table);
        return table;
    }

    private static void addFuel(List<TCAspectEntry> table) {
        add(table, ComponentListMF.coke, new AspectList().add(Aspect.FIRE, 3).add(Aspect.ENERGY, 2));
        add(table, ComponentListMF.coalDust, new AspectList().add(Aspect.FIRE, 1).add(Aspect.ENTROPY, 1));
        add(table, ComponentListMF.coal_prep, new AspectList().add(Aspect.FIRE, 2));
        add(table, ComponentListMF.coal_flux, new AspectList().add(Aspect.FIRE, 2).add(Aspect.ORDER, 1));
    }

    private static void addHides(List<TCAspectEntry> table) {
        add(table, ComponentListMF.rawhideSmall, new AspectList().add(Aspect.BEAST, 1).add(Aspect.FLESH, 1));
        add(table, ComponentListMF.rawhideMedium, new AspectList().add(Aspect.BEAST, 2).add(Aspect.FLESH, 1));
        add(table, ComponentListMF.rawhideLarge, new AspectList().add(Aspect.BEAST, 3).add(Aspect.FLESH, 2));
        add(table, ComponentListMF.hideSmall, new AspectList().add(Aspect.BEAST, 1).add(Aspect.CLOTH, 1));
        add(table, ComponentListMF.hideMedium, new AspectList().add(Aspect.BEAST, 2).add(Aspect.CLOTH, 1));
        add(table, ComponentListMF.hideLarge, new AspectList().add(Aspect.BEAST, 3).add(Aspect.CLOTH, 2));
        // Four and three to a craft.
        add(table, ComponentListMF.leather_strip, new AspectList().add(Aspect.CLOTH, 1));
        add(table, ComponentListMF.thread, new AspectList().add(Aspect.CLOTH, 1));
    }

    private static void addAlchemy(List<TCAspectEntry> table) {
        // Nitre, sulfur and oil are made four at a time.
        add(table, ComponentListMF.nitre, new AspectList().add(Aspect.ENERGY, 1));
        add(table, ComponentListMF.sulfur, new AspectList().add(Aspect.FIRE, 1));
        add(table, ComponentListMF.plant_oil, new AspectList().add(Aspect.PLANT, 1));
        add(table, ComponentListMF.flux, new AspectList().add(Aspect.ORDER, 1));
        add(table, ComponentListMF.flux_strong, new AspectList().add(Aspect.ORDER, 2));
        add(table, ComponentListMF.blackpowder, new AspectList().add(Aspect.FIRE, 2).add(Aspect.ENERGY, 2));
        add(table, ComponentListMF.magma_cream_refined, new AspectList().add(Aspect.FIRE, 3).add(Aspect.SLIME, 2));
        add(table, ComponentListMF.shrapnel, new AspectList().add(Aspect.METAL, 1).add(Aspect.ENTROPY, 1));
    }

    private static void addOreChunks(List<TCAspectEntry> table) {
        add(table, ComponentListMF.oreCopper, new AspectList().add(Aspect.METAL, 1).add(Aspect.EARTH, 1));
        add(table, ComponentListMF.oreTin, new AspectList().add(Aspect.METAL, 1).add(Aspect.EARTH, 1));
        add(table, ComponentListMF.oreIron, new AspectList().add(Aspect.METAL, 1).add(Aspect.EARTH, 1));
        add(table, ComponentListMF.oreSilver, new AspectList().add(Aspect.METAL, 1).add(Aspect.GREED, 1));
        add(table, ComponentListMF.oreGold, new AspectList().add(Aspect.METAL, 1).add(Aspect.GREED, 2));
        add(table, ComponentListMF.oreTungsten, new AspectList().add(Aspect.METAL, 2).add(Aspect.EARTH, 1));
    }

    private static void addStoneAndClay(List<TCAspectEntry> table) {
        // Batches, so one point each.
        add(table, ComponentListMF.sharp_rock, new AspectList().add(Aspect.EARTH, 1));
        add(table, ComponentListMF.obsidian_rock, new AspectList().add(Aspect.EARTH, 1).add(Aspect.DARKNESS, 1));
        add(table, ComponentListMF.kaolinite, new AspectList().add(Aspect.EARTH, 1));
        add(table, ComponentListMF.kaolinite_dust, new AspectList().add(Aspect.EARTH, 1).add(Aspect.ENTROPY, 1));
        add(table, ComponentListMF.fireclay, new AspectList().add(Aspect.EARTH, 1).add(Aspect.FIRE, 1));
        add(table, ComponentListMF.clay_brick, new AspectList().add(Aspect.EARTH, 1).add(Aspect.CRAFT, 1));
        add(table, ComponentListMF.fireclay_brick, new AspectList().add(Aspect.EARTH, 1).add(Aspect.FIRE, 1));
        add(table, ComponentListMF.strong_brick, new AspectList().add(Aspect.EARTH, 2).add(Aspect.FIRE, 1));

        add(table, ComponentListMF.clay_pot, new AspectList().add(Aspect.EARTH, 1).add(Aspect.VOID, 1));
        add(table, ComponentListMF.clay_pot_uncooked, new AspectList().add(Aspect.EARTH, 1));
        add(table, ComponentListMF.ingot_mould, new AspectList().add(Aspect.EARTH, 1).add(Aspect.CRAFT, 2));
        add(table, ComponentListMF.ingot_mould_uncooked, new AspectList().add(Aspect.EARTH, 1).add(Aspect.CRAFT, 1));
        add(table, ComponentListMF.pie_tray_uncooked, new AspectList().add(Aspect.EARTH, 1).add(Aspect.CRAFT, 1));
    }

    private static void addMetalParts(List<TCAspectEntry> table) {
        // Nails and fletchings are left out: sixteen to a craft.
        add(table, ComponentListMF.rivet, new AspectList().add(Aspect.METAL, 1));
        add(table, ComponentListMF.bolt, new AspectList().add(Aspect.METAL, 1));
        add(table, ComponentListMF.hinge, new AspectList().add(Aspect.METAL, 1));
        add(table, ComponentListMF.iron_prep, new AspectList().add(Aspect.METAL, 1).add(Aspect.FIRE, 1));
        add(table, ComponentListMF.iron_frame, new AspectList().add(Aspect.METAL, 1));
        add(table, ComponentListMF.iron_strut, new AspectList().add(Aspect.METAL, 1));
        add(table, ComponentListMF.steel_tube, new AspectList().add(Aspect.METAL, 1));
        add(table, ComponentListMF.cogwork_shaft, new AspectList().add(Aspect.METAL, 1).add(Aspect.MECHANISM, 1));
        add(table, ComponentListMF.cogwork_pulley, new AspectList().add(Aspect.METAL, 2).add(Aspect.MECHANISM, 2));
        add(table, ComponentListMF.bronze_gears, new AspectList().add(Aspect.METAL, 2).add(Aspect.MECHANISM, 2));
        add(table, ComponentListMF.tungsten_gears, new AspectList().add(Aspect.METAL, 2).add(Aspect.MECHANISM, 2));
        add(table, ComponentListMF.ingotCompositeAlloy, new AspectList().add(Aspect.METAL, 3).add(Aspect.ORDER, 1));
        add(table, ComponentListMF.diamond_shards, new AspectList().add(Aspect.CRYSTAL, 2).add(Aspect.GREED, 1));
        add(table, ComponentListMF.vine, new AspectList().add(Aspect.PLANT, 1));

        add(table, ComponentListMF.copper_coin, new AspectList().add(Aspect.METAL, 1).add(Aspect.GREED, 1));
        add(table, ComponentListMF.silver_coin, new AspectList().add(Aspect.METAL, 1).add(Aspect.GREED, 2));
        add(table, ComponentListMF.gold_coin, new AspectList().add(Aspect.METAL, 1).add(Aspect.GREED, 3));
    }

    /** One item per metal. The metal follows the MF tier, and an unbreakable material also carries magic. */
    private static void addIngots(List<TCAspectEntry> table) {
        for (int i = 0; i < ComponentListMF.ingots.length; i++) {
            Item ingot = ComponentListMF.ingots[i];
            if (ingot == null || i >= ComponentListMF.ingotMats.length) {
                continue;
            }
            BaseMaterialMF base = BaseMaterialMF.getMaterial(ComponentListMF.ingotMats[i]);
            int tier = base == null ? 0 : Math.max(0, Math.min(base.tier, 3));

            AspectList aspects = new AspectList().add(Aspect.METAL, 2 + tier);
            CustomMaterial material = base == null ? null : CustomMaterial.getMaterial(base.name);
            if (material != null && material.isUnbrekable()) {
                aspects.add(Aspect.MAGIC, 2);
            }
            add(table, ingot, aspects);
        }
    }

    /**
     * Fames for anything edible, corpus for meat, herba for what grew, ignis for what was cooked. A portion is worth
     * the single point it cannot go below; bread slices are left out, twelve to a loaf.
     */
    private static void addFood(List<TCAspectEntry> table) {
        // Whole cuts.
        AspectList rawMeat = new AspectList().add(Aspect.FLESH, 2).add(Aspect.HUNGER, 2).add(Aspect.BEAST, 1);
        addAll(table, rawMeat, FoodListMF.wolf_raw, FoodListMF.horse_raw);

        AspectList cookedMeat = new AspectList().add(Aspect.FLESH, 2).add(Aspect.HUNGER, 3).add(Aspect.FIRE, 1);
        addAll(table, cookedMeat, FoodListMF.wolf_cooked, FoodListMF.horse_cooked, FoodListMF.jerky);

        // Up to three per carcass, then strips, chunks and mince one for one off these.
        AspectList portionRaw = new AspectList().add(Aspect.FLESH, 1).add(Aspect.HUNGER, 1);
        addAll(
                table,
                portionRaw,
                FoodListMF.generic_meat_uncooked,
                FoodListMF.generic_meat_strip_uncooked,
                FoodListMF.generic_meat_chunk_uncooked,
                FoodListMF.generic_meat_mince_uncooked);

        AspectList portionCooked = new AspectList().add(Aspect.FLESH, 1).add(Aspect.HUNGER, 1).add(Aspect.FIRE, 1);
        addAll(
                table,
                portionCooked,
                FoodListMF.generic_meat_cooked,
                FoodListMF.generic_meat_strip_cooked,
                FoodListMF.generic_meat_chunk_cooked,
                FoodListMF.generic_meat_mince_cooked);

        // Four to a craft.
        add(table, FoodListMF.saussage_raw, new AspectList().add(Aspect.FLESH, 1).add(Aspect.HUNGER, 1));
        add(
                table,
                FoodListMF.saussage_cooked,
                new AspectList().add(Aspect.FLESH, 1).add(Aspect.HUNGER, 1).add(Aspect.FIRE, 1));

        add(table, FoodListMF.guts, new AspectList().add(Aspect.FLESH, 3).add(Aspect.DEATH, 1));

        AspectList grain = new AspectList().add(Aspect.PLANT, 2).add(Aspect.HUNGER, 1);
        addAll(
                table,
                grain,
                FoodListMF.flour,
                FoodListMF.breadcrumbs,
                FoodListMF.oats,
                FoodListMF.dough,
                FoodListMF.pastry,
                FoodListMF.raw_bread,
                FoodListMF.sweetroll_raw,
                FoodListMF.eclair_raw);

        // Bread slices are left out: twelve to a loaf.
        AspectList baked = new AspectList().add(Aspect.PLANT, 1).add(Aspect.HUNGER, 2).add(Aspect.CRAFT, 1);
        addAll(table, baked, FoodListMF.breadroll, FoodListMF.cheese_roll, FoodListMF.eclair_empty);

        AspectList sweetroll = new AspectList().add(Aspect.PLANT, 1).add(Aspect.HUNGER, 3).add(Aspect.CRAFT, 1);
        addPortioned(table, FoodListMF.sweetroll_uniced, sweetroll);
        addPortioned(table, FoodListMF.sweetroll, sweetroll);
        addPortioned(table, FoodListMF.eclair_uniced, sweetroll);
        addPortioned(table, FoodListMF.eclair, sweetroll);

        AspectList meal = new AspectList().add(Aspect.FLESH, 1).add(Aspect.HUNGER, 4).add(Aspect.CRAFT, 1);
        addPortioned(table, FoodListMF.sandwitch_meat, meal);
        addPortioned(table, FoodListMF.sandwitch_big, meal);
        add(table, FoodListMF.stew, meal.copy());

        AspectList dairy = new AspectList().add(Aspect.HUNGER, 2).add(Aspect.BEAST, 1);
        addAll(table, dairy, FoodListMF.curds, FoodListMF.custard);

        // Sets into a wheel of eight slices, so priced as the wheel.
        add(table, FoodListMF.cheese_pot, new AspectList().add(Aspect.HUNGER, 6).add(Aspect.BEAST, 2));

        AspectList sweet = new AspectList().add(Aspect.PLANT, 1).add(Aspect.HUNGER, 1);
        addAll(
                table,
                sweet,
                FoodListMF.icing,
                FoodListMF.sugarpot,
                FoodListMF.coca_powder,
                FoodListMF.chocolate,
                FoodListMF.chocchips,
                FoodListMF.berriesJuicy);

        // The bush regrows them.
        add(table, FoodListMF.berries, new AspectList().add(Aspect.HUNGER, 1));

        AspectList pieRaw = new AspectList().add(Aspect.HUNGER, 3).add(Aspect.CRAFT, 1);
        addAll(
                table,
                pieRaw,
                FoodListMF.pie_meat_uncooked,
                FoodListMF.pie_apple_uncooked,
                FoodListMF.pie_berry_uncooked,
                FoodListMF.pie_shepard_uncooked,
                FoodListMF.pie_pumpkin_uncooked,
                FoodListMF.cake_raw,
                FoodListMF.cake_simple_raw,
                FoodListMF.cake_carrot_raw,
                FoodListMF.cake_choc_raw,
                FoodListMF.cake_bf_raw);

        // Sets out as eight slices, so worth about what those eight are.
        AspectList pieBaked = new AspectList().add(Aspect.HUNGER, 6).add(Aspect.CRAFT, 1).add(Aspect.FIRE, 1);
        addAll(
                table,
                pieBaked,
                FoodListMF.pie_meat_cooked,
                FoodListMF.pie_apple_cooked,
                FoodListMF.pie_berry_cooked,
                FoodListMF.pie_shepard_cooked,
                FoodListMF.pie_pumpkin_cooked,
                FoodListMF.cake_uniced,
                FoodListMF.cake_simple_uniced,
                FoodListMF.cake_carrot_uniced,
                FoodListMF.cake_choc_uniced,
                FoodListMF.cake_bf_uniced);

        // Eight slices off one cake, pie or cheese wheel; the wheel is a BlockCakeMF like the rest.
        AspectList slice = new AspectList().add(Aspect.HUNGER, 1);
        addAll(
                table,
                slice,
                FoodListMF.cheese_slice,
                FoodListMF.pieslice_apple,
                FoodListMF.pieslice_berry,
                FoodListMF.pieslice_shepards,
                FoodListMF.meatpie_slice,
                FoodListMF.cake_slice,
                FoodListMF.carrotcake_slice,
                FoodListMF.choccake_slice,
                FoodListMF.bfcake_slice);

        // Seasoning and vessels. Jugs are thrown four to a craft.
        add(table, FoodListMF.salt, new AspectList().add(Aspect.ORDER, 2).add(Aspect.EARTH, 1));
        add(table, FoodListMF.bowl_water_salt, new AspectList().add(Aspect.WATER, 2).add(Aspect.ORDER, 1));
        add(table, FoodListMF.jug_uncooked, new AspectList().add(Aspect.EARTH, 1));
        add(table, FoodListMF.jug_empty, new AspectList().add(Aspect.EARTH, 1).add(Aspect.VOID, 1));
        add(table, FoodListMF.jug_water, new AspectList().add(Aspect.EARTH, 1).add(Aspect.WATER, 1));
        add(table, FoodListMF.jug_milk, new AspectList().add(Aspect.EARTH, 1).add(Aspect.BEAST, 1));
        add(table, FoodListMF.cake_tin, new AspectList().add(Aspect.METAL, 1).add(Aspect.VOID, 1));
        add(table, FoodListMF.pie_tray, new AspectList().add(Aspect.EARTH, 1).add(Aspect.VOID, 1));

        AspectList burnt = new AspectList().add(Aspect.ENTROPY, 2).add(Aspect.FIRE, 1);
        addAll(table, burnt, FoodListMF.burnt_food, FoodListMF.burnt_pot, FoodListMF.burnt_pie, FoodListMF.burnt_cake);
    }

    /**
     * Logs and leaves are described for every metadata they wear in the world: rotation, decay or a trunk base do not
     * make a different tree, while the item always drops as zero.
     */
    private static void addPlants(List<TCAspectEntry> table) {
        int[] woodMetas = new int[] { 0, 4, 8, 12, 15 };

        addMetas(table, BlockListMF.log_yew, woodMetas, new AspectList().add(Aspect.TREE, 3).add(Aspect.PLANT, 1));
        addMetas(
                table,
                BlockListMF.log_ironbark,
                woodMetas,
                new AspectList().add(Aspect.TREE, 3).add(Aspect.PLANT, 1).add(Aspect.EARTH, 1));
        addMetas(
                table,
                BlockListMF.log_ebony,
                woodMetas,
                new AspectList().add(Aspect.TREE, 3).add(Aspect.PLANT, 1).add(Aspect.DARKNESS, 1));

        AspectList leaves = new AspectList().add(Aspect.PLANT, 2).add(Aspect.TREE, 1);
        addMetas(table, BlockListMF.leaves_yew, woodMetas, leaves);
        addMetas(table, BlockListMF.leaves_ironbark, woodMetas, leaves);
        addMetas(table, BlockListMF.leaves_ebony, woodMetas, leaves);

        AspectList sapling = new AspectList().add(Aspect.PLANT, 3).add(Aspect.TREE, 2).add(Aspect.LIFE, 1);
        addBlock(table, BlockListMF.sapling_yew, sapling);
        addBlock(table, BlockListMF.sapling_ironbark, sapling);
        addBlock(table, BlockListMF.sapling_ebony, sapling);

        // Several planks per log.
        AspectList planks = new AspectList().add(Aspect.TREE, 1).add(Aspect.CRAFT, 1);
        addBlock(table, BlockListMF.yew_planks, planks);
        addBlock(table, BlockListMF.ironbark_planks, planks);
        addBlock(table, BlockListMF.ebony_planks, planks);

        addBlock(table, BlockListMF.thatch, new AspectList().add(Aspect.PLANT, 1).add(Aspect.CRAFT, 1));

        // Zero is ripe, one is picked.
        addMetas(
                table,
                BlockListMF.berryBush,
                new int[] { 0, 1 },
                new AspectList().add(Aspect.PLANT, 2).add(Aspect.LIFE, 1));

        addBlock(table, BlockListMF.oreCopper, new AspectList().add(Aspect.EARTH, 2).add(Aspect.METAL, 2));
        addBlock(table, BlockListMF.oreTin, new AspectList().add(Aspect.EARTH, 2).add(Aspect.METAL, 2));
        addBlock(
                table,
                BlockListMF.oreSilver,
                new AspectList().add(Aspect.EARTH, 2).add(Aspect.METAL, 1).add(Aspect.GREED, 2));
        addBlock(table, BlockListMF.oreTungsten, new AspectList().add(Aspect.EARTH, 2).add(Aspect.METAL, 3));
        addBlock(table, BlockListMF.oreKaolinite, new AspectList().add(Aspect.EARTH, 3));
        addBlock(table, BlockListMF.oreClay, new AspectList().add(Aspect.EARTH, 3).add(Aspect.WATER, 1));
        addBlock(table, BlockListMF.oreNitre, new AspectList().add(Aspect.EARTH, 2).add(Aspect.ENERGY, 2));
        addBlock(table, BlockListMF.oreSulfur, new AspectList().add(Aspect.EARTH, 2).add(Aspect.FIRE, 2));
        addBlock(table, BlockListMF.oreBorax, new AspectList().add(Aspect.EARTH, 2).add(Aspect.ORDER, 2));
        addBlock(table, BlockListMF.oreCoalRich, new AspectList().add(Aspect.EARTH, 2).add(Aspect.FIRE, 3));
        addBlock(
                table,
                BlockListMF.oreMythic,
                new AspectList().add(Aspect.EARTH, 2).add(Aspect.METAL, 2).add(Aspect.MAGIC, 2));
    }

    private static void addAll(List<TCAspectEntry> table, AspectList aspects, Item... items) {
        for (Item item : items) {
            add(table, item, aspects.copy());
        }
    }

    private static void add(List<TCAspectEntry> table, Item item, AspectList aspects) {
        if (item != null) {
            table.add(new TCAspectEntry(new ItemStack(item, 1, 0), aspects));
        }
    }

    /**
     * A food eaten in bites keeps the rest in its damage value, which Thaumcraft keys on, so each state is worth what
     * is left of it. Only items asked to be portioned come through here.
     */
    private static void addPortioned(List<TCAspectEntry> table, Item item, AspectList whole) {
        if (item == null) {
            return;
        }
        int bites = item.getMaxDamage() + 1;
        if (bites <= 1) {
            add(table, item, whole.copy());
            return;
        }
        for (int damage = 0; damage <= item.getMaxDamage(); damage++) {
            int left = bites - damage;
            AspectList scaled = new AspectList();
            for (Aspect aspect : whole.getAspects()) {
                int amount = whole.getAmount(aspect) * left / bites;
                if (amount > 0) {
                    scaled.add(aspect, amount);
                }
            }
            if (scaled.size() == 0) {
                // The last bite of something small still has to be worth its own cheapest aspect or it vanishes.
                Aspect[] all = whole.getAspectsSortedAmount();
                if (all.length > 0) {
                    scaled.add(all[all.length - 1], 1);
                }
            }
            table.add(new TCAspectEntry(new ItemStack(item, 1, damage), scaled));
        }
    }

    private static void addBlock(List<TCAspectEntry> table, Block block, AspectList aspects) {
        if (block != null) {
            add(table, Item.getItemFromBlock(block), aspects);
        }
    }

    private static void addMetas(List<TCAspectEntry> table, Block block, int[] metas, AspectList aspects) {
        if (block == null) {
            return;
        }
        Item item = Item.getItemFromBlock(block);
        if (item == null) {
            return;
        }
        for (int meta : metas) {
            table.add(new TCAspectEntry(new ItemStack(item, 1, meta), aspects.copy()));
        }
    }
}
