package minefantasy.mf2.integration.minetweaker;

import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.gametest.TestItems.*;
import static minefantasy.mf2.integration.minetweaker.Scripts.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import net.minecraft.init.Blocks;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.gtnewhorizons.horizonqa.api.GameTestArguments;
import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;
import com.gtnewhorizons.horizonqa.api.annotation.MethodSource;

import minefantasy.mf2.api.cooking.CookRecipe;
import minefantasy.mf2.api.crafting.GridRecipe;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.MineFantasyFuels;
import minefantasy.mf2.api.crafting.Salvage;
import minefantasy.mf2.api.crafting.exotic.SpecialForging;
import minefantasy.mf2.api.crafting.refine.BloomRecipe;
import minefantasy.mf2.api.heating.Heatable;
import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.material.CustomMaterial;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.api.recipe.RecipeEntry;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.api.recipe.RecipeRegistry;
import minefantasy.mf2.api.refine.Alloy;
import minefantasy.mf2.gametest.TestItems;
import minefantasy.mf2.integration.minetweaker.helpers.ScriptRecipes;

/**
 * Every MineFantasy tweaker called from a script run through CraftTweaker's reload: what a script adds, replaces and
 * removes is what the stations then see, and a broken line is logged and leaves nothing behind.
 */
@GameTestHolder(value = "minefantasy2", requiredMods = "MineTweaker3")
public class TweakerTest {

    private TweakerTest() {}

    private static <R> R recipe(RecipeRegistry<R> registry, RecipeId id) {
        RecipeEntry<R> entry = registry.published().get(id);
        return entry == null ? null : entry.getRecipe();
    }

    private static <R> R added(RecipeRegistry<R> registry, String name) {
        R recipe = recipe(registry, ScriptRecipes.scriptId(registry.getStation(), name));
        assertNotNull("the script's " + name + " was not published", recipe);
        return recipe;
    }

    private static void absent(RecipeRegistry<?> registry, RecipeId id) {
        assertNull(id + " is still published", registry.published().get(id));
    }

    private static void noErrors(List<String> errors) {
        assertEquals("the script logged errors", Collections.emptyList(), errors);
    }

    // region one-input stations

    /** A one-input station's case: its tweaker, registry, accepted amounts, extra arguments and recipe parts. */
    private static <R> GameTestArguments oneInputCase(String name, String tweaker, RecipeRegistry<R> registry,
            int amount, int made, String args, Function<R, Input> input, Function<R, ItemStack> output) {
        return GameTestArguments
                .namedValues(name, new Object[] { tweaker, registry, amount, made, args, input, output });
    }

    public static List<GameTestArguments> oneInputStation() {
        return Arrays.asList(
                oneInputCase(
                        "quern",
                        "Quern",
                        MFRecipes.QUERN,
                        3,
                        2,
                        ", 0, true",
                        ProcessRecipe::getInput,
                        ProcessRecipe::getOutput),
                oneInputCase(
                        "tanning_rack",
                        "TanningRack",
                        MFRecipes.TANNING,
                        3,
                        2,
                        ", 5.0",
                        ProcessRecipe::getInput,
                        ProcessRecipe::getOutput),
                oneInputCase(
                        "big_furnace",
                        "BigFurnace",
                        MFRecipes.BIG_FURNACE,
                        3,
                        2,
                        ", 0",
                        ProcessRecipe::getInput,
                        ProcessRecipe::getOutput),
                oneInputCase(
                        "blast_furnace",
                        "BlastFurnace",
                        MFRecipes.BLAST_FURNACE,
                        3,
                        2,
                        "",
                        ProcessRecipe::getInput,
                        ProcessRecipe::getOutput),
                // The bloomery smelts the whole stack in its slot: a recipe names one item of it and gives one bloom
                oneInputCase(
                        "bloomery",
                        "Bloomery",
                        MFRecipes.BLOOMERY,
                        1,
                        1,
                        ", \"\"",
                        BloomRecipe::getInput,
                        BloomRecipe::getOutput),
                oneInputCase(
                        "cooking",
                        "Cooking",
                        MFRecipes.COOKING,
                        3,
                        2,
                        ", 50, 500, 10, 5, false, true",
                        CookRecipe::getInput,
                        CookRecipe::getOutput));
    }

