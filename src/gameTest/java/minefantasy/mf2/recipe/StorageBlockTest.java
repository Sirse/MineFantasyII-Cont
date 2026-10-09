package minefantasy.mf2.recipe;

import static minefantasy.mf2.gametest.Assert.*;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapelessRecipes;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.item.list.ComponentListMF;

/**
 * Another mod's ingot under a MineFantasy metal's ore name packs into the storage block like any other, but blocks and
 * bars turn back into MineFantasy's own ingot only.
 */
@GameTestHolder("minefantasy2")
public class StorageBlockTest {

    private StorageBlockTest() {}

    private static ItemStack copperBlock() {
        for (int id = 0; id < BlockListMF.metalBlocks.length; id++) {
            if ("copper".equals(BlockListMF.metalBlocks[id])) {
                return new ItemStack(BlockListMF.storage[id]);
            }
        }
        throw new IllegalStateException("no copper storage block");
    }

    /** What the crafting grid unpacks the block into. */
    private static List<ItemStack> unpacked(ItemStack block) {
        List<ItemStack> outputs = new ArrayList<>();
        for (Object recipe : CraftingManager.getInstance().getRecipeList()) {
            if (recipe instanceof ShapelessRecipes) {
                List<?> inputs = ((ShapelessRecipes) recipe).recipeItems;
                if (inputs.size() == 1 && inputs.get(0) instanceof ItemStack
                        && ((ItemStack) inputs.get(0)).getItem() == block.getItem()) {
                    outputs.add(((IRecipe) recipe).getRecipeOutput());
                }
            }
        }
        return outputs;
    }

    @GameTest
    public static void aStorageBlockUnpacksIntoTheOwnIngotOnly(GameTestHelper helper) {
        List<ItemStack> outputs = unpacked(copperBlock());
        assertEquals("the block unpacks more than one way: " + outputs, 1, outputs.size());
        assertSame(
                "the block unpacks into another mod's ingot",
                ComponentListMF.ingot("copper").getItem(),
                outputs.get(0).getItem());
        helper.succeed();
    }
}
