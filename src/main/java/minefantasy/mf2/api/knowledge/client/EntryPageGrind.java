package minefantasy.mf2.api.knowledge.client;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class EntryPageGrind extends EntryPageConversion {

    public EntryPageGrind(ItemStack in, ItemStack out) {
        super("grindGrid", "quern", in, out);
    }

    public EntryPageGrind(Item in, Item out) {
        this(new ItemStack(in), new ItemStack(out));
    }
}
