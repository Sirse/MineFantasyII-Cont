package minefantasy.mf2.api.heating;

import java.util.Set;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;

import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.NativeRecipes;
import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.material.CustomMaterial;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.RecipeChecks;
import minefantasy.mf2.api.recipe.RecipeEntry;

/**
 * How an item behaves in the forge: the temperatures at which it becomes workable, unstable and ruined. Profiles live
 * in {@link MFRecipes#HEATING}; stacks without one cannot be heated.
 */
public final class Heatable implements RecipeChecks.Validated {

    @Override
    public void validate() {
        RecipeChecks.input("input", input);
        RecipeChecks.temperatures(minTemperature, unstableTemperature, maxTemperature);
    }

    public static final int forgeMaximumMetalHeat = 5000;
    public static final String NBT_Item = "MFHeatable_ItemSave";
    public static final String NBT_ShouldDisplay = "MFHeatable_DisplayTemperature";
    public static final String NBT_CurrentTemp = "MFHeatable_Temperature";
    public static final String NBT_WorkableTemp = "MFHeatable_WorkTemp";
    public static final String NBT_UnstableTemp = "MFHeatable_UnstableTemp";
    public static final String NBT_MaxTemp = "MFHeatable_MaxTemp";
    public static boolean requiresHeating = true;
    /**
     * Hardcore Crafting: Should quencing in inproper sources damage items
     */
    public static boolean HCCquenchRuin = true;
    /**
     * The min heat the ingot must be to forge with mesured in celcius
     */
    private final int minTemperature;
    /**
     * The heat when it becomes unstable mesured in celcius
     */
    private final int unstableTemperature;
    /**
     * The max heat until the ingot is destroyed mesured in celcius
     */
    private final int maxTemperature;
    private final Input input;

    private Heatable(Input input, int min, int unstable, int max) {
        this.input = input;
        this.minTemperature = min;
        this.unstableTemperature = unstable;
        this.maxTemperature = max;
    }

    /** A heat profile; -1 for a temperature takes it from the stack's main material. */
    public static Heatable of(Input input, int min, int unstable, int max) {
        if (input == null) {
            throw new IllegalArgumentException("A heat profile needs an input");
        }
        return new Heatable(input.amount(1), min, unstable, max);
    }

    /**
     * Registers the native heat profile of an item; registering the same item and metadata again replaces it. A profile
     * for one metadata goes before the item's any-metadata profile.
     */
    public static void addItem(ItemStack item, int min, int unstable, int max) {
        if (item == null || item.getItem() == null) {
            return;
        }
        Heatable profile = of(Input.of(item.getItem(), item.getItemDamage()), min, unstable, max);
        int priority = item.getItemDamage() == OreDictionary.WILDCARD_VALUE ? 0 : 1;
        NativeRecipes.setNative(MFRecipes.HEATING, item, profile, priority);
    }

    public static boolean canHeatItem(ItemStack item) {
        return loadStats(item) != null;
    }

    /** The profile the stack heats by, in lookup order, or null. */
    public static Heatable loadStats(ItemStack item) {
        if (item == null || item.getItem() == null) {
            return null;
        }
        for (RecipeEntry<Heatable> entry : MFRecipes.HEATING.published().candidates(Input.lookupKeys(item))) {
            if (entry.getRecipe().input.matches(item)) {
                return entry.getRecipe();
            }
        }
        return null;
    }

    public Input getInput() {
        return input;
    }

    public Set<Object> indexKeys() {
        return input.indexKeys();
    }

    /**
     * 0 = nothing, 1 = soft, 2 = unstable
     */
    public static byte getHeatableStage(ItemStack item) {
        if (item == null || !(item.getItem() instanceof IHotItem)) {
            return 0;
        }
        if (item != null && item.hasTagCompound()) {
            int temp = getTemp(item);
            int work = item.getTagCompound().getInteger(NBT_WorkableTemp);
            int unstable = item.getTagCompound().getInteger(NBT_UnstableTemp);
            if (temp > unstable) return (byte) 2;
            if (temp > work) return (byte) 1;
        }
        return (byte) 0;
    }

