package minefantasy.mf2.item;

import static minefantasy.mf2.gametest.Assert.*;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.item.food.FoodListMF;
import minefantasy.mf2.item.food.ItemFoodMF;

/**
 * Eating the mod's food: a short pause after each meal, and the bowl coming back. (Juicy berries curing poison is left
 * out: a fake player cannot take a potion effect, having no connection to send it through.)
 */
@GameTestHolder("minefantasy2")
public class FoodTest {

    private FoodTest() {}

    private static FakePlayer hungry(GameTestHelper helper) {
        FakePlayer player = Modders.fresh(helper, Modders.SMITH);
        player.getFoodStats().addStats(-20, 0F);
        player.inventory.currentItem = 0;
        return player;
    }

    private static boolean startsEating(FakePlayer player, ItemStack food) {
        player.stopUsingItem();
        player.inventory.setInventorySlotContents(0, food);
        food.getItem().onItemRightClick(food, player.worldObj, player);
        return player.isUsingItem();
    }

    /** After a meal the next one waits half a second, then may start. */
    @GameTest
    public static void thePauseAfterAMealRunsOut(GameTestHelper helper) {
        FakePlayer player = hungry(helper);
        ItemStack stew = new ItemStack(FoodListMF.stew);
        stew.getItem().onEaten(stew, helper.getWorld(), player);
        assertFalse("a meal started right after another", startsEating(player, new ItemStack(FoodListMF.stew)));
        // The pause is ten ticks; counted any other way it would grow or wrap round instead
        for (int tick = 0; tick < 9; tick++) {
            ItemFoodMF.onTick(player);
        }
        assertFalse("the pause ended early", startsEating(player, new ItemStack(FoodListMF.stew)));
        ItemFoodMF.onTick(player);
        assertTrue("the pause after a meal never ran out", startsEating(player, new ItemStack(FoodListMF.stew)));
        helper.succeed();
    }

    @GameTest
    public static void aBowlComesBackFromStew(GameTestHelper helper) {
        FakePlayer player = hungry(helper);
        ItemStack left = FoodListMF.stew.onEaten(new ItemStack(FoodListMF.stew), helper.getWorld(), player);
        assertNotNull("nothing came back", left);
        assertEquals(Items.bowl, left.getItem());
        assertEquals(1, left.stackSize);
        helper.succeed();
    }
}
