package minefantasy.mf2.api.recipe;

import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.gametest.TestItems.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.heating.Heatable;
import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.material.CustomMaterial;
import minefantasy.mf2.gametest.TestItems;

@GameTestHolder("minefantasy2")
public class InputTest {

    private static final Item bar = TestItems.metaBar;
    private static final Item ore = TestItems.ore;
    private static final Item hotItem = TestItems.hot;

    /** Ore dictionary stub: item → names. */
    private static final Map<Item, List<String>> ORES = new HashMap<>();

    private static void resetOres() {
        ORES.clear();
        OreNames.set(null);
    }

    private static void useOreStub() {
        OreNames.set(new OreNames() {

            @Override
            public List<String> namesOf(ItemStack stack) {
                List<String> names = ORES.get(stack.getItem());
                return names == null ? Collections.emptyList() : names;
            }

            @Override
            public List<ItemStack> stacksOf(String name) {
                List<ItemStack> out = new ArrayList<>();
                for (Map.Entry<Item, List<String>> e : ORES.entrySet()) {
                    if (e.getValue().contains(name)) {
                        out.add(new ItemStack(e.getKey()));
                    }
                }
                return out;
            }
        });
    }

    private static ItemStack withMaterial(Item item, String material) {
        ItemStack stack = new ItemStack(item);
        CustomMaterial.addMaterial(stack, CustomToolHelper.slot_main, material);
        return stack;
    }

    private static ItemStack heated(ItemStack inside, int size, int temp) {
        ItemStack hot = new ItemStack(hotItem, size);
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagCompound saved = new NBTTagCompound();
        inside.writeToNBT(saved);
        tag.setTag(Heatable.NBT_Item, saved);
        tag.setInteger(Heatable.NBT_CurrentTemp, temp);
        tag.setInteger(Heatable.NBT_WorkableTemp, 100);
        tag.setInteger(Heatable.NBT_UnstableTemp, 1000);
        hot.setTagCompound(tag);
        return hot;
    }

    @GameTest
    public static void metaAndWildcard(GameTestHelper helper) throws Exception {

        try {
            assertTrue(Input.of(bar).matches(new ItemStack(bar, 1, 5)));
            assertTrue(Input.of(bar, 5).matches(new ItemStack(bar, 1, 5)));
            assertFalse(Input.of(bar, 5).matches(new ItemStack(bar, 1, 4)));
            assertFalse(Input.of(bar).matches(new ItemStack(ore)));
        } finally {
            resetOres();
        }
        helper.succeed();
    }

    @GameTest
    public static void matchesIgnoresSizeHasEnoughChecksIt(GameTestHelper helper) throws Exception {

        try {
            Input four = Input.of(bar).amount(4);
            assertTrue(four.matches(new ItemStack(bar, 1)));
            assertFalse(four.hasEnough(new ItemStack(bar, 3)));
            assertTrue(four.hasEnough(new ItemStack(bar, 4)));
        } finally {
            resetOres();
        }
        helper.succeed();
    }

    @GameTest
    public static void foreignNbtDoesNotBreakMatching(GameTestHelper helper) throws Exception {

        try {
            ItemStack named = new ItemStack(bar);
            named.setStackDisplayName("Shiny");
            named.getTagCompound().setString("othermod", "x");
            assertTrue(Input.of(bar).matches(named));
            assertTrue(Input.of(bar).material(Input.MaterialRule.ANY).matches(named));
        } finally {
            resetOres();
        }
        helper.succeed();
    }

    @GameTest
    public static void materialRules(GameTestHelper helper) throws Exception {

        try {
            ItemStack iron = withMaterial(bar, "iron");
            ItemStack plain = new ItemStack(bar);
            assertTrue(Input.of(bar).material("Iron").matches(iron));
            assertFalse(Input.of(bar).material("copper").matches(iron));
            assertFalse(Input.of(bar).material("iron").matches(plain));
            assertTrue(Input.of(bar).material(Input.MaterialRule.NONE).matches(plain));
            assertFalse(Input.of(bar).material(Input.MaterialRule.NONE).matches(iron));
            assertTrue(Input.of(bar).matches(iron));
        } finally {
            resetOres();
        }
        helper.succeed();
    }

