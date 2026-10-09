package minefantasy.mf2.api.crafting;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.material.CustomMaterial;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.RecipeEntry;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.api.recipe.RecipeRegistrationException;
import minefantasy.mf2.api.recipe.RecipeRegistry;
import minefantasy.mf2.api.recipe.RecipeSource;
import minefantasy.mf2.util.MFLogUtil;

/**
 * Registration of the recipes mods declare in code: their stable ids, named after what a recipe takes or makes, and the
 * legacy input descriptions the old registration methods accept. The registries themselves are in {@link MFRecipes}.
 */
public final class NativeRecipes {

    public static final String NAMESPACE = "minefantasy2";

    private NativeRecipes() {}

    private static final ThreadLocal<String> VARIANT = new ThreadLocal<>();

    /**
     * Registers a native grid recipe under an id derived from what it makes
     * ({@code anvil/modid.item[.meta][.material]}): a grid has no single input to name it after. Grid recipes are not
     * indexed; the bench checks every one in order.
     *
     * Another recipe for the same result needs a {@link #variant} name.
     */
    public static <R> RecipeEntry<R> addGrid(RecipeRegistry<R> registry, ItemStack output, R recipe, int priority) {
        return registry.add(nativeId(registry, output), recipe, RecipeSource.NATIVE, priority);
    }

    /**
     * Names the native recipes registered until the scope closes: their ids get {@code .name} after what they make or
     * take. Use it for alternative recipes of the same thing, so each keeps its id whatever else is registered.
     *
     * <pre>
     * try (NativeRecipes.Variant v = NativeRecipes.variant("from_ore")) {
     *     KnowledgeListMF.addAnvilRecipe(...);
     * }
     * </pre>
     */
    public static Variant variant(String name) {
        String previous = VARIANT.get();
        VARIANT.set(previous == null ? sanitize(name) : previous + "." + sanitize(name));
        return () -> VARIANT.set(previous);
    }

    /** A {@link #variant} named after an item, for recipes generated per source item. */
    public static Variant variantOf(Object source) {
        return variant(variantName(source));
    }

    /** The stable source key used by {@link #variantOf(Object)}, useful for skipping duplicate generated inputs. */
    public static String variantName(Object source) {
        return describe(source);
    }

    /**
     * Registers one recipe per distinct source item, for recipes generated from an ore name or other list another mod
     * may add to: the preferred source (or, when it is not listed, the first) under the plain id, every other one
     * inside a {@link #variantOf} scope named after it, and a source listed twice only once. So a second item under the
     * same ore name neither clashes with the first recipe nor renames it.
     */
    public static void perSource(List<ItemStack> sources, ItemStack preferred, Consumer<ItemStack> register) {
        String base = null;
        if (preferred != null) {
            String wanted = variantName(preferred);
            for (ItemStack source : sources) {
                if (variantName(source).equals(wanted)) base = wanted;
            }
        }
        if (base == null && !sources.isEmpty()) base = variantName(sources.get(0));
        Set<String> seen = new HashSet<>();
        for (ItemStack source : sources) {
            String name = variantName(source);
            if (!seen.add(name)) continue;
            if (name.equals(base)) {
                register.accept(source);
            } else {
                try (Variant v = variantOf(source)) {
                    register.accept(source);
                }
            }
        }
    }

    /**
     * Registers one recipe per distinct source item, each inside a {@link #variantOf} scope named after it, and a
     * source listed twice only once.
     */
    public static void eachSource(List<ItemStack> sources, Consumer<ItemStack> register) {
        Set<String> seen = new HashSet<>();
        for (ItemStack source : sources) {
            if (!seen.add(variantName(source))) continue;
            try (Variant v = variantOf(source)) {
                register.accept(source);
            }
        }
    }

    /** The scope of {@link #variant}. */
    public interface Variant extends AutoCloseable {

        @Override
        void close();
    }

    public static RecipeId id(String path) {
        return RecipeId.of(NAMESPACE, path);
    }

    /**
     * An id derived from an item's registry name, for recipes generated per item: {@code station/modid.name}.
     */
    public static RecipeId idFor(String station, Item item) {
        return id(station + "/" + nameOf(item));
    }

    /**
     * Converts a legacy input description: an {@link Item} or {@link Block} (any metadata), an {@link ItemStack} (its
     * metadata and main material; the size is ignored) or an ore name.
     */
    public static Input input(Object input) {
        if (input instanceof Input) {
            return (Input) input;
        }
        if (input instanceof Item) {
            return Input.of((Item) input);
        }
        if (input instanceof Block) {
            return Input.of((Block) input);
        }
        if (input instanceof String) {
            return Input.ore((String) input);
        }
        if (input instanceof ItemStack && ((ItemStack) input).getItem() != null) {
            ItemStack stack = (ItemStack) input;
            Input result = Input.of(stack.getItem(), stack.getItemDamage());
            NBTTagCompound materials = CustomMaterial.getNBT(stack, false);
            if (materials != null && materials.hasKey(CustomToolHelper.slot_main)) {
                result = result.material(materials.getString(CustomToolHelper.slot_main));
            }
            return result;
        }
        throw new IllegalArgumentException("Not a recipe input: " + input);
    }