    /**
     * add, removeByOutput, replace and remove of a one-input station: {@code amount} and {@code made} are the input and
     * output amounts it accepts, {@code args} follow the input in add and replace.
     */
    @GameTest
    @MethodSource
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void oneInputStation(GameTestHelper helper, String tweaker, RecipeRegistry registry, int amount,
            int made, String args, Function input, Function output) throws Exception {
        around(helper, () -> oneInput(tweaker, registry, amount, made, args, input, output));
    }

    private static <R> void oneInput(String tweaker, RecipeRegistry<R> registry, int amount, int made, String args,
            Function<R, Input> input, Function<R, ItemStack> output) {
        List<RecipeId> natives = natives(registry, 2);
        String call = "mods.minefantasy." + tweaker;
        noErrors(
                run(
                        call + ".add(\"t_add\", "
                                + item("flour")
                                + " * "
                                + made
                                + ", "
                                + item("seed")
                                + " * "
                                + amount
                                + args
                                + ");",
                        call + ".add(\"t_gone\", " + item("bar") + ", " + item("ore") + args + ");",
                        call + ".removeByOutput(" + item("bar") + ");",
                        call + ".replace(\""
                                + natives.get(0)
                                + "\", "
                                + item("junk")
                                + ", "
                                + item("seed")
                                + args
                                + ");",
                        call + ".remove(\"" + natives.get(1) + "\");"));

        R add = added(registry, "t_add");
        assertEquals("the script's input amount", amount, input.apply(add).getAmount());
        assertTrue(input.apply(add).matches(new ItemStack(seed, amount)));
        assertEquals(flour, output.apply(add).getItem());
        assertEquals("the script's output amount", made, output.apply(add).stackSize);

        absent(registry, ScriptRecipes.scriptId(registry.getStation(), "t_gone"));
        R replaced = recipe(registry, natives.get(0));
        assertNotNull("the replaced recipe is gone", replaced);
        assertEquals(junk, output.apply(replaced).getItem());
        absent(registry, natives.get(1));
    }

    @GameTest
    public static void bloomeryRefusesAmounts(GameTestHelper helper) throws Exception {
        around(helper, () -> {
            List<String> errors = run(
                    "mods.minefantasy.Bloomery.add(\"t_pair\", " + item("bar") + ", " + item("ore") + " * 2, \"\");",
                    "mods.minefantasy.Bloomery.add(\"t_double\", " + item("bar") + " * 2, " + item("ore") + ", \"\");");
            assertEquals("both refusals are logged: " + errors, 2, errors.size());
            absent(MFRecipes.BLOOMERY, ScriptRecipes.scriptId("bloomery", "t_pair"));
            absent(MFRecipes.BLOOMERY, ScriptRecipes.scriptId("bloomery", "t_double"));
        });
    }

