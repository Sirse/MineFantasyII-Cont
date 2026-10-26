package minefantasy.mf2.api.recipe;

import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.gametest.TestItems.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.item.ItemStack;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.cooking.CookRecipe;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.NativeRecipes;

@GameTestHolder("minefantasy2")
public class RecipeRegistryTest {

    /** Test recipe: an input key and an output name. */
    private static final class Rec {

        final String input;
        final String output;

        Rec(String input, String output) {
            this.input = input;
            this.output = output;
        }
    }

    private static RecipeRegistries registries;
    private static RecipeRegistry<Rec> bloomery;
    private static RecipeRegistry<Rec> salvage;

    private static RecipeId id(String path) {
        return RecipeId.of("test", path);
    }

    private static List<String> ids(List<? extends RecipeEntry<?>> entries) {
        return entries.stream().map(e -> e.getId().getPath()).collect(Collectors.toList());
    }

    private static void setUp() {
        registries = new RecipeRegistries();
        bloomery = registries.create("bloomery", r -> r.input == null ? null : Collections.singleton(r.input));
        salvage = registries.create("salvage", r -> Collections.singleton(r.input));
    }

    // region ids and operations

    @GameTest
    public static void recipeIdParsesAndValidates(GameTestHelper helper) throws Exception {
        setUp();
        try {
            RecipeId parsed = RecipeId.parse("minefantasy2:bloomery/iron_ore");
            assertEquals("minefantasy2", parsed.getNamespace());
            assertEquals("bloomery", parsed.getStation());
            assertEquals(RecipeId.of("minefantasy2", "bloomery/iron_ore"), parsed);
            for (String bad : new String[] { "bloomery/iron", "mf:iron", "MF:bloomery/iron", "mf:bloomery/Iron" }) {
                try {
                    RecipeId.parse(bad);
                    fail("accepted " + bad);
                } catch (IllegalArgumentException expected) {}
            }
        } finally {

        }
        helper.succeed();
    }

    @GameTest
    public static void addRejectsDuplicateId(GameTestHelper helper) throws Exception {
        setUp();
        try {
            assertThrows(RecipeRegistrationException.class, () -> {
                bloomery.add(id("bloomery/a"), new Rec("ore", "bar"), RecipeSource.NATIVE);
                bloomery.add(id("bloomery/a"), new Rec("ore", "bar2"), RecipeSource.NATIVE);
            });
        } finally {

        }
        helper.succeed();
    }

    @GameTest
    public static void replaceRejectsMissingId(GameTestHelper helper) throws Exception {
        setUp();
        try {
            assertThrows(RecipeRegistrationException.class, () -> {
                bloomery.replace(id("bloomery/a"), new Rec("ore", "bar"), RecipeSource.NATIVE, 0);
            });
        } finally {

        }
        helper.succeed();
    }

    @GameTest
    public static void removeRejectsMissingId(GameTestHelper helper) throws Exception {
        setUp();
        try {
            assertThrows(RecipeRegistrationException.class, () -> { bloomery.remove(id("bloomery/a")); });
        } finally {

        }
        helper.succeed();
    }

    @GameTest
    public static void idMustNameTheStation(GameTestHelper helper) throws Exception {
        setUp();
        try {
            assertThrows(RecipeRegistrationException.class, () -> {
                bloomery.add(id("quern/a"), new Rec("ore", "bar"), RecipeSource.NATIVE);
            });
        } finally {

        }
        helper.succeed();
    }

    @GameTest
    public static void removeWhereReportsAffectedIds(GameTestHelper helper) throws Exception {
        setUp();
        try {
            bloomery.add(id("bloomery/a"), new Rec("ore", "bar"), RecipeSource.NATIVE);
            bloomery.add(id("bloomery/b"), new Rec("sand", "glass"), RecipeSource.NATIVE);
            bloomery.add(id("bloomery/c"), new Rec("ore2", "bar"), RecipeSource.NATIVE);
            List<RecipeId> removed = bloomery.removeWhere(e -> e.getRecipe().output.equals("bar"));
            assertEquals(Arrays.asList(id("bloomery/a"), id("bloomery/c")), removed);
            registries.finishLoading();
            assertEquals(Collections.singletonList("bloomery/b"), ids(bloomery.published().all()));
        } finally {

        }
        helper.succeed();
    }