    /**
     * Registers a native recipe under an id derived from its input ({@code station/modid.item[.meta][.material]}).
     * Another recipe for the same input needs a {@link #variant} name.
     */
    public static <R> RecipeEntry<R> addNative(RecipeRegistry<R> registry, Object input, R recipe) {
        return registry.add(nativeId(registry, input), recipe, RecipeSource.NATIVE);
    }

    /** The id derived from the input alone, without a variant. */
    public static RecipeId nativeBaseId(RecipeRegistry<?> registry, Object input) {
        return id(registry.getStation() + "/" + describe(input));
    }

    /**
     * Registers a native entry under the id derived from its input alone, replacing (in place) an entry already there:
     * for per-item settings where a later registration overrides an earlier one.
     */
    public static <R> RecipeEntry<R> setNative(RecipeRegistry<R> registry, Object input, R recipe, int priority) {
        RecipeId id = id(registry.getStation() + "/" + describe(input));
        if (registry.containsWorking(id)) {
            return registry.replace(id, recipe, RecipeSource.NATIVE, priority);
        }
        return registry.add(id, recipe, RecipeSource.NATIVE, priority);
    }

    /**
     * The id of a native recipe: the station, then what it is named after (its input, or its output for a grid), then
     * the {@link #variant} name in scope. It never depends on registration order, so a second unnamed recipe for the
     * same thing is refused rather than numbered: name alternative recipes with {@link #variant}.
     */
    public static RecipeId nativeId(RecipeRegistry<?> registry, Object target) {
        String named = VARIANT.get();
        String path = registry.getStation() + "/" + describe(target) + (named == null ? "" : "." + named);
        String owner = registeringMod();
        if (!NAMESPACE.equals(owner)) {
            return addonId(registry, owner, path);
        }
        RecipeId id = id(path);
        if (registry.containsWorking(id)) {
            throw new RecipeRegistrationException(
                    "Native recipe " + id
                            + " already exists; register alternative recipes for the same thing"
                            + " inside NativeRecipes.variant(\"name\") so each keeps its own id");
        }
        return id;
    }

    private static String describe(Object input) {
        if (input instanceof Item) {
            return nameOf((Item) input);
        }
        if (input instanceof Block) {
            return nameOf(Item.getItemFromBlock((Block) input));
        }
        if (input instanceof ItemStack) {
            ItemStack stack = (ItemStack) input;
            String name = nameOf(stack.getItem());
            if (stack.getItemDamage() != OreDictionary.WILDCARD_VALUE) {
                name += "." + stack.getItemDamage();
            }
            // Generated recipes differ by material alone, so the materials are part of what the recipe is about
            NBTTagCompound materials = CustomMaterial.getNBT(stack, false);
            if (materials != null) {
                for (String slot : new String[] { CustomToolHelper.slot_main, CustomToolHelper.slot_haft }) {
                    if (materials.hasKey(slot)) {
                        name += "." + sanitize(materials.getString(slot).toLowerCase());
                    }
                }
            }
            return name;
        }
        if (input instanceof String) {
            return "ore." + sanitize((String) input);
        }
        throw new IllegalArgumentException("Cannot name a recipe after " + input);
    }

    private static String nameOf(Item item) {
        String name = item == null ? null : Item.itemRegistry.getNameForObject(item);
        if (name == null) {
            throw new IllegalArgumentException("Item is not registered: " + item);
        }
        return sanitize(name.replace(':', '.'));
    }

    /**
     * The mod registering a recipe right now, as a recipe id namespace. Outside a mod's loading stage (game tests,
     * reloads) there is none, and the recipe counts as MineFantasy's own.
     */
    private static String registeringMod() {
        ModContainer mod = Loader.instance().activeModContainer();
        return mod == null ? NAMESPACE : sanitize(mod.getModId());
    }

    /**
     * Another mod's recipe: under that mod's namespace, so it never takes or clashes with a MineFantasy id. A second
     * unnamed recipe for the same thing is numbered with a warning instead of stopping the game: the addon cannot be
     * fixed by the player, and its numbered ids only depend on its own registration order.
     */
    private static RecipeId addonId(RecipeRegistry<?> registry, String owner, String path) {
        RecipeId id = RecipeId.of(owner, path);
        for (int n = 2; registry.containsWorking(id); n++) {
            id = RecipeId.of(owner, path + "." + n);
        }
        if (!id.getPath().equals(path)) {
            MFLogUtil.warnOnce(
                    "addon_recipe_id|" + id,
                    "{} registered another recipe for {} without NativeRecipes.variant; it was numbered {}",
                    owner,
                    path,
                    id);
        }
        return id;
    }

    private static String sanitize(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9_.-]", "_");
    }
}
