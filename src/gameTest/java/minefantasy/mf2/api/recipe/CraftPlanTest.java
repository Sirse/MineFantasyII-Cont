package minefantasy.mf2.api.recipe;

import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.gametest.TestItems.*;

import java.util.Collections;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.gametest.TestItems;

@GameTestHolder("minefantasy2")
public class CraftPlanTest {

    private static final Item ore = TestItems.ore;
    private static final Item bar = TestItems.bar;
    private static final Item hammer = TestItems.hammer;
    private static final Item bucket = TestItems.bucket;
    private static final Item water = TestItems.water;

    private static final RecipeId ID = RecipeId.of("test", "bloomery/bar");

    private static void noOreDictionary() {
        OreNames.set(new OreNames() {

            @Override
            public java.util.List<String> namesOf(ItemStack stack) {
                return Collections.emptyList();
            }

            @Override
            public java.util.List<ItemStack> stacksOf(String name) {
                return Collections.emptyList();
            }
        });
    }

    private static void restoreOreDictionary() {
        OreNames.set(null);
    }

    /** Slots: 0 ore, 1 tool/container input, 2 output. */
    private static ItemStack[] slots(ItemStack in0, ItemStack in1, ItemStack out) {
        return new ItemStack[] { in0, in1, out };
    }

    private static CraftPlan.Builder plan() {
        return CraftPlan.builder(ID, 1, 2);
    }

    @GameTest
    public static void consumesAndProduces(GameTestHelper helper) throws Exception {
        noOreDictionary();
        try {
            ItemStack[] inv = slots(new ItemStack(ore, 5), null, null);
            CraftPlan p = plan().use(0, Input.of(ore).amount(2), inv[0]).output(new ItemStack(bar)).build();
            assertTrue(p.apply(CraftInventory.of(inv)));
            assertEquals(3, inv[0].stackSize);
            assertEquals(bar, inv[2].getItem());
        } finally {
            restoreOreDictionary();
        }
        helper.succeed();
    }

    @GameTest
    public static void fullOutputChangesNothing(GameTestHelper helper) throws Exception {
        noOreDictionary();
        try {
            ItemStack[] inv = slots(new ItemStack(ore, 5), null, new ItemStack(ore, 64));
            CraftPlan p = plan().use(0, Input.of(ore).amount(2), inv[0]).output(new ItemStack(bar)).build();
            assertFalse(p.apply(CraftInventory.of(inv)));
            assertEquals(5, inv[0].stackSize);
            assertEquals(64, inv[2].stackSize);
        } finally {
            restoreOreDictionary();
        }
        helper.succeed();
    }

    @GameTest
    public static void changedInputChangesNothing(GameTestHelper helper) throws Exception {
        noOreDictionary();
        try {
            ItemStack[] inv = slots(new ItemStack(ore, 5), null, null);
            CraftPlan p = plan().use(0, Input.of(ore).amount(2), inv[0]).output(new ItemStack(bar)).build();
            inv[0].stackSize = 1;
            assertFalse(p.apply(CraftInventory.of(inv)));
            assertEquals(1, inv[0].stackSize);
            assertNull(inv[2]);
            inv[0] = new ItemStack(bar, 5);
            assertFalse(p.canApply(CraftInventory.of(inv)));
        } finally {
            restoreOreDictionary();
        }
        helper.succeed();
    }

    @GameTest
    public static void catalystStays(GameTestHelper helper) throws Exception {
        noOreDictionary();
        try {
            ItemStack[] inv = slots(new ItemStack(ore, 1), null, null);
            CraftPlan p = plan().use(0, Input.of(ore).usage(Usage.CATALYST), inv[0]).output(new ItemStack(bar)).build();
            assertTrue(p.apply(CraftInventory.of(inv)));
            assertEquals(1, inv[0].stackSize);
        } finally {
            restoreOreDictionary();
        }
        helper.succeed();
    }