    // endregion

    // region transactions

    @GameTest
    public static void failedPartDiscardsWholeTransaction(GameTestHelper helper) throws Exception {
        setUp();
        try {
            salvage.add(id("salvage/sword"), new Rec("sword", "bar"), RecipeSource.NATIVE);
            try (RecipeTransaction tx = registries.begin(RecipeSource.script(""))) {
                tx.add(bloomery, id("bloomery/sword"), new Rec("hunk", "sword"), 0);
                tx.add(salvage, id("salvage/sword"), new Rec("sword", "hunk"), 0); // duplicate
                tx.commit();
                fail("duplicate accepted");
            } catch (RecipeRegistrationException expected) {}
            registries.finishLoading();
            assertFalse(bloomery.published().contains(id("bloomery/sword")));
            assertEquals("bar", salvage.published().get(id("salvage/sword")).getRecipe().output);
        } finally {

        }
        helper.succeed();
    }

    @GameTest
    public static void uncommittedTransactionChangesNothing(GameTestHelper helper) throws Exception {
        setUp();
        try {
            try (RecipeTransaction tx = registries.begin(RecipeSource.NATIVE)) {
                tx.add(bloomery, id("bloomery/a"), new Rec("ore", "bar"), 0);
            }
            registries.finishLoading();
            assertEquals(0, bloomery.published().size());
        } finally {

        }
        helper.succeed();
    }

    @GameTest
    public static void transactionSeesItsOwnOperations(GameTestHelper helper) throws Exception {
        setUp();
        try {
            try (RecipeTransaction tx = registries.begin(RecipeSource.NATIVE)) {
                tx.add(bloomery, id("bloomery/a"), new Rec("ore", "bar"), 0);
                tx.replace(bloomery, id("bloomery/a"), new Rec("ore", "bar2"), 0);
                tx.remove(bloomery, id("bloomery/a"));
                tx.add(bloomery, id("bloomery/a"), new Rec("ore", "bar3"), 0);
                tx.commit();
            }
            registries.finishLoading();
            assertEquals("bar3", bloomery.published().get(id("bloomery/a")).getRecipe().output);
        } finally {

        }
        helper.succeed();
    }

    @GameTest
    public static void writesOutsideLoadingOrReloadAreRejected(GameTestHelper helper) throws Exception {
        setUp();
        try {
            assertThrows(RecipeRegistrationException.class, () -> {
                registries.finishLoading();
                bloomery.add(id("bloomery/a"), new Rec("ore", "bar"), RecipeSource.NATIVE);
            });
        } finally {

        }
        helper.succeed();
    }

    // endregion

    // region order and lookup

    @GameTest
    public static void higherPriorityFirstThenRegistrationOrder(GameTestHelper helper) throws Exception {
        setUp();
        try {
            bloomery.add(id("bloomery/general"), new Rec("ore", "bar"), RecipeSource.NATIVE);
            bloomery.add(id("bloomery/special"), new Rec("ore", "good_bar"), RecipeSource.NATIVE, 10);
            bloomery.add(id("bloomery/late"), new Rec("ore", "bar"), RecipeSource.NATIVE);
            registries.finishLoading();
            assertEquals(
                    Arrays.asList("bloomery/special", "bloomery/general", "bloomery/late"),
                    ids(bloomery.published().candidates(Collections.singleton("ore"))));
        } finally {

        }
        helper.succeed();
    }

    @GameTest
    public static void replaceKeepsPosition(GameTestHelper helper) throws Exception {
        setUp();
        try {
            bloomery.add(id("bloomery/a"), new Rec("ore", "bar"), RecipeSource.NATIVE);
            bloomery.add(id("bloomery/b"), new Rec("ore", "bar"), RecipeSource.NATIVE);
            bloomery.replace(id("bloomery/a"), new Rec("ore", "better"), RecipeSource.NATIVE, 0);
            registries.finishLoading();
            assertEquals(Arrays.asList("bloomery/a", "bloomery/b"), ids(bloomery.published().all()));
        } finally {

        }
        helper.succeed();
    }

