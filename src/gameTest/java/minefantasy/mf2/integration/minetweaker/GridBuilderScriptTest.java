package minefantasy.mf2.integration.minetweaker;

import static minefantasy.mf2.gametest.Assert.*;

import java.util.List;

import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.crafting.GridRecipe;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.recipe.RecipeEntry;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.api.recipe.RecipeRegistry;

/** The grid recipe builders: what register() adds, and everything it refuses with the recipe's id. */
@GameTestHolder(value = "minefantasy2", requiredMods = "MineTweaker3")
public class GridBuilderScriptTest {

    private GridBuilderScriptTest() {}

    private static GridRecipe added(RecipeRegistry<GridRecipe> registry, String id) {
        RecipeEntry<GridRecipe> entry = registry.published().get(RecipeId.parse(id));
        assertNotNull(id + " is not registered", entry);
        return entry.getRecipe();
    }

    /** A 4 by 4 crafting grid filled by column and row. */
    private static InventoryCrafting grid(Object... cells) {
        ItemStack[] slots = new ItemStack[16];
        for (int i = 0; i < cells.length; i += 3) {
            slots[(Integer) cells[i] + (Integer) cells[i + 1] * 4] = (ItemStack) cells[i + 2];
        }
        return new InventoryCrafting(null, 4, 4) {

            @Override
            public ItemStack getStackInSlot(int slot) {
                return slot >= 0 && slot < slots.length ? slots[slot] : null;
            }

            @Override
            public ItemStack getStackInRowAndColumn(int col, int row) {
                return col >= 0 && col < 4 && row >= 0 && row < 4 ? slots[col + row * 4] : null;
            }
        };
    }

    @GameTest
    public static void anAnvilRecipeIsBuiltAsNamed(GameTestHelper helper) throws Exception {
        Scripts.around(helper, () -> {
            assertEquals(
                    0,
                    Scripts.run(
                            "mods.minefantasy.Anvil.shaped(\"built_sword\", <minecraft:iron_sword>)",
                            "    .pattern([\" I \", \" I \", \" H \"])",
                            "    .key(\"I\", mods.minefantasy.MF.input(<minefantasy2:custom_bar>, \"steel\"))",
                            "    .key(\"H\", <minecraft:stick>)",
                            "    .tool(\"hammer\", 2).stationTier(1).research(\"craftWeapons\").time(100).hot()",
                            "    .register();").size());
            GridRecipe recipe = added(MFRecipes.ANVIL, "crafttweaker:anvil/built_sword");
            assertTrue(recipe.isShaped());
            assertEquals(3, recipe.getWidth());
            assertEquals(3, recipe.getHeight());
            assertEquals("hammer", recipe.getToolType());
            assertEquals(2, recipe.getRecipeHammer());
            assertEquals(1, recipe.getAnvil());
            assertEquals(100, recipe.getCraftTime());
            assertEquals("craftWeapons", recipe.getResearch());
            assertTrue("the result is not hot", recipe.outputHot());
            assertNull("an empty cell got an ingredient", recipe.getEntry(0));
        });
    }

