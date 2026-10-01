package minefantasy.mf2.api.knowledge.client;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class EntryPageSmelting extends EntryPageConversion {

    public EntryPageSmelting(ItemStack in, ItemStack out) {
        super("furnaceGrid", "furnace", in, out);
    }

    public EntryPageSmelting(Item in, Item out) {
        this(new ItemStack(in), new ItemStack(out));
    }
}
