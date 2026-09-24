package minefantasy.mf2.api.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

/**
 * Access to ore dictionary names, read at lookup time so late registrations count. Tests swap it via
 * {@link #set(OreNames)} to avoid loading Forge's dictionary.
 */
public interface OreNames {

    /** Names the stack is registered under. */
    List<String> namesOf(ItemStack stack);

    /** Stacks registered under the name, as copies. */
    List<ItemStack> stacksOf(String name);

    static OreNames get() {
        return Holder.current;
    }

    static void set(OreNames names) {
        Holder.current = names == null ? new Forge() : names;
    }

    final class Holder {

        private static volatile OreNames current = new Forge();

        private Holder() {}
    }

    final class Forge implements OreNames {

        @Override
        public List<String> namesOf(ItemStack stack) {
            if (stack == null || stack.getItem() == null) {
                return Collections.emptyList();
            }
            int[] ids = OreDictionary.getOreIDs(stack);
            List<String> names = new ArrayList<>(ids.length);
            for (int id : ids) {
                names.add(OreDictionary.getOreName(id));
            }
            return names;
        }

        @Override
        public List<ItemStack> stacksOf(String name) {
            List<ItemStack> copies = new ArrayList<>();
            for (ItemStack stack : OreDictionary.getOres(name)) {
                copies.add(stack.copy());
            }
            return copies;
        }
    }
}
