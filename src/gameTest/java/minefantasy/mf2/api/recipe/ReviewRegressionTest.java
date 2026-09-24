package minefantasy.mf2.api.recipe;

import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.gametest.TestItems.*;

import java.util.Collections;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.crafting.MFRecipeKeys;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.NativeRecipes;
import minefantasy.mf2.gametest.TestItems;

@GameTestHolder("minefantasy2")
public class ReviewRegressionTest {

    private static final Item ore = TestItems.ore;
    private static final Item bar = TestItems.bar;

    private static void noOreDictionary() {
        OreNames.set(new OreNames() {

            @Override
            public List<String> namesOf(ItemStack stack) {
                return Collections.emptyList();
            }

            @Override
            public List<ItemStack> stacksOf(String name) {
                return Collections.emptyList();
            }
        });
    }

    private static void restoreOreDictionary() {
        OreNames.set(null);
    }

    private static RecipeId id(String path) {
        return RecipeId.of("test", "quern/" + path);
    }

    @GameTest
    public static void removeWhereSeesStagedAddsAndReplaces(GameTestHelper helper) throws Exception {
        noOreDictionary();
        try {
            RecipeRegistries registries = new RecipeRegistries();
            RecipeRegistry<String> quern = registries.create("quern", r -> null);
            quern.add(id("old"), "old", RecipeSource.NATIVE);
            try (RecipeTransaction tx = registries.begin(RecipeSource.NATIVE)) {
                tx.add(quern, id("new"), "new", 0);
                tx.replace(quern, id("old"), "replaced", 0);
                List<RecipeId> removed = tx.removeWhere(quern, e -> !e.getRecipe().equals("old"));
                assertEquals(2, removed.size());
                tx.commit();
            }
            registries.finishLoading();
            assertEquals(0, quern.published().size());
        } finally {
            restoreOreDictionary();
        }
        helper.succeed();
    }

    @GameTest
    public static void lookupSkipsRecipesTheStackCannotPayFor(GameTestHelper helper) throws Exception {
        noOreDictionary();
        try {
            RecipeRegistries registries = new RecipeRegistries();
            RecipeRegistry<ProcessRecipe> quern = registries.create("quern", ProcessRecipe::indexKeys);
            quern.add(
                    id("four"),
                    ProcessRecipe.of(Input.of(ore).amount(4), new ItemStack(bar, 2)),
                    RecipeSource.NATIVE,
                    5);
            quern.add(id("one"), ProcessRecipe.of(Input.of(ore), new ItemStack(bar)), RecipeSource.NATIVE);
            registries.finishLoading();

            assertEquals("quern/one", MFRecipes.find(quern, new ItemStack(ore, 1)).getId().getPath());
            assertEquals("quern/four", MFRecipes.find(quern, new ItemStack(ore, 4)).getId().getPath());
            assertTrue(MFRecipes.accepts(quern, new ItemStack(ore, 1)));
            assertNull(MFRecipes.find(quern, new ItemStack(bar, 1)));
        } finally {
            restoreOreDictionary();
        }
        helper.succeed();
    }

    @GameTest
    public static void singlePotIsConsumedPerGrind(GameTestHelper helper) throws Exception {
        noOreDictionary();
        try {
            ItemStack[] slots = { new ItemStack(ore, 3), new ItemStack(bar, 16), null };
            CraftPlan plan = CraftPlan.builder(id("x"), 1, 2).use(0, Input.of(ore), slots[0])
                    .use(1, Input.of(slots[1]).amount(1), slots[1]).output(new ItemStack(ore)).build();
            assertTrue(plan.apply(CraftInventory.of(slots)));
            assertEquals(15, slots[1].stackSize);
        } finally {
            restoreOreDictionary();
        }
        helper.succeed();
    }

    @GameTest
    public static void projectCarriesItsWorkedOutRequirements(GameTestHelper helper) throws Exception {
        noOreDictionary();
        try {
            ItemStack[] slots = { new ItemStack(ore, 2), null };
            CraftPlan base = project(slots, 200F);
            CraftPlan slower = project(slots, 500F);
            assertEquals(200F, base.require(MFRecipeKeys.TIME, 0F), 0F);
            assertFalse("a changed requirement is another project", base.sameAs(slower));

            RunningCraft running = new RunningCraft();
            running.start(base);
            NBTTagCompound nbt = new NBTTagCompound();
            running.write(nbt);
            RunningCraft loaded = new RunningCraft();
            loaded.read(nbt);
            assertTrue(loaded.holds(project(slots, 200F)));
            assertFalse(loaded.holds(slower));
        } finally {
            restoreOreDictionary();
        }
        helper.succeed();
    }

