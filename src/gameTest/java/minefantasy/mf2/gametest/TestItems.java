package minefantasy.mf2.gametest;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.registry.GameRegistry;
import minefantasy.mf2.api.crafting.MineFantasyFuels;
import minefantasy.mf2.api.heating.IHotItem;
import minefantasy.mf2.api.material.CustomMaterial;

/** The items and materials the game tests use, registered by {@link GameTestMod}. */
public final class TestItems {

    public static Item ore, bar, metaBar, seed, flour, pot, junk, carbon;
    /** A tool that breaks after ten uses. */
    public static Item hammer;
    /** A single water container that leaves a bucket, which stacks to 16. */
    public static Item water, bucket;
    /** A stackable container that leaves an empty jar. */
    public static Item jar, emptyJar;
    /** A worn tool with 100 uses, for repairs. */
    public static Item blade;
    /** A hot piece: its NBT carries the item inside and its temperatures, as the forge writes them. */
    public static Item hot;
    /** Made carbon by a script only: the ore dictionary cannot take it back, so nothing else uses it. */
    public static Item fuel;

    /** A metal whose craft time modifier is 3 and whose tiers are 3; a second metal; a wood. */
    public static CustomMaterial steel, bronze, oak;

    private TestItems() {}

    static void register() {
        ore = item("ore", new Item());
        bar = item("bar", new Item());
        metaBar = item("meta_bar", new Item().setHasSubtypes(true));
        seed = item("seed", new Item());
        flour = item("flour", new Item());
        pot = item("pot", new Item());
        junk = item("junk", new Item());
        carbon = item("carbon", new Item());
        hammer = item("hammer", new Item().setMaxDamage(10).setMaxStackSize(1));
        bucket = item("bucket", new Item().setMaxStackSize(16));
        water = item("water", new Item().setContainerItem(bucket).setMaxStackSize(1));
        emptyJar = item("empty_jar", new Item());
        jar = item("jar", new Item().setContainerItem(emptyJar).setMaxStackSize(16));
        blade = item("blade", new Item().setMaxDamage(100).setMaxStackSize(1));
        hot = item("hot", new HotItem());
        fuel = item("fuel", new Item());

        // Carbon burns for four items
        MineFantasyFuels.addCarbon(carbon, 4);

        steel = new CustomMaterial("teststeel", "metal", 3, 1F, 1F, 1F, 1F, 0.5F, 1F).setCrafterTiers(3).register();
        bronze = new CustomMaterial("testbronze", "metal", 2, 1F, 1F, 1F, 1F, 0.5F, 1F).setCrafterTiers(2).register();
        oak = new CustomMaterial("testoak", "wood", 1, 1F, 1F, 1F, 1F, 0.5F, 1F).setCrafterTiers(1).register();
    }

    private static Item item(String name, Item item) {
        item.setUnlocalizedName(GameTestMod.MODID + "." + name);
        GameRegistry.registerItem(item, name);
        return item;
    }

    private static final class HotItem extends Item implements IHotItem {

        @Override
        public boolean isHot(ItemStack item) {
            return true;
        }

        @Override
        public boolean isCoolable(ItemStack item) {
            return true;
        }
    }
}