    @GameTest
    public static void paintOil(GameTestHelper helper) throws Exception {
        around(helper, () -> {
            List<RecipeId> natives = natives(MFRecipes.PAINT_OIL, 2);
            String call = "mods.minefantasy.PaintOil";
            List<String> errors = run(
                    call + ".add(\"t_add\", <minecraft:stone>, <minecraft:cobblestone>);",
                    call + ".add(\"t_gone\", <minecraft:dirt>, <minecraft:sand>);",
                    call + ".removeByOutput(<minecraft:sand>);",
                    call + ".replace(\"" + natives.get(0) + "\", <minecraft:dirt>, <minecraft:glass>);",
                    call + ".remove(\"" + natives.get(1) + "\");",
                    call + ".add(\"t_item\", " + item("seed") + ", <minecraft:glass>);");
            assertEquals("only the item that is not a block is refused: " + errors, 1, errors.size());
            assertTrue(errors.get(0).contains("blocks on both sides"));
            absent(MFRecipes.PAINT_OIL, ScriptRecipes.scriptId("paint_oil", "t_item"));
            ProcessRecipe add = added(MFRecipes.PAINT_OIL, "t_add");
            assertTrue(add.getInput().matches(new ItemStack(Blocks.stone)));
            assertEquals(Item.getItemFromBlock(Blocks.cobblestone), add.getOutput().getItem());
            absent(MFRecipes.PAINT_OIL, ScriptRecipes.scriptId("paint_oil", "t_gone"));
            assertEquals(
                    Item.getItemFromBlock(Blocks.glass),
                    recipe(MFRecipes.PAINT_OIL, natives.get(0)).getOutput().getItem());
            absent(MFRecipes.PAINT_OIL, natives.get(1));
        });
    }

    // endregion

    // region grids

    public static List<GameTestArguments> gridStation() {
        return Arrays.asList(
                GameTestArguments.named("anvil", "Anvil", MFRecipes.ANVIL, ", false, \"hammer\", 0, 0, 10"),
                GameTestArguments
                        .named("carpenter", "CarpenterBench", MFRecipes.CARPENTER, ", \"\", 0.0, \"hands\", 0, 0, 10"),
                GameTestArguments.named("kitchen", "KitchenBench", MFRecipes.KITCHEN, ", \"\", \"hands\", 10, 5.0"));
    }

    /** addShaped, addShapeless, removeByOutput and remove of a grid station; {@code terms} follow skill, research. */
    @GameTest
    @MethodSource
    public static void gridStation(GameTestHelper helper, String tweaker, RecipeRegistry<GridRecipe> registry,
            String terms) throws Exception {
        around(helper, () -> grid(tweaker, registry, terms));
    }

    private static void grid(String tweaker, RecipeRegistry<GridRecipe> registry, String terms) {
        RecipeId native0 = natives(registry, 1).get(0);
        String call = "mods.minefantasy." + tweaker;
        noErrors(
                run(
                        call + ".addShaped(\"t_shaped\", "
                                + item("bar")
                                + ", \"\", \"\""
                                + terms
                                + ", [["
                                + item("ore")
                                + ", "
                                + item("seed")
                                + "]]);",
                        call + ".addShapeless(\"t_shapeless\", "
                                + item("junk")
                                + ", \"\", \"\""
                                + terms
                                + ", ["
                                + item("seed")
                                + " * 2, "
                                + item("ore")
                                + "]);",
                        call + ".addShapeless(\"t_gone\", "
                                + item("flour")
                                + ", \"\", \"\""
                                + terms
                                + ", ["
                                + item("seed")
                                + "]);",
                        call + ".removeByOutput(" + item("flour") + ");",
                        call + ".remove(\"" + native0 + "\");"));

        GridRecipe shaped = added(registry, "t_shaped");
        assertTrue(shaped.isShaped());
        assertEquals(2, shaped.getWidth());
        assertEquals(bar, shaped.getRecipeOutput().getItem());
        GridRecipe shapeless = added(registry, "t_shapeless");
        assertFalse(shapeless.isShaped());
        assertEquals(2, shapeless.getRecipeSize());
        absent(registry, ScriptRecipes.scriptId(registry.getStation(), "t_gone"));
        absent(registry, native0);
    }

