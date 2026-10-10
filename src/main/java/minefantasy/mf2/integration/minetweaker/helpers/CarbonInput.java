package minefantasy.mf2.integration.minetweaker.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.crafting.MineFantasyFuels;
import minefantasy.mf2.api.recipe.Input;
import minetweaker.api.item.IItemCondition;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;

/**
 * {@code mods.minefantasy.MF.carbon()}: any carbon item, as the bloomery and blast furnace burn. Carbon is
 * interchangeable only where a script asks for it this way; a named item asks for that item.
 */
public final class CarbonInput extends ExplicitIngredient {

    public CarbonInput() {
        this(1, null, Collections.<IItemCondition>emptyList());
    }

    private CarbonInput(int amount, String mark, List<IItemCondition> conditions) {
        super(amount, mark, conditions);
    }

    @Override
    protected Input rule() {
        return Input.carbon();
    }

    /** The carbon items known now; carbon a script adds later still matches. */
    @Override
    public List<IItemStack> getItems() {
        List<IItemStack> items = new ArrayList<IItemStack>();
        for (ItemStack item : MineFantasyFuels.carbonItems()) {
            items.add(MineTweakerMC.getIItemStack(item));
        }
        return items;
    }

    @Override
    protected String describe() {
        return "mods.minefantasy.MF.carbon()";
    }

    @Override
    protected ExplicitIngredient with(int amount, String mark, List<IItemCondition> conditions) {
        return new CarbonInput(amount, mark, conditions);
    }
}
