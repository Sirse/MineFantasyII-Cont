package minefantasy.mf2.integration.minetweaker;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;

import minefantasy.mf2.api.recipe.RecipeEntry;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.api.recipe.RecipeRegistry;
import minefantasy.mf2.api.recipe.RecipeSource;
import minefantasy.mf2.block.tileentity.Stations;
import minetweaker.MineTweakerAPI;
import minetweaker.MineTweakerImplementationAPI;
import minetweaker.runtime.ILogger;
import minetweaker.runtime.IScriptProvider;
import minetweaker.runtime.providers.ScriptProviderCustom;

/**
 * Runs a script through CraftTweaker's own reload, as {@code /mt reload} does, and gives back the errors it logged.
 * {@link #around} puts the server's scripts back afterwards.
 */
final class Scripts {

    /** The body of a script test. */
    interface Body {

        void run() throws Exception;
    }

    private Scripts() {}

    /** A bracket for a test item. */
    static String item(String name) {
        return "<minefantasy2tests:" + name + ">";
    }

    /** A bracket for any item stack, wildcard metadata as {@code *}. */
    static String bracket(ItemStack stack) {
        String name = Item.itemRegistry.getNameForObject(stack.getItem());
        int meta = stack.getItemDamage();
        return "<" + name + (meta == OreDictionary.WILDCARD_VALUE ? ":*" : meta == 0 ? "" : ":" + meta) + ">";
    }

    /** The first recipes the mod registered in the registry, in lookup order. */
    static List<RecipeId> natives(RecipeRegistry<?> registry, int count) {
        List<RecipeId> ids = new ArrayList<>();
        for (RecipeEntry<?> entry : registry.published().all()) {
            if (entry.getSource().getKind() == RecipeSource.Kind.NATIVE && ids.size() < count) {
                ids.add(entry.getId());
            }
        }
        if (ids.size() < count) {
            throw new IllegalStateException(registry.getStation() + " has fewer than " + count + " native recipes");
        }
        return ids;
    }

    /** Reloads with the script made of these lines; returns the errors CraftTweaker logged. */
    static List<String> run(String... lines) {
        return run(false, lines);
    }

    /** Reloads with the script made of these lines; returns the warnings CraftTweaker logged. */
    static List<String> warnings(String... lines) {
        return run(true, lines);
    }

    private static List<String> run(boolean warnings, String... lines) {
        List<String> errors = new ArrayList<>();
        List<String> warned = new ArrayList<>();
        ILogger capture = new ILogger() {

            @Override
            public void logCommand(String message) {}

            @Override
            public void logInfo(String message) {}

            @Override
            public void logWarning(String message) {
                warned.add(message);
            }

            @Override
            public void logError(String message) {
                errors.add(message);
            }

            @Override
            public void logError(String message, Throwable exception) {
                errors.add(exception == null ? message : message + ": " + exception);
            }
        };
        ScriptProviderCustom scripts = new ScriptProviderCustom("minefantasy2tests");
        scripts.add("test.zs", String.join("\n", lines));
        MineTweakerImplementationAPI.logger.addLogger(capture);
        try {
            MineTweakerImplementationAPI.setScriptProvider(scripts);
            MineTweakerImplementationAPI.reload();
        } finally {
            MineTweakerImplementationAPI.logger.removeLogger(capture);
        }
        return warnings ? warned : errors;
    }

    /**
     * The recipe a script line builds, through CraftTweaker's own reload, with the server's scripts back in place
     * afterwards. A test over ticks hands it to {@link Stations#reload} as its layer: a CraftTweaker reload by a test
     * running alongside would otherwise drop it before the station gets to it.
     */
    static <R> R build(RecipeRegistry<R> registry, String id, String... lines) throws Exception {
        IScriptProvider server = serverScripts();
        try {
            List<String> errors = run(lines);
            if (!errors.isEmpty()) {
                throw new IllegalStateException("the script failed: " + errors);
            }
            RecipeEntry<R> entry = registry.published().get(RecipeId.parse(id));
            if (entry == null) {
                throw new IllegalStateException("the script built no " + id);
            }
            return entry.getRecipe();
        } finally {
            MineTweakerImplementationAPI.setScriptProvider(server);
            MineTweakerImplementationAPI.reload();
            Stations.publish();
        }
    }

    /** Runs the body, then reloads the server's own scripts and the running tests' recipes. */
    static void around(GameTestHelper helper, Body body) throws Exception {
        IScriptProvider server = serverScripts();
        try {
            body.run();
        } finally {
            MineTweakerImplementationAPI.setScriptProvider(server);
            MineTweakerImplementationAPI.reload();
            Stations.publish();
        }
        helper.succeed();
    }

    private static IScriptProvider serverScripts() throws Exception {
        Field provider = MineTweakerAPI.tweaker.getClass().getDeclaredField("scriptProvider");
        provider.setAccessible(true);
        return (IScriptProvider) provider.get(MineTweakerAPI.tweaker);
    }
}