    /**
     * A material condition in a script anvil recipe applies to the piece inside a hot stack: with onlyWithTag hot iron
     * matches an iron recipe, hot copper and iron that has cooled below working heat do not. withTag on an input is no
     * condition in CraftTweaker, so a recipe written that way takes any material.
     */
    @GameTest
    public static void anvilScriptTellsHotPiecesApartByMaterial(GameTestHelper helper) throws Exception {
        around(helper, () -> {
            String iron = "({MF_CustomMaterials: {main_metal: \"iron\"}})";
            String add = "mods.minefantasy.Anvil.addShapeless(\"%s\", " + item(
                    "junk") + ", \"\", \"\", false, \"hammer\", 0, 0, 10, [" + item("bar") + ".%s" + iron + "]);";
            noErrors(run(String.format(add, "t_hot_iron", "onlyWithTag"), String.format(add, "t_any", "withTag")));
            GridRecipe recipe = added(MFRecipes.ANVIL, "t_hot_iron");
            assertTrue("hot iron is refused", recipe.matches(anvil(hot(bar, "iron", 500))));
            assertFalse("hot copper passes for iron", recipe.matches(anvil(hot(bar, "copper", 500))));
            assertFalse("iron below working heat passes", recipe.matches(anvil(hot(bar, "iron", 50))));
            assertTrue(
                    "withTag became a condition: the guidance to use onlyWithTag is out of date",
                    added(MFRecipes.ANVIL, "t_any").matches(anvil(hot(bar, "copper", 500))));
        });
    }

    /** A hot stack carrying one piece of the material, at the temperature; workable from 100. */
    private static ItemStack hot(Item item, String material, int temperature) {
        ItemStack piece = new ItemStack(item);
        CustomMaterial.addMaterial(piece, CustomToolHelper.slot_main, material);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag(Heatable.NBT_Item, piece.writeToNBT(new NBTTagCompound()));
        tag.setInteger(Heatable.NBT_CurrentTemp, temperature);
        tag.setInteger(Heatable.NBT_WorkableTemp, 100);
        tag.setInteger(Heatable.NBT_UnstableTemp, 1000);
        ItemStack stack = new ItemStack(TestItems.hot);
        stack.setTagCompound(tag);
        return stack;
    }

    /** An anvil grid holding the stack in its first cell. */
    private static InventoryCrafting anvil(ItemStack stack) {
        int w = GridRecipe.Grid.ANVIL.width;
        int h = GridRecipe.Grid.ANVIL.height;
        return new InventoryCrafting(null, w, h) {

            @Override
            public ItemStack getStackInSlot(int slot) {
                return slot == 0 ? stack : null;
            }

            @Override
            public ItemStack getStackInRowAndColumn(int col, int row) {
                return col == 0 && row == 0 ? stack : null;
            }
        };
    }

    @GameTest
    public static void gridTooLargeForTheStationIsRefused(GameTestHelper helper) throws Exception {
        around(helper, () -> {
            String cell = item("ore");
            List<String> errors = run(
                    "mods.minefantasy.Anvil.addShaped(\"t_wide\", " + item("bar")
                            + ", \"\", \"\", false, \"hammer\", 0, 0, 10, [["
                            + String.join(", ", Collections.nCopies(7, cell))
                            + "]]);");
            assertFalse("the refusal is not logged", errors.isEmpty());
            absent(MFRecipes.ANVIL, ScriptRecipes.scriptId("anvil", "t_wide"));
        });
    }

    // endregion

    // region crucible, forge, salvage, special forging, fuel

    @GameTest
    public static void crucible(GameTestHelper helper) throws Exception {
        around(helper, () -> {
            RecipeId native0 = natives(MFRecipes.ALLOY, 1).get(0);
            String call = "mods.minefantasy.Crucible";
            noErrors(
                    run(
                            call + ".add(\"t_alloy\", "
                                    + item("bar")
                                    + ", 0, 2, ["
                                    + item("ore")
                                    + ", "
                                    + item("seed")
                                    + "]);",
                            call + ".remove(\"t_alloy_x2\");",
                            call + ".add(\"t_gone\", " + item("flour") + ", 0, 1, [" + item("ore") + "]);",
                            call + ".removeByOutput(" + item("flour") + ");",
                            call + ".remove(\"" + native0 + "\");"));
            Alloy alloy = added(MFRecipes.ALLOY, "t_alloy");
            assertEquals(bar, alloy.getRecipeOutput().getItem());
            assertEquals(2, alloy.getIngredients().size());
            absent(MFRecipes.ALLOY, ScriptRecipes.scriptId("alloy", "t_alloy_x2"));
            absent(MFRecipes.ALLOY, ScriptRecipes.scriptId("alloy", "t_gone"));
            absent(MFRecipes.ALLOY, native0);
        });
    }

