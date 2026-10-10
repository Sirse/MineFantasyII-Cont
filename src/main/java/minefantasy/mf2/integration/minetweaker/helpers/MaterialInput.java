package minefantasy.mf2.integration.minetweaker.helpers;

import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.recipe.Input;
import minetweaker.api.item.IItemCondition;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;

/**
 * {@code mods.minefantasy.MF.input}: an item in a main material, with a haft rule. A haft left unnamed is any haft,
 * none included; {@code MF.inputNoHaft} asks for none. Stations check the materials as native rules, and NEI shows the
 * item made of them.
 */
public final class MaterialInput extends ExplicitIngredient {

    private final ItemStack example;
    private final String main;
    private final Input.MaterialRule haft;
    private final String written;

    public MaterialInput(ItemStack example, String main, Input.MaterialRule haft, String written) {
        this(example, main, haft, written, 1, null, Collections.<IItemCondition>emptyList());
    }

    private MaterialInput(ItemStack example, String main, Input.MaterialRule haft, String written, int amount,
            String mark, List<IItemCondition> conditions) {
        super(amount, mark, conditions);
        this.example = example.copy();
        this.example.stackSize = 1;
        this.main = main;
        this.haft = haft;
        this.written = written;
    }

    @Override
    protected Input rule() {
        return Input.of(example.getItem(), example.getItemDamage()).material(main).haft(haft);
    }

    /** Whether an item-and-material store keeps the rule: one asking for no haft cannot be told from any haft. */
    public boolean keptByItem() {
        return haft != Input.MaterialRule.NONE;
    }

    @Override
    public List<IItemStack> getItems() {
        return Collections.singletonList(MineTweakerMC.getIItemStack(example));
    }

    @Override
    protected String describe() {
        return written;
    }

    @Override
    protected ExplicitIngredient with(int amount, String mark, List<IItemCondition> conditions) {
        return new MaterialInput(example, main, haft, written, amount, mark, conditions);
    }
}