    /** A pattern sits in the top left and is never mirrored unless anywhere() frees it. */
    @GameTest
    public static void aPatternIsAnchoredUnlessFreed(GameTestHelper helper) throws Exception {
        Scripts.around(helper, () -> {
            assertEquals(
                    0,
                    Scripts.run(
                            "mods.minefantasy.CarpenterBench.shaped(\"anchored\", <minecraft:diamond>)",
                            "    .pattern([\"SB\"]).key(\"S\", <minecraft:stick>).key(\"B\", <minecraft:bone>)",
                            "    .tool(\"hands\", 0).time(10).register();",
                            "mods.minefantasy.CarpenterBench.shaped(\"free\", <minecraft:emerald>)",
                            "    .pattern([\"SB\"]).key(\"S\", <minecraft:stick>).key(\"B\", <minecraft:bone>)",
                            "    .tool(\"hands\", 0).time(10).anywhere().register();").size());
            InventoryCrafting corner = grid(0, 0, new ItemStack(Items.stick), 1, 0, new ItemStack(Items.bone));
            InventoryCrafting shiftedMirrored = grid(2, 2, new ItemStack(Items.bone), 3, 2, new ItemStack(Items.stick));
            GridRecipe anchored = added(MFRecipes.CARPENTER, "crafttweaker:carpenter/anchored");
            assertTrue(anchored.matches(corner));
            assertFalse("an anchored pattern matched shifted and mirrored", anchored.matches(shiftedMirrored));
            GridRecipe free = added(MFRecipes.CARPENTER, "crafttweaker:carpenter/free");
            assertTrue(free.matches(corner));
            assertTrue("anywhere() did not shift and mirror", free.matches(shiftedMirrored));
        });
    }

    @GameTest
    public static void aShapelessKitchenRecipeTakesItsDefaults(GameTestHelper helper) throws Exception {
        Scripts.around(helper, () -> {
            assertEquals(
                    0,
                    Scripts.run(
                            "mods.minefantasy.KitchenBench.shapeless(\"mash\", <minecraft:mushroom_stew>)",
                            "    .ingredients([<minecraft:apple>, <minecraft:bowl>]).tool(\"spoon\", 0).time(40)",
                            "    .register();").size());
            RecipeRegistry<GridRecipe> registry = minefantasy.mf2.config.ConfigKitchen.enableBench ? MFRecipes.KITCHEN
                    : MFRecipes.CARPENTER;
            GridRecipe recipe = added(registry, "crafttweaker:" + registry.getStation() + "/mash");
            assertFalse(recipe.isShaped());
            assertEquals(2, recipe.getRecipeSize());
            assertTrue("no default dirt", recipe.getDirtyAmount() > 0);
        });
    }

