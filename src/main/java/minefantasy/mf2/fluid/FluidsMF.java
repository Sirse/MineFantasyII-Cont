package minefantasy.mf2.fluid;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.item.food.FoodListMF;
import minefantasy.mf2.item.list.ComponentListMF;

/**
 * MineFantasy's fluids, by the names other mods give them, so they are shared: seed oil as Forestry and GregTech call
 * it, salt water as TerraFirmaCraft and GregTech do. A fluid another mod has registered by the end of loading is taken
 * as it is; only a missing one is registered here.
 */
public final class FluidsMF {

    /** The millibuckets of a jug, as four are filled from one bucket. */
    public static final int JUG = 250;

    public static Fluid seedOil;
    public static Fluid saltWater;
    /** The fluids registered here, not by another mod: they take water's icons. */
    private static final List<Fluid> OWN = new ArrayList<Fluid>();

    private FluidsMF() {}

    /** Whether this mod supplies the fluid's appearance rather than another mod. */
    public static boolean isOwn(Fluid fluid) {
        return OWN.contains(fluid);
    }

    /** Takes or registers the fluids and their containers: after every mod has registered its own. */
    public static void load() {
        seedOil = shared("seedoil", 0x59431F, 885, 5000);
        saltWater = shared("saltwater", 0xD8ECF0, 1025, 1000);
        FluidContainerRegistry.registerFluidContainer(
                new FluidStack(FluidRegistry.WATER, JUG),
                new ItemStack(FoodListMF.jug_water),
                new ItemStack(FoodListMF.jug_empty));
        FluidContainerRegistry.registerFluidContainer(
                new FluidStack(seedOil, JUG),
                new ItemStack(ComponentListMF.plant_oil),
                new ItemStack(FoodListMF.jug_empty));
    }

    private static Fluid shared(String name, int colour, int density, int viscosity) {
        Fluid existing = FluidRegistry.getFluid(name);
        if (existing != null) {
            return existing;
        }
        Fluid fluid = new Tinted(name, colour).setDensity(density).setViscosity(viscosity);
        FluidRegistry.registerFluid(fluid);
        OWN.add(fluid);
        return fluid;
    }

    /** A fluid drawn with water's icons in its own colour. */
    private static final class Tinted extends Fluid {

        private final int colour;

        Tinted(String name, int colour) {
            super(name);
            this.colour = colour;
        }

        @Override
        public int getColor() {
            return colour;
        }
    }

    /** Gives the fluids registered here water's icons, once the block textures are stitched. */
    public static final class Icons {

        @SubscribeEvent
        @SideOnly(Side.CLIENT)
        public void stitched(TextureStitchEvent.Post event) {
            if (event.map.getTextureType() != 0) {
                return;
            }
            for (Fluid fluid : OWN) {
                fluid.setIcons(Blocks.water.getIcon(0, 0), Blocks.flowing_water.getIcon(2, 0));
            }
        }
    }
}