    @GameTest
    public static void candidatesMergeKeysWithoutDuplicatesAndKeepUnindexed(GameTestHelper helper) throws Exception {
        setUp();
        try {
            bloomery.add(id("bloomery/a"), new Rec("ore", "bar"), RecipeSource.NATIVE);
            bloomery.add(id("bloomery/b"), new Rec("sand", "glass"), RecipeSource.NATIVE);
            bloomery.add(id("bloomery/any"), new Rec(null, "slag"), RecipeSource.NATIVE);
            registries.finishLoading();
            RecipeSnapshot<Rec> snapshot = bloomery.published();
            assertEquals(
                    Arrays.asList("bloomery/a", "bloomery/b", "bloomery/any"),
                    ids(snapshot.candidates(Arrays.asList("ore", "sand", "ore"))));
            assertEquals(
                    Collections.singletonList("bloomery/any"),
                    ids(snapshot.candidates(Collections.singleton("x"))));
        } finally {

        }
        helper.succeed();
    }

    @GameTest
    public static void sameStateGivesSameOrder(GameTestHelper helper) throws Exception {
        setUp();
        try {
            for (int i = 0; i < 20; i++) {
                bloomery.add(id("bloomery/r" + i), new Rec("ore", "bar" + i), RecipeSource.NATIVE, i % 3);
            }
            registries.finishLoading();
            List<String> first = ids(bloomery.published().candidates(Collections.singleton("ore")));
            registries.beginReload();
            registries.publish();
            assertEquals(first, ids(bloomery.published().candidates(Collections.singleton("ore"))));
        } finally {

        }
        helper.succeed();
    }

    // endregion

    // region publication and reload

    @GameTest
    public static void draftInvisibleUntilPublished(GameTestHelper helper) throws Exception {
        setUp();
        try {
            bloomery.add(id("bloomery/a"), new Rec("ore", "bar"), RecipeSource.NATIVE);
            registries.finishLoading();
            RecipeSnapshot<Rec> before = bloomery.published();

            registries.beginReload();
            bloomery.add(id("bloomery/script"), new Rec("ore", "gold"), RecipeSource.script(""), 5);
            bloomery.remove(id("bloomery/a"));
            assertSame(before, bloomery.published());
            assertTrue(bloomery.published().contains(id("bloomery/a")));

            registries.publish();
            assertEquals(Collections.singletonList("bloomery/script"), ids(bloomery.published().all()));
        } finally {

        }
        helper.succeed();
    }

    @GameTest
    public static void publicationSwapsAllStationsWithOneGeneration(GameTestHelper helper) throws Exception {
        setUp();
        try {
            registries.finishLoading();
            long generation = registries.getGeneration();
            registries.beginReload();
            bloomery.add(id("bloomery/a"), new Rec("ore", "bar"), RecipeSource.script(""));
            salvage.add(id("salvage/a"), new Rec("bar", "ore"), RecipeSource.script(""));
            registries.publish();
            assertEquals(generation + 1, registries.getGeneration());
            assertEquals(registries.getGeneration(), bloomery.published().getGeneration());
            assertEquals(registries.getGeneration(), salvage.published().getGeneration());
        } finally {

        }
        helper.succeed();
    }

    @GameTest
    public static void repeatedReloadDoesNotDuplicate(GameTestHelper helper) throws Exception {
        setUp();
        try {
            bloomery.add(id("bloomery/native"), new Rec("ore", "bar"), RecipeSource.NATIVE);
            registries.finishLoading();
            for (int i = 0; i < 3; i++) {
                registries.beginReload();
                bloomery.add(id("bloomery/script"), new Rec("ore", "gold"), RecipeSource.script(""));
                registries.publish();
            }
            assertEquals(Arrays.asList("bloomery/native", "bloomery/script"), ids(bloomery.published().all()));
        } finally {

        }
        helper.succeed();
    }

