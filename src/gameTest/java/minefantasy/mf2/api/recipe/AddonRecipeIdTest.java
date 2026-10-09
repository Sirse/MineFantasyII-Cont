package minefantasy.mf2.api.recipe;

import static minefantasy.mf2.gametest.Assert.*;

import java.lang.reflect.Field;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import cpw.mods.fml.common.Loader;
import minefantasy.mf2.api.crafting.NativeRecipes;
import minefantasy.mf2.gametest.GameTestMod;

/**
 * Recipes another mod registers through the API: under its own namespace, and a second one for the same thing is
 * numbered rather than stopping the game. MineFantasy's own stay strict, since a duplicate there is a bug to fix.
 */
@GameTestHolder("minefantasy2")
public class AddonRecipeIdTest {

    private AddonRecipeIdTest() {}

    private static final ItemStack MADE = new ItemStack(Blocks.bedrock);

    private interface Check {

        void run(RecipeRegistry<Object> registry);
    }

    /**
     * Runs the check on a fresh registry as if the given mod were loading. FML keeps the loading mod in its load
     * controller, which has no setter.
     */
    private static void as(String modId, Check check) {
        try {
            Field controllerField = Loader.class.getDeclaredField("modController");
            controllerField.setAccessible(true);
            Object controller = controllerField.get(Loader.instance());
            Field active = controller.getClass().getDeclaredField("activeContainer");
            active.setAccessible(true);
            Object previous = active.get(controller);
            active.set(controller, Loader.instance().getIndexedModList().get(modId));
            try {
                check.run(new RecipeRegistries().create("anvil", r -> null));
            } finally {
                active.set(controller, previous);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @GameTest
    public static void anAddonsDuplicateIsNumberedUnderItsOwnNamespace(GameTestHelper helper) {
        as(GameTestMod.MODID, registry -> {
            RecipeId first = NativeRecipes.nativeId(registry, MADE);
            assertEquals("not under the addon's namespace", GameTestMod.MODID, first.getNamespace());
            registry.add(first, new Object(), RecipeSource.NATIVE);

            RecipeId second = NativeRecipes.nativeId(registry, MADE);
            assertEquals("the duplicate was not numbered", first.getPath() + ".2", second.getPath());
        });
        helper.succeed();
    }

    @GameTest
    public static void aMineFantasyDuplicateIsStillRefused(GameTestHelper helper) {
        as(NativeRecipes.NAMESPACE, registry -> {
            RecipeId first = NativeRecipes.nativeId(registry, MADE);
            assertEquals(NativeRecipes.NAMESPACE, first.getNamespace());
            registry.add(first, new Object(), RecipeSource.NATIVE);
            try {
                NativeRecipes.nativeId(registry, MADE);
                fail("a second unnamed MineFantasy recipe was accepted");
            } catch (RecipeRegistrationException expected) {}
        });
        helper.succeed();
    }
}