    @GameTest
    public static void forge(GameTestHelper helper) throws Exception {
        around(helper, () -> {
            List<RecipeId> natives = natives(MFRecipes.HEATING, 2);
            String call = "mods.minefantasy.Forge";
            noErrors(
                    run(
                            call + ".add(\"t_heat\", " + item("ore") + ", 100, 500, 900);",
                            call + ".add(\"t_gone\", " + item("flour") + ", 100, 500, 900);",
                            call + ".removeFor(" + item("flour") + ");",
                            call + ".replace(\"" + natives.get(0) + "\", " + item("seed") + ", 150, 600, 1000);",
                            call + ".remove(\"" + natives.get(1) + "\");"));
            ItemStack piece = new ItemStack(ore);
            Heatable heat = added(MFRecipes.HEATING, "t_heat");
            assertTrue(heat.getInput().matches(piece));
            assertEquals(100, heat.getWorkableStat(piece));
            assertEquals(500, heat.getUnstableStat(piece));
            assertSame("the forge heats by the script's profile", heat, Heatable.loadStats(piece));
            absent(MFRecipes.HEATING, ScriptRecipes.scriptId("forge_heat", "t_gone"));
            assertTrue(recipe(MFRecipes.HEATING, natives.get(0)).getInput().matches(new ItemStack(seed)));
            absent(MFRecipes.HEATING, natives.get(1));
        });
    }

    @GameTest
    public static void salvage(GameTestHelper helper) throws Exception {
        around(helper, () -> {
            RecipeId native0 = null;
            ItemStack nativeInput = null;
            for (RecipeEntry<Salvage.SalvageRecipe> entry : MFRecipes.SALVAGE.published().all()) {
                if (entry.getRecipe().getAlias() == null
                        && entry.getSource().getKind() == minefantasy.mf2.api.recipe.RecipeSource.Kind.NATIVE) {
                    native0 = entry.getId();
                    nativeInput = entry.getRecipe().getDisplayInput();
                    break;
                }
            }
            assertNotNull("no native salvage entry", native0);
            String call = "mods.minefantasy.Salvage";
            noErrors(
                    run(
                            call + ".set(" + item("blade") + ", [" + item("bar") + " * 2, " + item("ore") + "]);",
                            call + ".set(" + item("jar") + ", [" + item("bar") + "]);",
                            call + ".removeByPart(" + item("bar") + ", " + item("jar") + ");",
                            call + ".remove(" + bracket(nativeInput) + ");"));
            RecipeEntry<Salvage.SalvageRecipe> bladeParts = MFRecipes.SALVAGE.published()
                    .get(Salvage.partsId(new ItemStack(blade)));
            assertNotNull("the script's salvage was not published", bladeParts);
            List<ItemStack> parts = bladeParts.getRecipe().getParts();
            assertEquals(2, parts.size());
            assertEquals(bar, parts.get(0).getItem());
            assertEquals(2, parts.get(0).stackSize);
            absent(MFRecipes.SALVAGE, Salvage.partsId(new ItemStack(jar)));
            absent(MFRecipes.SALVAGE, native0);
        });
    }

