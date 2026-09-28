package minefantasy.mf2.api;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.oredict.OreDictionary;

import com.google.common.collect.Lists;

import cpw.mods.fml.common.IFuelHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.api.cooking.CookRecipe;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.NativeGridRecipe;
import minefantasy.mf2.api.crafting.NativeRecipes;
import minefantasy.mf2.api.crafting.engineer.ICrossbowPart;
import minefantasy.mf2.api.crafting.refine.QuernRecipes;
import minefantasy.mf2.api.crafting.tanning.TanningRecipe;
import minefantasy.mf2.api.heating.Heatable;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.api.recipe.RecipeEntry;
import minefantasy.mf2.api.refine.Alloy;
import minefantasy.mf2.api.refine.AlloyRecipes;
import minefantasy.mf2.api.refine.BigFurnaceRecipes;
import minefantasy.mf2.util.MFLogUtil;

/**
 * Entry points for other mods. Recipes are registered through builders named after their station ({@link #anvilRecipe},
 * {@link #carpenterRecipe}, {@link #kitchenRecipe}, {@link #alloyRecipe}, {@link #cookingRecipe}, {@link #quernRecipe},
 * {@link #tanningRecipe}, {@link #bigFurnaceRecipe}), in init or postInit; see the package
 * {@code minefantasy.mf2.api.crafting} for how they are named, checked and published. Heat, fuel and crossbow parts
 * register here too.
 */
public class MineFantasyAPI {

    /**
     * This variable saves if MineFantasy is in debug mode
     */
    public static boolean isInDebugMode;
    /**
     * This fuel handler is for MineFantasy equipment, it uses real fuel(like coal) not wood
     */
    private static List<IFuelHandler> fuelHandlers = Lists.newArrayList();

    @SideOnly(Side.CLIENT)
    public static void init() {}

    public static void debugMsg(String msg) {
        MFLogUtil.logDebug(msg);
    }

    public static void removeAllRecipes(Item result) {
        List recipeList = CraftingManager.getInstance().getRecipeList();
        for (int i = recipeList.size() - 1; i >= 0; i--) {
            Object entry = recipeList.get(i);
            if (!(entry instanceof IRecipe)) {
                continue;
            }
            IRecipe recipe = (IRecipe) entry;
            ItemStack output = recipe.getRecipeOutput();
            if (output != null && output.getItem() == result) {
                debugMsg("Removed Recipe for " + output.getDisplayName());
                recipeList.remove(i);
            }
        }
    }

    /** A native anvil recipe, registered by its pattern; see {@link NativeGridRecipe}. */
    public static NativeGridRecipe anvilRecipe(ItemStack result) {
        return NativeGridRecipe.anvil(result);
    }

    public static NativeGridRecipe anvilRecipe(Item result) {
        return NativeGridRecipe.anvil(result);
    }

    /** A native carpenter's bench recipe, registered by its pattern; see {@link NativeGridRecipe}. */
    public static NativeGridRecipe carpenterRecipe(ItemStack result) {
        return NativeGridRecipe.carpenter(result);
    }

    public static NativeGridRecipe carpenterRecipe(Item result) {
        return NativeGridRecipe.carpenter(result);
    }

    /**
     * A native kitchen bench recipe, registered by its pattern; it goes to the carpenter's bench when the kitchen bench
     * is disabled. See {@link NativeGridRecipe}.
     */
    public static NativeGridRecipe kitchenRecipe(ItemStack result) {
        return NativeGridRecipe.kitchen(result);
    }

    /** Registers a native blast furnace recipe; the input may be an item, block, stack or ore name. */
    public static RecipeEntry<ProcessRecipe> addBlastFurnaceRecipe(Object input, ItemStack output) {
        return NativeRecipes
                .addNative(MFRecipes.BLAST_FURNACE, input, ProcessRecipe.of(NativeRecipes.input(input), output));
    }

    public static void registerFuelHandler(IFuelHandler handler) {
        fuelHandlers.add(handler);
    }

    public static int getFuelValue(ItemStack itemStack) {
        int fuelValue = 0;
        for (IFuelHandler handler : fuelHandlers) {
            fuelValue = Math.max(fuelValue, handler.getBurnTime(itemStack));
        }
        return fuelValue;
    }

    /**
     * A native crucible alloy, registered with its ingredients; see
     * {@link minefantasy.mf2.api.refine.AlloyRecipes.Builder}.
     */
    public static AlloyRecipes.Builder alloyRecipe(ItemStack output) {
        return AlloyRecipes.alloy(output);
    }