    @GameTest
    public static void toolIsDamagedInPlaceAndBreaks(GameTestHelper helper) throws Exception {
        noOreDictionary();
        try {
            ItemStack[] inv = slots(new ItemStack(ore, 1), new ItemStack(hammer, 1, 8), null);
            Input tool = Input.of(hammer).usage(Usage.damage(2));
            CraftPlan p = plan().use(1, tool, inv[1]).output(new ItemStack(bar)).build();
            assertTrue(p.apply(CraftInventory.of(inv)));
            assertEquals(10, inv[1].getItemDamage());

            CraftPlan again = plan().use(1, tool, inv[1]).output(new ItemStack(bar)).build();
            assertTrue(again.apply(CraftInventory.of(inv)));
            assertNull("broken tool disappears", inv[1]);
            assertEquals(2, inv[2].stackSize);
        } finally {
            restoreOreDictionary();
        }
        helper.succeed();
    }

    @GameTest
    public static void containerComesBack(GameTestHelper helper) throws Exception {
        noOreDictionary();
        try {
            ItemStack[] inv = slots(null, new ItemStack(water), null);
            CraftPlan p = plan().use(1, Input.of(water).usage(Usage.CONTAINER), inv[1]).build();
            assertTrue(p.apply(CraftInventory.of(inv)));
            assertEquals(bucket, inv[1].getItem());
        } finally {
            restoreOreDictionary();
        }
        helper.succeed();
    }

    @GameTest
    public static void transformReplacesUsedPartOnly(GameTestHelper helper) throws Exception {
        noOreDictionary();
        try {
            ItemStack[] inv = slots(new ItemStack(ore, 5), null, null);
            Input cooled = Input.of(ore).amount(2).usage(Usage.transform(s -> new ItemStack(bar, s.stackSize)));
            CraftPlan p = plan().use(0, cooled, inv[0]).build();
            assertTrue(p.apply(CraftInventory.of(inv)));
            assertEquals(3, inv[0].stackSize);
            assertEquals(bar, inv[2].getItem());
            assertEquals(2, inv[2].stackSize);
        } finally {
            restoreOreDictionary();
        }
        helper.succeed();
    }

    @GameTest
    public static void fingerprintIgnoresGenerationButSeesOutputs(GameTestHelper helper) throws Exception {
        noOreDictionary();
        try {
            ItemStack in = new ItemStack(ore, 5);
            CraftPlan a = CraftPlan.builder(ID, 1, 2).use(0, Input.of(ore), in).output(new ItemStack(bar)).build();
            CraftPlan b = CraftPlan.builder(ID, 7, 2).use(0, Input.of(ore), in).output(new ItemStack(bar)).build();
            CraftPlan c = CraftPlan.builder(ID, 1, 2).use(0, Input.of(ore), in).output(new ItemStack(bar, 2)).build();
            assertTrue(a.sameAs(b));
            assertFalse(a.sameAs(c));
            assertTrue(a.matchesFingerprint(b.fingerprint()));
        } finally {
            restoreOreDictionary();
        }
        helper.succeed();
    }

    @GameTest
    public static void planDoesNotShareStacks(GameTestHelper helper) throws Exception {
        noOreDictionary();
        try {
            ItemStack out = new ItemStack(bar);
            ItemStack in = new ItemStack(ore, 5);
            CraftPlan p = plan().use(0, Input.of(ore), in).output(out).build();
            out.stackSize = 50;
            in.stackSize = 0;
            assertEquals(1, p.getOutputs().get(0).stackSize);
            p.getOutputs().get(0).stackSize = 9;
            assertEquals(1, p.getOutputs().get(0).stackSize);
        } finally {
            restoreOreDictionary();
        }
        helper.succeed();
    }

    // region lookup

    private static RecipeRegistries lookupSetup(RecipeRegistry<?>[] out) {
        RecipeRegistries registries = new RecipeRegistries();
        RecipeRegistry<Input> reg = registries.create("bloomery", Input::indexKeys);
        out[0] = reg;
        return registries;
    }