    @GameTest
    public static void reloadStartsFromBaseSoRemovedNativeRecipesReturn(GameTestHelper helper) throws Exception {
        setUp();
        try {
            bloomery.add(id("bloomery/native"), new Rec("ore", "bar"), RecipeSource.NATIVE);
            registries.finishLoading();
            registries.beginReload();
            bloomery.remove(id("bloomery/native"));
            registries.publish();
            assertNull(bloomery.published().get(id("bloomery/native")));
            assertTrue(bloomery.published().candidates(Collections.singleton("ore")).isEmpty());

            registries.beginReload();
            registries.publish();
            assertNotNull(bloomery.published().get(id("bloomery/native")));
        } finally {

        }
        helper.succeed();
    }

    @GameTest
    public static void abortedReloadKeepsPublishedSet(GameTestHelper helper) throws Exception {
        setUp();
        try {
            bloomery.add(id("bloomery/native"), new Rec("ore", "bar"), RecipeSource.NATIVE);
            registries.finishLoading();
            RecipeSnapshot<Rec> before = bloomery.published();
            registries.beginReload();
            bloomery.remove(id("bloomery/native"));
            registries.beginReload(); // the previous reload never published
            registries.abortReload();
            assertSame(before, bloomery.published());
        } finally {

        }
        helper.succeed();
    }

    @GameTest
    public static void publishedListsAreReadOnly(GameTestHelper helper) throws Exception {
        setUp();
        try {
            assertThrows(UnsupportedOperationException.class, () -> {
                bloomery.add(id("bloomery/a"), new Rec("ore", "bar"), RecipeSource.NATIVE);
                registries.finishLoading();
                bloomery.published().all().clear();
            });
        } finally {

        }
        helper.succeed();
    }

    // endregion

    @GameTest
    public static void setSeesTheTransactionsOwnOperations(GameTestHelper helper) throws Exception {
        setUp();
        bloomery.add(id("bloomery/old"), new Rec("ore", "bar"), RecipeSource.NATIVE);
        try (RecipeTransaction tx = registries.begin(RecipeSource.NATIVE)) {
            // A second set of a new id replaces the first
            tx.set(bloomery, id("bloomery/new"), new Rec("ore", "first"), 0);
            tx.set(bloomery, id("bloomery/new"), new Rec("ore", "second"), 0);
            // A set after a removal adds the id again
            tx.remove(bloomery, id("bloomery/old"));
            tx.set(bloomery, id("bloomery/old"), new Rec("ore", "again"), 0);
            tx.commit();
        }
        registries.finishLoading();
        assertEquals("second", bloomery.published().get(id("bloomery/new")).getRecipe().output);
        assertEquals("again", bloomery.published().get(id("bloomery/old")).getRecipe().output);
        helper.succeed();
    }

    // region metadata

    @GameTest
    public static void metadataValidatesTypeAndValue(GameTestHelper helper) throws Exception {
        setUp();
        try {
            RecipeMetadataKey<Integer> tier = RecipeMetadataKey.create("test:tier", Integer.class, v -> v >= 0, "tier");
            RecipeMetadata meta = RecipeMetadata.builder().put(tier, 2).build();
            assertEquals(Integer.valueOf(2), meta.get(tier));
            try {
                RecipeMetadata.builder().put(tier, -1);
                fail("negative tier accepted");
            } catch (IllegalArgumentException expected) {}
            try {
                RecipeMetadataKey.create("test:tier", Integer.class, "tier");
                fail("duplicate key accepted");
            } catch (IllegalArgumentException expected) {}
        } finally {

        }
        helper.succeed();
    }

    // endregion

    // region registration and lookup against a quern

    private static RecipeId quernId(String path) {
        return RecipeId.of("test", "quern/" + path);
    }

