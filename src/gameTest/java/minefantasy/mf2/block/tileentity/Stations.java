package minefantasy.mf2.block.tileentity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.WorldServer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;

import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.heating.Heatable;
import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.material.CustomMaterial;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.api.recipe.RecipeRegistries;
import minefantasy.mf2.api.recipe.RecipeSource;
import minefantasy.mf2.api.recipe.RecipeTransaction;
import minefantasy.mf2.gametest.TestItems;

/**
 * Shared fixture for tests that drive real stations. A station stands in the test's cell of the server world, so what
 * it drops lands there; recipes change the way a script reload changes them.
 * <p>
 * Every test body runs between {@link #begin} and {@link #end}, or {@link #keepUntilFinished} for a test that goes on
 * over ticks. Each test's script recipes are its own layer: a reload publishes the layers of every running test, so a
 * test that lasts several ticks keeps its recipes while others reload theirs, and finishing drops only its own.
 */
public final class Stations {

    private static GameTestHelper helper;
    /** The script recipes of every running test. */
    private static final Map<GameTestHelper, Consumer<RecipeTransaction>> LAYERS = new LinkedHashMap<>();
    /** The test whose layer is being applied: its recipe ids carry its mark, so layers never share an id. */
    private static GameTestHelper layerOwner;

    private Stations() {}

    public static void begin(GameTestHelper test) {
        helper = test;
    }

    /** The body is done: its recipes and drops go. */
    public static void end() {
        GameTestHelper test = helper;
        helper = null;
        release(test);
    }

    /** The body is done but the test goes on over ticks: its recipes stay until it finishes. */
    public static void keepUntilFinished() {
        GameTestHelper test = helper;
        helper = null;
        test.afterTest(() -> release(test));
    }

    private static void release(GameTestHelper test) {
        for (EntityItem item : drops(test)) {
            item.setDead();
        }
        if (LAYERS.remove(test) != null) {
            publish();
        }
    }

    static WorldServer world() {
        return helper.getWorld();
    }

    /** Stands the station in the cell: it acts on the server world but is not placed, so it never ticks by itself. */
    static <T extends TileEntity> T place(T station) {
        TestPos at = helper.absolute(1, 1, 1);
        station.setWorldObj(world());
        station.xCoord = at.x();
        station.yCoord = at.y();
        station.zCoord = at.z();
        return station;
    }

    /** Replaces this test's script recipes, as a reload does. */
    public static void reload(Consumer<RecipeTransaction> changes) {
        reload(helper, changes);
    }

    /** Replaces the given test's script recipes: for a test over ticks, reloading after its body. */
    public static void reload(GameTestHelper test, Consumer<RecipeTransaction> changes) {
        LAYERS.put(test, changes);
        try {
            publish();
        } catch (RuntimeException e) {
            LAYERS.remove(test);
            publish();
            throw e;
        }
    }

    /** Publishes the native recipes plus the layers of the running tests. */
    public static void publish() {
        RecipeRegistries registries = MFRecipes.REGISTRIES;
        registries.beginReload();
        try (RecipeTransaction tx = registries.begin(RecipeSource.script("test"))) {
            for (Map.Entry<GameTestHelper, Consumer<RecipeTransaction>> layer : LAYERS.entrySet()) {
                layerOwner = layer.getKey();
                layer.getValue().accept(tx);
            }
            tx.commit();
        } catch (RuntimeException e) {
            registries.abortReload();
            throw e;
        } finally {
            layerOwner = null;
        }
        registries.publish();
    }

    /** A script recipe id of the test whose layer is applied, so tests running together never collide. */
    static RecipeId id(String station, String path) {
        GameTestHelper owner = layerOwner != null ? layerOwner : helper;
        String mark = owner == null ? "" : "." + Integer.toHexString(System.identityHashCode(owner));
        return RecipeId.of("crafttweaker", station + "/" + path + mark);
    }

    @SuppressWarnings("unchecked")
    private static List<EntityItem> drops(GameTestHelper test) {
        TestPos low = test.absolute(-1, 0, -1);
        TestPos high = test.absolute(4, 4, 4);
        return test.getWorld().getEntitiesWithinAABB(
                EntityItem.class,
                AxisAlignedBB.getBoundingBox(low.x(), low.y(), low.z(), high.x(), high.y(), high.z()));
    }

    /** Items of the given kind the stations of this test dropped. */
    static int dropped(Item item) {
        int count = 0;
        for (EntityItem entity : drops(helper)) {
            if (!entity.isDead && entity.getEntityItem().getItem() == item) {
                count += entity.getEntityItem().stackSize;
            }
        }
        return count;
    }

    /** A hot piece carrying the given stack, as the forge makes it: workable from 100, unstable above 500. */
    static ItemStack heated(ItemStack cold, int temperature) {
        ItemStack piece = new ItemStack(TestItems.hot, cold.stackSize);
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setTag(Heatable.NBT_Item, cold.writeToNBT(new NBTTagCompound()));
        nbt.setInteger(Heatable.NBT_CurrentTemp, temperature);
        nbt.setInteger(Heatable.NBT_WorkableTemp, 100);
        nbt.setInteger(Heatable.NBT_UnstableTemp, 500);
        nbt.setInteger(Heatable.NBT_MaxTemp, 900);
        piece.setTagCompound(nbt);
        return piece;
    }

    /** A stack of the given material. */
    static ItemStack of(Item item, CustomMaterial material) {
        ItemStack stack = new ItemStack(item);
        CustomMaterial.addMaterial(stack, CustomToolHelper.slot_main, material.getName());
        return stack;
    }

    static Object get(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    static Object call(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(target, args);
    }
}