    @GameTest
    public static void cachedLookupAgreesWithContextChanges(GameTestHelper helper) throws Exception {
        noOreDictionary();
        try {
            RecipeRegistry<?>[] holder = new RecipeRegistry<?>[1];
            RecipeRegistries registries = lookupSetup(holder);
            @SuppressWarnings("unchecked")
            RecipeRegistry<Input> reg = (RecipeRegistry<Input>) holder[0];
            reg.add(RecipeId.of("test", "bloomery/general"), Input.of(ore), RecipeSource.NATIVE);
            reg.add(RecipeId.of("test", "bloomery/hot"), Input.of(ore), RecipeSource.NATIVE, 10);
            registries.finishLoading();

            RecipeLookup<Input> lookup = new RecipeLookup<>(reg);
            int[] temperature = { 100 };
            RecipeLookup.Checker<Input> checker = e -> {
                if (e.getId().getPath().equals("bloomery/hot") && temperature[0] < 500) {
                    return CheckResult.failure(CheckResult.Reason.temperature(temperature[0], 500));
                }
                return CheckResult.success(CraftPlan.builder(e.getId(), 1, 2).build());
            };
            Set<Object> keys = RecipeLookup.keysOf(new ItemStack(ore));
            assertEquals("bloomery/general", lookup.find(keys, checker).getPlan().getRecipeId().getPath());
            temperature[0] = 600; // same inputs, same cache, hotter forge
            assertEquals("bloomery/hot", lookup.find(keys, checker).getPlan().getRecipeId().getPath());
        } finally {
            restoreOreDictionary();
        }
        helper.succeed();
    }

    @GameTest
    public static void lookupReportsMostUsefulFailure(GameTestHelper helper) throws Exception {
        noOreDictionary();
        try {
            RecipeRegistry<?>[] holder = new RecipeRegistry<?>[1];
            RecipeRegistries registries = lookupSetup(holder);
            @SuppressWarnings("unchecked")
            RecipeRegistry<Input> reg = (RecipeRegistry<Input>) holder[0];
            reg.add(RecipeId.of("test", "bloomery/a"), Input.of(ore), RecipeSource.NATIVE);
            registries.finishLoading();
            RecipeLookup<Input> lookup = new RecipeLookup<>(reg);
            CheckResult result = lookup.find(
                    RecipeLookup.keysOf(new ItemStack(ore)),
                    e -> CheckResult.failure(CheckResult.Reason.temperature(10, 500)));
            assertEquals(CheckResult.Reason.temperature(10, 500), result.getReason());
            assertSame(
                    CheckResult.Reason.NO_RECIPE,
                    lookup.find(RecipeLookup.keysOf(new ItemStack(bar)), e -> null).getReason());
        } finally {
            restoreOreDictionary();
        }
        helper.succeed();
    }

    @GameTest
    public static void removedRecipeIsNotServedFromCache(GameTestHelper helper) throws Exception {
        noOreDictionary();
        try {
            RecipeRegistry<?>[] holder = new RecipeRegistry<?>[1];
            RecipeRegistries registries = lookupSetup(holder);
            @SuppressWarnings("unchecked")
            RecipeRegistry<Input> reg = (RecipeRegistry<Input>) holder[0];
            RecipeId id = RecipeId.of("test", "bloomery/a");
            reg.add(id, Input.of(ore), RecipeSource.NATIVE);
            registries.finishLoading();
            RecipeLookup<Input> lookup = new RecipeLookup<>(reg);
            Set<Object> keys = RecipeLookup.keysOf(new ItemStack(ore));
            assertEquals(1, lookup.candidates(keys).size());
            registries.beginReload();
            reg.remove(id);
            registries.publish();
            assertEquals(Collections.emptyList(), lookup.candidates(keys));
        } finally {
            restoreOreDictionary();
        }
        helper.succeed();
    }

    // endregion
}