    @GameTest
    public static void stationPlacedProductIsNotPaidOutAndReturnsSpill(GameTestHelper helper) throws Exception {
        noOreDictionary();
        try {
            ItemStack[] slots = { new ItemStack(ore, 2), new ItemStack(bar, 64) };
            CraftPlan plan = CraftPlan.builder(id("x"), 1, 1)
                    .consume(0, slots[0], 2, new ItemStack(ore), s -> s.getItem() == ore).product(new ItemStack(ore))
                    .build();
            List<ItemStack> spill = new java.util.ArrayList<>();
            assertTrue(plan.apply(CraftInventory.of(slots), spill));
            assertNull(slots[0]);
            assertEquals(64, slots[1].stackSize);
            assertEquals(new ItemStack(ore).getItem(), plan.getProduct().getItem());
        } finally {
            restoreOreDictionary();
        }
        helper.succeed();
    }

    private static CraftPlan project(ItemStack[] slots, float time) {
        return CraftPlan.builder(id("x"), 1, 1).consume(0, slots[0], 2, new ItemStack(ore), s -> s.getItem() == ore)
                .require(MFRecipeKeys.TIME, time).require(MFRecipeKeys.RESEARCH, "").product(new ItemStack(bar, 2))
                .build();
    }

    @GameTest
    public static void runningCraftSurvivesSaveAndLoad(GameTestHelper helper) throws Exception {
        noOreDictionary();
        try {
            ItemStack[] slots = { new ItemStack(ore, 2), null };
            CraftPlan plan = CraftPlan.builder(id("x"), 1, 1).use(0, Input.of(ore).amount(2), slots[0])
                    .output(new ItemStack(bar, 2)).build();
            RunningCraft started = new RunningCraft();
            started.start(plan);
            NBTTagCompound nbt = new NBTTagCompound();
            started.write(nbt);

            RunningCraft loaded = new RunningCraft();
            loaded.read(nbt);
            assertTrue(loaded.holds(CheckResult.success(plan)));
            assertFalse(loaded.holds(CheckResult.failure(CheckResult.Reason.NO_RECIPE)));

            RunningCraft old = new RunningCraft();
            old.read(new NBTTagCompound());
            assertFalse("2.x saves carry no craft", old.isRunning());
        } finally {
            restoreOreDictionary();
        }
        helper.succeed();
    }

    @GameTest
    public static void registryRefusesInputsItsStationWouldIgnore(GameTestHelper helper) throws Exception {
        noOreDictionary();
        try {
            RecipeRegistries registries = new RecipeRegistries();
            RecipeRegistry<ProcessRecipe> rack = registries
                    .create("quern", ProcessRecipe::indexKeys, InputSupport.ONE_CONSUMED.of(ProcessRecipe::getInput));
            rack.add(id("one"), ProcessRecipe.of(Input.of(ore), new ItemStack(bar)), RecipeSource.NATIVE);
            for (Input refused : new Input[] { Input.of(ore).amount(2), Input.of(ore).usage(Usage.CATALYST) }) {
                try {
                    rack.add(id("bad"), ProcessRecipe.of(refused, new ItemStack(bar)), RecipeSource.NATIVE);
                    fail("accepted " + refused);
                } catch (RecipeRegistrationException expected) {
                    assertTrue(expected.getMessage().contains("refused"));
                }
            }
            assertFalse(rack.containsWorking(id("bad")));
        } finally {
            restoreOreDictionary();
        }
        helper.succeed();
    }

    @GameTest
    public static void nativeIdsDoNotDependOnRegistrationOrder(GameTestHelper helper) throws Exception {
        noOreDictionary();
        try {
            for (boolean namedFirst : new boolean[] { false, true }) {
                RecipeRegistry<ProcessRecipe> quern = new RecipeRegistries().create("quern", ProcessRecipe::indexKeys);
                RecipeId plain = null;
                RecipeId named = null;
                for (int step = 0; step < 2; step++) {
                    if ((step == 0) == namedFirst) {
                        try (NativeRecipes.Variant v = NativeRecipes.variant("Fine Grind")) {
                            named = NativeRecipes.addNative(quern, new ItemStack(ore), grind(2)).getId();
                        }
                    } else {
                        plain = NativeRecipes.addNative(quern, new ItemStack(ore), grind(1)).getId();
                    }
                }
                assertEquals("quern/minefantasy2tests.ore.0", plain.getPath());
                assertEquals("quern/minefantasy2tests.ore.0.fine_grind", named.getPath());
            }
        } finally {
            restoreOreDictionary();
        }
        helper.succeed();
    }