    /** Each problem is reported with the recipe's id, and nothing is added. */
    @GameTest
    public static void registerRefusesWhatItCannotBuild(GameTestHelper helper) throws Exception {
        Scripts.around(helper, () -> {
            String[][] cases = { { "mods.minefantasy.Anvil.shaped(\"unknown_symbol\", <minecraft:diamond>)"
                    + ".pattern([\"IX\"]).key(\"I\", <minecraft:stick>).tool(\"hammer\", 0).time(10).register();",
                    "anvil/unknown_symbol", "symbol X has no key" },
                    { "mods.minefantasy.Anvil.shaped(\"unused_key\", <minecraft:diamond>)"
                            + ".pattern([\"I\"]).key(\"I\", <minecraft:stick>).key(\"H\", <minecraft:bone>)"
                            + ".tool(\"hammer\", 0).time(10).register();", "anvil/unused_key", "key H is not used" },
                    { "mods.minefantasy.Anvil.shaped(\"ragged\", <minecraft:diamond>)"
                            + ".pattern([\"II\", \"I\"]).key(\"I\", <minecraft:stick>).tool(\"hammer\", 0).time(10)"
                            + ".register();", "anvil/ragged", "row 2 is not 2 wide" },
                    { "mods.minefantasy.Anvil.shaped(\"too_big\", <minecraft:diamond>)"
                            + ".pattern([\"IIIIIII\"]).key(\"I\", <minecraft:stick>).tool(\"hammer\", 0).time(10)"
                            + ".register();", "anvil/too_big", "does not fit the 6x4 grid" },
                    { "mods.minefantasy.Anvil.shaped(\"no_tool\", <minecraft:diamond>)"
                            + ".pattern([\"I\"]).key(\"I\", <minecraft:stick>).time(10).register();", "anvil/no_tool",
                            "tool must be given" },
                    { "mods.minefantasy.Anvil.shaped(\"no_time\", <minecraft:diamond>)"
                            + ".pattern([\"I\"]).key(\"I\", <minecraft:stick>).tool(\"hammer\", 0).register();",
                            "anvil/no_time", "time must be positive" },
                    { "mods.minefantasy.CarpenterBench.shapeless(\"hot_bench\", <minecraft:diamond>)"
                            + ".ingredients([<minecraft:stick>]).tool(\"hands\", 0).time(10).hot().register();",
                            "carpenter/hot_bench", "only the anvil gives hot results" },
                    { "mods.minefantasy.Anvil.shaped(\"bad_skill\", <minecraft:diamond>)"
                            + ".pattern([\"I\"]).key(\"I\", <minecraft:stick>).tool(\"hammer\", 0).time(10)"
                            + ".skill(\"juggling\").register();", "anvil/bad_skill", "unknown skill juggling" },
                    { "mods.minefantasy.Anvil.shaped(\"bad_research\", <minecraft:diamond>)"
                            + ".pattern([\"I\"]).key(\"I\", <minecraft:stick>).tool(\"hammer\", 0).time(10)"
                            + ".research(\"noSuchResearch\").register();", "anvil/bad_research",
                            "'noSuchResearch' is not a known research" },
                    { "mods.minefantasy.Anvil.shaped(\"reused\", <minecraft:diamond>)"
                            + ".pattern([\"I\"]).key(\"I\", <minecraft:stick>.reuse()).tool(\"hammer\", 0).time(10)"
                            + ".register();", "anvil/reused", "input I has transformers" },
                    { "mods.minefantasy.Anvil.shaped(\"blank\", <minecraft:diamond>)"
                            + ".pattern([\"\", \"\"]).tool(\"hammer\", 0).time(10).register();", "anvil/blank",
                            "the pattern has no ingredients" },
                    { "mods.minefantasy.CarpenterBench.shapeless(\"dirty\", <minecraft:diamond>)"
                            + ".ingredients([<minecraft:stick>]).tool(\"hands\", 0).time(10).dirt(1.0).register();",
                            "carpenter/dirty", "only the kitchen bench gets dirty" },
                    { "mods.minefantasy.Anvil.shapeless(\"mixed\", <minecraft:diamond>)"
                            + ".pattern([\"I\"]).ingredients([<minecraft:stick>]).tool(\"hammer\", 0).time(10)"
                            + ".register();", "anvil/mixed", "pattern is for shaped recipes" } };
            for (String[] c : cases) {
                List<String> errors = Scripts.run(c[0]);
                String id = "crafttweaker:" + c[1];
                assertTrue(
                        c[1] + " was not refused with \"" + c[2] + "\": " + errors,
                        errors.stream()
                                .anyMatch(e -> e.contains(id) && e.contains(c[2]) && e.contains("Nothing changed.")));
                RecipeRegistry<GridRecipe> registry = c[1].startsWith("anvil") ? MFRecipes.ANVIL : MFRecipes.CARPENTER;
                assertNull(c[1] + " was added", registry.published().get(RecipeId.parse(id)));
            }
        });
    }

    /** A negative dirt is the script's mistake, refused, not quietly the bench's default. */
    @GameTest
    public static void negativeDirtIsRefused(GameTestHelper helper) throws Exception {
        Scripts.around(helper, () -> {
            for (String line : new String[] {
                    "mods.minefantasy.KitchenBench.shapeless(\"minus\", <minecraft:mushroom_stew>)"
                            + ".ingredients([<minecraft:apple>]).tool(\"spoon\", 0).time(40).dirt(-5.0).register();",
                    "mods.minefantasy.KitchenBench.addShapeless(\"minus\", <minecraft:mushroom_stew>, \"\", \"\", \"\","
                            + " \"spoon\", 40, -5.0, [<minecraft:apple>]);" }) {
                List<String> errors = Scripts.run(line);
                assertTrue(
                        "a negative dirt was taken: " + errors,
                        errors.stream().anyMatch(e -> e.contains("dirty amount must not be negative")));
            }
        });
    }
}