    @GameTest
    public static void specialForging(GameTestHelper helper) throws Exception {
        around(helper, () -> {
            RecipeEntry<SpecialForging.SpecialCraft> native0 = MFRecipes.SPECIAL_FORGING.published().all().get(0);
            SpecialForging.SpecialCraft nativeCraft = native0.getRecipe();
            String call = "mods.minefantasy.SpecialForging";
            noErrors(
                    run(
                            call + ".setOrnate(" + item("blade") + ", " + item("hammer") + ");",
                            call + ".setDragonforge(" + item("seed") + ", " + item("flour") + ");",
                            call + ".set(\"ornate\", " + item("jar") + ", " + item("bar") + ");",
                            call + ".remove(\"ornate\", " + item("jar") + ");",
                            call + ".remove(\""
                                    + nativeCraft.getDesign()
                                    + "\", "
                                    + bracket(new ItemStack(nativeCraft.getBase()))
                                    + ");"));
            RecipeEntry<SpecialForging.SpecialCraft> ornate = MFRecipes.SPECIAL_FORGING.published()
                    .get(SpecialForging.idFor("ornate", blade));
            assertNotNull("the ornate craft was not published", ornate);
            assertEquals(hammer, ornate.getRecipe().getOutput());
            assertNotNull(
                    "the dragonforge craft was not published",
                    MFRecipes.SPECIAL_FORGING.published().get(SpecialForging.idFor(SpecialForging.DRAGONFORGE, seed)));
            absent(MFRecipes.SPECIAL_FORGING, SpecialForging.idFor("ornate", jar));
            absent(MFRecipes.SPECIAL_FORGING, native0.getId());
        });
    }

    @GameTest
    public static void carbonFuel(GameTestHelper helper) throws Exception {
        around(helper, () -> {
            noErrors(run("mods.minefantasy.Fuel.addCarbon(" + item("fuel") + ", 3);"));
            assertEquals(3, MineFantasyFuels.getCarbon(new ItemStack(fuel)));
        });
    }

    // endregion

    @GameTest
    public static void aBrokenLineIsLoggedAndTheRestOfTheScriptRuns(GameTestHelper helper) throws Exception {
        around(helper, () -> {
            String ten = String.join(", ", Collections.nCopies(10, item("ore")));
            List<String> errors = run(
                    "mods.minefantasy.Quern.remove(\"minefantasy2:quern/does_not_exist\");",
                    "mods.minefantasy.Crucible.add(\"t_too_many\", " + item("bar") + ", 0, 2, [" + ten + "]);",
                    "mods.minefantasy.Quern.add(\"t_after\", " + item("flour") + ", " + item("seed") + ");");
            assertEquals("each broken line is logged: " + errors, 2, errors.size());
            absent(MFRecipes.ALLOY, ScriptRecipes.scriptId("alloy", "t_too_many"));
            absent(MFRecipes.ALLOY, ScriptRecipes.scriptId("alloy", "t_too_many_x2"));
            added(MFRecipes.QUERN, "t_after");
        });
    }

    @GameTest
    public static void unknownResearchIsRefused(GameTestHelper helper) throws Exception {
        around(helper, () -> {
            String known = minefantasy.mf2.api.knowledge.InformationList.nameMap.keySet().iterator().next();
            String grid = "mods.minefantasy.Anvil.addShapeless(\"%s\", " + item("bar")
                    + ", \"\", \"%s\", false, \"hammer\", 0, 0, 10, ["
                    + item("ore")
                    + "]);";
            List<String> errors = run(
                    String.format(grid, "t_typo", "no_such_research"),
                    String.format(grid, "t_known", known),
                    "mods.minefantasy.Bloomery.add(\"t_typo\", " + item("bar")
                            + ", "
                            + item("ore")
                            + ", \"no_such_research\");");
            assertEquals("both misspelt researches are refused: " + errors, 2, errors.size());
            for (String error : errors) {
                assertTrue(error, error.contains("'no_such_research' is not a known research"));
            }
            absent(MFRecipes.ANVIL, ScriptRecipes.scriptId("anvil", "t_typo"));
            absent(MFRecipes.BLOOMERY, ScriptRecipes.scriptId("bloomery", "t_typo"));
            assertEquals(known, added(MFRecipes.ANVIL, "t_known").getResearch());
        });
    }
}