    @GameTest
    public static void secondUnnamedRecipeForTheSameThingIsRefused(GameTestHelper helper) throws Exception {
        noOreDictionary();
        try {
            RecipeRegistry<ProcessRecipe> quern = new RecipeRegistries().create("quern", ProcessRecipe::indexKeys);
            NativeRecipes.addNative(quern, new ItemStack(ore), grind(1));
            try {
                NativeRecipes.addNative(quern, new ItemStack(ore), grind(2));
                fail("a second unnamed recipe got an id");
            } catch (RecipeRegistrationException e) {
                assertTrue(e.getMessage(), e.getMessage().contains("NativeRecipes.variant"));
            }
        } finally {
            restoreOreDictionary();
        }
        helper.succeed();
    }

    private static ProcessRecipe grind(int bars) {
        return ProcessRecipe.of(Input.of(ore), new ItemStack(bar, bars));
    }

    @GameTest
    public static void registrationRefusesBadValuesNamingIdAndSource(GameTestHelper helper) throws Exception {
        noOreDictionary();
        try {
            RecipeRegistries registries = new RecipeRegistries();
            RecipeRegistry<ProcessRecipe> quern = registries.create("quern", ProcessRecipe::indexKeys);
            RecipeRegistry<minefantasy.mf2.api.cooking.CookRecipe> cooking = registries
                    .create("cooking", minefantasy.mf2.api.cooking.CookRecipe::indexKeys);
            RecipeRegistry<Object> carpenter = registries.create("carpenter", r -> null);

            assertRefused(
                    () -> quern.add(
                            id("unknown"),
                            ProcessRecipe.of(Input.of(ore).material("unobtainium"), new ItemStack(bar)),
                            RecipeSource.script("test.zs")),
                    "test:quern/unknown (SCRIPT(test.zs))",
                    "unknown material 'unobtainium'");
            assertRefused(
                    () -> cooking.add(
                            RecipeId.of("test", "cooking/cold"),
                            minefantasy.mf2.api.cooking.CookRecipe
                                    .of(Input.of(ore), new ItemStack(bar), null, 300, 100, 20, 0, false, false),
                            RecipeSource.NATIVE),
                    "test:cooking/cold (NATIVE(minefantasy2))",
                    "temperatures must rise");
            assertRefused(
                    () -> carpenter.add(
                            RecipeId.of("test", "carpenter/wide"),
                            minefantasy.mf2.api.crafting.GridRecipe.shaped(
                                    minefantasy.mf2.api.crafting.GridRecipe.Grid.BENCH,
                                    5,
                                    1,
                                    new ItemStack[] { new ItemStack(ore), null, null, null, new ItemStack(ore) },
                                    null,
                                    new ItemStack(bar)).time(10).build(),
                            RecipeSource.NATIVE),
                    "test:carpenter/wide",
                    "does not fit the 4x4 grid");
            assertRefused(
                    () -> carpenter.add(
                            RecipeId.of("test", "carpenter/instant"),
                            minefantasy.mf2.api.crafting.GridRecipe.shaped(
                                    minefantasy.mf2.api.crafting.GridRecipe.Grid.BENCH,
                                    1,
                                    1,
                                    new ItemStack[] { new ItemStack(ore) },
                                    null,
                                    new ItemStack(bar)).time(0).build(),
                            RecipeSource.NATIVE),
                    "test:carpenter/instant",
                    "time must be positive");
        } finally {
            restoreOreDictionary();
        }
        helper.succeed();
    }

    private static void assertRefused(Runnable register, String who, String why) {
        try {
            register.run();
            fail("accepted " + who);
        } catch (RecipeRegistrationException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(who) && e.getMessage().contains(why));
        }
    }

    @GameTest
    public static void inputExplainsWhyAStackDoesNotPay(GameTestHelper helper) throws Exception {
        noOreDictionary();
        try {
            Input four = Input.of(ore).amount(4);
            assertNull(four.explain(new ItemStack(ore, 4)));
            assertEquals("wrong_item", four.explain(new ItemStack(bar, 4)).getId());
            assertEquals(CheckResult.Reason.of("amount", 2, 4), four.explain(new ItemStack(ore, 2)));
            assertEquals("condition", Input.of(ore).where(s -> false, "never").explain(new ItemStack(ore)).getId());
        } finally {
            restoreOreDictionary();
        }
        helper.succeed();
    }
}