    /**
     * Adds a custom alloy
     *
     * @param alloy the Alloy to add Use this if you want your alloy to have special properties
     * @see Alloy
     */
    public static void addAlloy(Alloy alloy) {
        AlloyRecipes.addAlloy(alloy);
    }

    /**
     * Allows an item to be heated
     *
     * @param item     the item to heat
     * @param min      the minimum heat to forge with(celcius)
     * @param max      the maximum heat until the item is ruined(celcius)
     * @param unstable when the ingot is unstable(celcius)
     */
    public static void setHeatableStats(ItemStack item, int min, int unstable, int max) {
        Heatable.addItem(item, min, unstable, max);
    }

    /**
     * Allows an item to be heated ignoring subId
     *
     * @param id       the item to heat
     * @param min      the minimum heat to forge with(celcius)
     * @param max      the maximum heat until the item is ruined(celcius)
     * @param unstable when the ingot is unstable(celcius)
     */
    public static void setHeatableStats(Item id, int min, int unstable, int max) {
        Heatable.addItem(new ItemStack(id, 1, OreDictionary.WILDCARD_VALUE), min, unstable, max);
    }

    public static void setHeatableStats(String oredict, int min, int unstable, int max) {
        for (ItemStack item : OreDictionary.getOres(oredict)) {
            setHeatableStats(item, min, unstable, max);
        }
    }

    private static ItemStack convertItem(Object object) {
        if (object instanceof ItemStack) {
            return ((ItemStack) object);
        }
        if (object instanceof Item) {
            return new ItemStack((Item) object, 1, OreDictionary.WILDCARD_VALUE);
        }
        if (object instanceof Block) {
            return new ItemStack((Block) object, 1, OreDictionary.WILDCARD_VALUE);
        }
        return null;
    }

    /**
     * Adds a crossbow part Make sure to do this when adding the item
     */
    public static void registerCrossbowPart(ICrossbowPart part) {
        ICrossbowPart.components.put(part.getComponentType() + part.getID(), part);
    }

    /**
     * For Hardcore Crafting: Can remove smelting recipes
     *
     * @param input can be an "Item", "Block" or "ItemStack"
     */
    public static boolean removeSmelting(Object input) {
        ItemStack object = null;
        if (input instanceof Item) {
            object = new ItemStack((Item) input, 1, 32767);
        } else if (input instanceof Block) {
            object = new ItemStack((Block) input, 1, 32767);
        } else if (input instanceof ItemStack) {
            object = (ItemStack) input;
        }
        if (object != null) {
            return removeFurnaceInput(object);
        }
        return false;
    }

    private static boolean removeFurnaceInput(ItemStack input) {
        Iterator iterator = FurnaceRecipes.smelting().getSmeltingList().entrySet().iterator();
        Entry entry;

        do {
            if (!iterator.hasNext()) {
                return false;
            }

            entry = (Entry) iterator.next();
        } while (!checkMatch(input, (ItemStack) entry.getKey()));

        FurnaceRecipes.smelting().getSmeltingList().remove(entry.getKey());
        return true;
    }

    private static boolean checkMatch(ItemStack item1, ItemStack item2) {
        return item2.getItem() == item1.getItem()
                && (item2.getItemDamage() == 32767 || item2.getItemDamage() == item1.getItemDamage());
    }

    /** A native cooking recipe; see {@link minefantasy.mf2.api.cooking.CookRecipe.Builder}. */
    public static CookRecipe.Builder cookingRecipe(ItemStack input, ItemStack output) {
        return CookRecipe.nativeRecipe(input, output);
    }

    /** A native quern recipe; the input may be an item, block, stack or ore name. */
    public static QuernRecipes.Builder quernRecipe(Object input, ItemStack output) {
        return QuernRecipes.recipe(input, output);
    }

    /** A native big furnace recipe; the input may be an item, block, stack or ore name. */
    public static BigFurnaceRecipes.Builder bigFurnaceRecipe(Object input, ItemStack output) {
        return BigFurnaceRecipes.recipe(input, output);
    }

    /** A native tanning recipe; the input may be an item, block, stack or ore name. */
    public static TanningRecipe.Builder tanningRecipe(Object input, ItemStack output) {
        return TanningRecipe.recipe(input, output);
    }
}
