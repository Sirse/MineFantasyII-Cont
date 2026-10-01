package minefantasy.mf2.api.knowledge.client;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class EntryPageRecipeBloom extends EntryPageConversion {

    public EntryPageRecipeBloom(ItemStack in, ItemStack out) {
        super("bloomeryGrid", "bloomery", in, out);
    }

    public EntryPageRecipeBloom(Item in, Item out) {
        this(new ItemStack(in), new ItemStack(out));
    }
}