    @GameTest
    public static void oreNamesAreReadAtLookupTime(GameTestHelper helper) throws Exception {

        try {
            useOreStub();
            Input ingot = Input.ore("ingotIron");
            assertFalse(ingot.matches(new ItemStack(bar)));
            ORES.put(bar, Collections.singletonList("ingotIron")); // late registration
            assertTrue(ingot.matches(new ItemStack(bar)));
            assertEquals(Collections.singleton("ore:ingotIron"), ingot.indexKeys());
            assertTrue(Input.lookupKeys(new ItemStack(bar)).contains("ore:ingotIron"));
        } finally {
            resetOres();
        }
        helper.succeed();
    }

    @GameTest
    public static void hotInputChecksWrapperAndInner(GameTestHelper helper) throws Exception {

        try {
            useOreStub();
            Input hotIron = Input.hot(Input.of(bar).material("iron").amount(4));
            assertTrue(hotIron.hasEnough(heated(withMaterial(bar, "iron"), 4, 500)));
            assertFalse(hotIron.hasEnough(heated(withMaterial(bar, "iron"), 1, 500)));
            assertFalse(hotIron.matches(heated(withMaterial(bar, "copper"), 4, 500)));
            assertFalse(hotIron.matches(heated(withMaterial(bar, "iron"), 4, 50))); // too cold
            assertFalse(hotIron.matches(withMaterial(bar, "iron"))); // not hot at all
        } finally {
            resetOres();
        }
        helper.succeed();
    }

    @GameTest
    public static void hotInputIsIndexedByInnerItemAndFoundThroughWrapper(GameTestHelper helper) throws Exception {

        try {
            useOreStub();
            Input hotBar = Input.hot(Input.of(bar));
            assertEquals(Collections.singleton((Object) bar), hotBar.indexKeys());
            Set<Object> keys = Input.lookupKeys(heated(new ItemStack(bar), 1, 500));
            assertTrue(keys.contains(hotItem));
            assertTrue(keys.contains(bar));
        } finally {
            resetOres();
        }
        helper.succeed();
    }

    @GameTest
    public static void conditionGetsCopySizedToAmount(GameTestHelper helper) throws Exception {

        try {
            int[] seen = new int[1];
            Input conditional = Input.of(bar).amount(3).where(s -> {
                seen[0] = s.stackSize;
                s.stackSize = 99;
                return true;
            }, "any");
            ItemStack stack = new ItemStack(bar, 10);
            assertTrue(conditional.matches(stack));
            assertEquals(3, seen[0]);
            assertEquals(10, stack.stackSize);
            assertTrue(conditional.isConditional());
        } finally {
            resetOres();
        }
        helper.succeed();
    }

    @GameTest
    public static void examplesAreFreshCopiesWithMaterial(GameTestHelper helper) throws Exception {

        try {
            Input input = Input.of(bar, 2).amount(3).material("steel");
            List<ItemStack> first = input.examples();
            assertEquals(1, first.size());
            assertEquals(3, first.get(0).stackSize);
            assertEquals("steel", CustomMaterial.getNBT(first.get(0), false).getString(CustomToolHelper.slot_main));
            first.get(0).stackSize = 64;
            assertNotSame(first.get(0), input.examples().get(0));
            assertEquals(3, input.examples().get(0).stackSize);
        } finally {
            resetOres();
        }
        helper.succeed();
    }

    @GameTest
    public static void anyOfMatchesEachAlternativeWithItsConstraints(GameTestHelper helper) throws Exception {

        try {
            Input either = Input.anyOf(Input.of(bar).material("iron"), Input.of(ore));
            assertTrue(either.matches(withMaterial(bar, "iron")));
            assertFalse(either.matches(withMaterial(bar, "tin")));
            assertTrue(either.matches(new ItemStack(ore)));
            assertEquals(new java.util.LinkedHashSet<Object>(Arrays.asList(bar, ore)), either.indexKeys());
        } finally {
            resetOres();
        }
        helper.succeed();
    }

    @GameTest
    public static void damagedToolMustBeSingle(GameTestHelper helper) throws Exception {

        try {
            assertThrows(IllegalArgumentException.class, () -> { Input.of(bar).usage(Usage.damage(1)).amount(2); });
        } finally {
            resetOres();
        }
        helper.succeed();
    }

    @GameTest
    public static void transformWorksOnCopies(GameTestHelper helper) throws Exception {

        try {
            Usage cool = Usage.transform(s -> {
                s.stackSize = 1;
                return s;
            });
            ItemStack used = new ItemStack(bar, 5);
            ItemStack result = cool.transform(used);
            assertEquals(1, result.stackSize);
            assertEquals(5, used.stackSize);
        } finally {
            resetOres();
        }
        helper.succeed();
    }
}
