package minefantasy.mf2.api.recipe;

import static minefantasy.mf2.gametest.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.crafting.NativeRecipes;

/**
 * Recipes generated per item of an ore name: another mod adding an item under the name, or the same item twice, must
 * neither crash registration nor rename the recipe already there.
 */
@GameTestHolder("minefantasy2")
public class PerSourceTest {

    private PerSourceTest() {}

    /** A registry of its own, still loading, so ids can be asked for. */
    private static final RecipeRegistry<Object> REGISTRY = new RecipeRegistries().create("anvil", r -> null);

    /** The ids the recipes would get, made for a thing no recipe is registered for. */
    private static List<String> ids(List<ItemStack> sources, ItemStack preferred) {
        final List<String> ids = new ArrayList<>();
        final ItemStack made = new ItemStack(Blocks.bedrock);
        if (preferred == null) {
            NativeRecipes.eachSource(sources, source -> ids.add(NativeRecipes.nativeId(REGISTRY, made).toString()));
        } else {
            NativeRecipes.perSource(
                    sources,
                    preferred,
                    source -> ids.add(NativeRecipes.nativeId(REGISTRY, made).toString()));
        }
        return ids;
    }

    @GameTest
    public static void theOwnIngotKeepsThePlainIdAndOthersGetTheirOwn(GameTestHelper helper) {
        ItemStack own = new ItemStack(Items.iron_ingot);
        ItemStack other = new ItemStack(Items.gold_ingot);
        String plain = NativeRecipes.nativeId(REGISTRY, new ItemStack(Blocks.bedrock)).toString();

        List<String> ids = ids(Arrays.asList(other, own, other.copy()), own);
        assertEquals("one recipe per distinct item", 2, ids.size());
        assertEquals("the material's own ingot keeps the plain id, wherever it is listed", plain, ids.get(1));
        assertTrue("the other one is named after itself", ids.get(0).startsWith(plain + "."));

        List<String> unlisted = ids(Arrays.asList(other, own), new ItemStack(Items.diamond));
        assertEquals("without the preferred item the first keeps the plain id", plain, unlisted.get(0));
        helper.succeed();
    }

    @GameTest
    public static void anItemListedTwiceIsRegisteredOnce(GameTestHelper helper) {
        ItemStack meat = new ItemStack(Items.beef);
        List<String> ids = ids(Arrays.asList(meat, meat.copy(), new ItemStack(Items.porkchop)), null);
        assertEquals(2, ids.size());
        assertTrue("each named after its source", !ids.get(0).equals(ids.get(1)));
        helper.succeed();
    }
}
