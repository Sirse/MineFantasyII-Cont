package minefantasy.mf2.api.knowledge.client;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class EntryPageBlastFurnace extends EntryPageConversion {

    public EntryPageBlastFurnace(ItemStack in, ItemStack out) {
        super("blastfurnaceGrid", "blastfurnace", in, out);
    }

    public EntryPageBlastFurnace(Item in, ItemStack out) {
        this(new ItemStack(in), out);
    }

    public EntryPageBlastFurnace(Item in, Item out) {
        this(new ItemStack(in), new ItemStack(out));
    }
}