    private static void noOreNames() {
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

    private static ProcessRecipe grind(int bars) {
        return ProcessRecipe.of(Input.of(ore), new ItemStack(bar, bars));
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
    public static void removeWhereSeesStagedAddsAndReplaces(GameTestHelper helper) throws Exception {
        noOreNames();
        try {
            RecipeRegistries registries = new RecipeRegistries();
            RecipeRegistry<String> quern = registries.create("quern", r -> null);
            quern.add(quernId("old"), "old", RecipeSource.NATIVE);
            try (RecipeTransaction tx = registries.begin(RecipeSource.NATIVE)) {
                tx.add(quern, quernId("new"), "new", 0);
                tx.replace(quern, quernId("old"), "replaced", 0);
                List<RecipeId> removed = tx.removeWhere(quern, e -> !e.getRecipe().equals("old"));
                assertEquals(2, removed.size());
                tx.commit();
            }
            registries.finishLoading();
            assertEquals(0, quern.published().size());
        } finally {
            OreNames.set(null);
        }
        helper.succeed();
    }

    @GameTest
    public static void lookupSkipsRecipesTheStackCannotPayFor(GameTestHelper helper) throws Exception {
        noOreNames();
        try {
            RecipeRegistries registries = new RecipeRegistries();
            RecipeRegistry<ProcessRecipe> quern = registries.create("quern", ProcessRecipe::indexKeys);
            quern.add(
                    quernId("four"),
                    ProcessRecipe.of(Input.of(ore).amount(4), new ItemStack(bar, 2)),
                    RecipeSource.NATIVE,
                    5);
            quern.add(quernId("one"), ProcessRecipe.of(Input.of(ore), new ItemStack(bar)), RecipeSource.NATIVE);
            registries.finishLoading();

            assertEquals("quern/one", MFRecipes.find(quern, new ItemStack(ore, 1)).getId().getPath());
            assertEquals("quern/four", MFRecipes.find(quern, new ItemStack(ore, 4)).getId().getPath());
            assertTrue(MFRecipes.accepts(quern, new ItemStack(ore, 1)));
            assertNull(MFRecipes.find(quern, new ItemStack(bar, 1)));
        } finally {
            OreNames.set(null);
        }
        helper.succeed();
    }

    @GameTest
    public static void registryRefusesInputsItsStationWouldIgnore(GameTestHelper helper) throws Exception {
        noOreNames();
        try {
            RecipeRegistries registries = new RecipeRegistries();
            RecipeRegistry<ProcessRecipe> rack = registries
                    .create("quern", ProcessRecipe::indexKeys, InputSupport.ONE_CONSUMED.of(ProcessRecipe::getInput));
            rack.add(quernId("one"), ProcessRecipe.of(Input.of(ore), new ItemStack(bar)), RecipeSource.NATIVE);
            for (Input refused : new Input[] { Input.of(ore).amount(2), Input.of(ore).usage(Usage.CATALYST) }) {
                try {
                    rack.add(quernId("bad"), ProcessRecipe.of(refused, new ItemStack(bar)), RecipeSource.NATIVE);
                    fail("accepted " + refused);
                } catch (RecipeRegistrationException expected) {
                    assertTrue(expected.getMessage().contains("refused"));
                }
            }
            assertFalse(rack.containsWorking(quernId("bad")));
        } finally {
            OreNames.set(null);
        }
        helper.succeed();
    }

    @GameTest
    public static void nativeIdsDoNotDependOnRegistrationOrder(GameTestHelper helper) throws Exception {
        noOreNames();
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
            OreNames.set(null);
        }
        helper.succeed();
    }

    @GameTest
    public static void registrationRefusesBadValuesNamingIdAndSource(GameTestHelper helper) throws Exception {
        noOreNames();
        try {
            RecipeRegistries registries = new RecipeRegistries();
            RecipeRegistry<ProcessRecipe> quern = registries.create("quern", ProcessRecipe::indexKeys);
            RecipeRegistry<minefantasy.mf2.api.cooking.CookRecipe> cooking = registries
                    .create("cooking", minefantasy.mf2.api.cooking.CookRecipe::indexKeys);
            RecipeRegistry<Object> carpenter = registries.create("carpenter", r -> null);

            assertRefused(
                    () -> quern.add(
                            quernId("unknown"),
                            ProcessRecipe.of(Input.of(ore).material("unobtainium"), new ItemStack(bar)),
                            RecipeSource.script("test.zs")),
                    "test:quern/unknown (SCRIPT(test.zs))",
                    "unknown material 'unobtainium'");
            assertRefused(
                    () -> cooking.add(
                            RecipeId.of("test", "cooking/cold"),
                            CookRecipe.builder(Input.of(ore), new ItemStack(bar)).temperature(300, 100).time(20)
                                    .burnTime(0).canBurn(false).build(),
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
            OreNames.set(null);
        }
        helper.succeed();
    }

    // endregion
}