    public static int getWorkTemp(ItemStack item) {
        if (item == null || !(item.getItem() instanceof IHotItem)) {
            return 0;
        }
        NBTTagCompound tag = getNBT(item);

        if (tag.hasKey(NBT_WorkableTemp)) return tag.getInteger(NBT_WorkableTemp);

        return 0;
    }

    public static int getUnstableTemp(ItemStack item) {
        if (item == null || !(item.getItem() instanceof IHotItem)) {
            return 0;
        }
        NBTTagCompound tag = getNBT(item);

        if (tag.hasKey(NBT_UnstableTemp)) return tag.getInteger(NBT_UnstableTemp);

        return 0;
    }

    public static int getMaxTemp(ItemStack item) {
        if (item == null || !(item.getItem() instanceof IHotItem)) {
            return 0;
        }
        NBTTagCompound tag = getNBT(item);

        if (tag.hasKey(NBT_MaxTemp)) return tag.getInteger(NBT_MaxTemp);

        return 0;
    }

    public static int getTemp(ItemStack item) {
        if (item == null || !(item.getItem() instanceof IHotItem)) {
            return 0;
        }
        NBTTagCompound tag = getNBT(item);

        if (tag.hasKey(NBT_CurrentTemp)) return tag.getInteger(NBT_CurrentTemp);

        return 0;
    }

    /**
     * Gets a hot item
     *
     * @param item   the hot item
     * @param hazard the amount the source is hazardous (damaging the item): usually a percent dura loss
     * @return what item is heated
     */
    public static ItemStack getQuenchedItem(ItemStack item, float hazard) {
        ItemStack cold = Heatable.getItem(item);

        if (cold == null) {
            return null;
        }
        if (HCCquenchRuin && cold.isItemStackDamageable() && hazard > 0) {
            cold.setItemDamage((int) (cold.getMaxDamage() * hazard / 100F));
        }

        return cold;
    }

    public static ItemStack getItem(ItemStack item) {
        if (item == null || !(item.getItem() instanceof IHotItem)) {
            return null;
        }
        NBTTagCompound tag = getNBT(item);

        if (tag.hasKey(NBT_Item)) {
            return ItemStack.loadItemStackFromNBT(tag.getCompoundTag(NBT_Item));
        }

        return null;
    }

    private static NBTTagCompound getNBT(ItemStack item) {
        if (!item.hasTagCompound()) item.setTagCompound(new NBTTagCompound());
        return item.getTagCompound();
    }

    public static boolean isWorkable(ItemStack inputItem) {
        if (inputItem == null || !(inputItem.getItem() instanceof IHotItem)) {
            return true;
        }
        if (inputItem != null && inputItem.getItem() instanceof IHotItem) {
            return getHeatableStage(inputItem) == 1;
        }
        return true;
    }

    public int getWorkableStat(ItemStack item) {
        if (this.minTemperature == -1) {
            CustomMaterial material = CustomToolHelper.getCustomPrimaryMaterial(item);
            if (material != null) return material.getHeatableStats()[0];
        }
        return this.minTemperature;
    }

    public int getUnstableStat(ItemStack item) {
        if (this.unstableTemperature == -1) {
            CustomMaterial material = CustomToolHelper.getCustomPrimaryMaterial(item);
            if (material != null) return material.getHeatableStats()[1];
        }
        return this.unstableTemperature;
    }

    public int getMaxStat(ItemStack item) {
        if (this.maxTemperature == -1) {
            CustomMaterial material = CustomToolHelper.getCustomPrimaryMaterial(item);
            if (material != null) return material.getHeatableStats()[2];
        }
        return this.maxTemperature;
    }
}
