package minefantasy.mf2.api.helpers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;
import net.minecraftforge.fluids.IFluidHandler;

/**
 * Fluid in what a player holds, through both of Forge's container kinds: an item that keeps its fluid itself
 * ({@link IFluidContainerItem}, such as universal cells and portable tanks, drained by the millibucket) and a
 * filled/empty pair in the {@link FluidContainerRegistry} (buckets, bottles, cells and cans, emptied whole).
 * <p>
 * As fluid mods do it: the item's own fluid is asked first, every transfer is simulated before it is made and made only
 * in full, one item of a stack is worked and the result handed back (to the inventory, or dropped when it is full),
 * nothing is used up in creative mode, and only the server changes the inventory.
 */
public final class FluidContainers {

    private FluidContainers() {}

    /** The millibuckets of the fluid in one item of the stack, 0 for another fluid or no container. */
    public static int amountIn(ItemStack stack, Fluid fluid) {
        FluidStack held = fluidIn(stack);
        return held != null && held.getFluid() == fluid ? held.amount : 0;
    }

    /** Whether the stack keeps its own fluid, drained by the millibucket, rather than being emptied whole. */
    public static boolean keepsOwn(ItemStack stack) {
        return stack != null && stack.getItem() instanceof IFluidContainerItem;
    }

    private static FluidStack fluidIn(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return null;
        }
        ItemStack one = one(stack);
        if (stack.getItem() instanceof IFluidContainerItem) {
            return ((IFluidContainerItem) stack.getItem()).getFluid(one);
        }
        return FluidContainerRegistry.getFluidForFilledItem(one);
    }

    /**
     * Takes the fluid from the held container: a registry container gives all it holds, which must be between
     * {@code min} and {@code max}; an item keeping its own gives up to {@code max}, at least {@code min}. Returns the
     * millibuckets taken, 0 when it holds too little of the fluid. The client and creative mode only learn the answer.
     */
    public static int drainHeld(EntityPlayer player, Fluid fluid, int min, int max) {
        ItemStack held = player.getHeldItem();
        if (held == null || held.getItem() == null || max < min) {
            return 0;
        }
        ItemStack one = one(held);
        if (held.getItem() instanceof IFluidContainerItem) {
            IFluidContainerItem container = (IFluidContainerItem) held.getItem();
            FluidStack inside = container.getFluid(one);
            if (inside == null || inside.getFluid() != fluid) {
                return 0;
            }
            FluidStack simulated = container.drain(one, max, false);
            int amount = simulated == null || simulated.getFluid() != fluid ? 0 : simulated.amount;
            if (amount < min) {
                return 0;
            }
            if (changes(player)) {
                container.drain(one, amount, true);
                replaceOne(player, held, one);
            }
            return amount;
        }
        FluidStack inside = FluidContainerRegistry.getFluidForFilledItem(one);
        if (inside == null || inside.getFluid() != fluid || inside.amount < min || inside.amount > max) {
            return 0;
        }
        if (changes(player)) {
            replaceOne(player, held, FluidContainerRegistry.drainFluidContainer(one));
        }
        return inside.amount;
    }

    /**
     * Fills the held container from up to {@code max} millibuckets: a registry container takes its whole capacity if
     * that fits, an item keeping its own as much as it has room for. Returns the millibuckets put in, 0 when it takes
     * none. The client and creative mode only learn the answer.
     */
    public static int fillHeld(EntityPlayer player, Fluid fluid, int max) {
        ItemStack held = player.getHeldItem();
        if (held == null || held.getItem() == null || max <= 0) {
            return 0;
        }
        ItemStack one = one(held);
        FluidStack offered = new FluidStack(fluid, max);
        if (held.getItem() instanceof IFluidContainerItem) {
            IFluidContainerItem container = (IFluidContainerItem) held.getItem();
            int amount = container.fill(one, offered, false);
            if (amount <= 0) {
                return 0;
            }
            if (changes(player)) {
                container.fill(one, new FluidStack(fluid, amount), true);
                replaceOne(player, held, one);
            }
            return amount;
        }
        ItemStack filled = FluidContainerRegistry.fillFluidContainer(offered, one);
        FluidStack inside = filled == null ? null : FluidContainerRegistry.getFluidForFilledItem(filled);
        if (inside == null || inside.amount <= 0 || inside.amount > max) {
            return 0;
        }
        if (changes(player)) {
            replaceOne(player, held, filled);
        }
        return inside.amount;
    }

    /**
     * Pours the held container into a fluid handler, or, when it holds no fluid, fills it from the handler, by the same
     * rules pipes meet: the handler is asked first by simulation, a registry container moves whole or not at all, an
     * item keeping its own fluid by the millibucket. Only the server changes the handler and the inventory; the client
     * learns whether the click was used. Returns the millibuckets moved.
     */
    public static int exchange(EntityPlayer player, IFluidHandler handler, ForgeDirection side) {
        ItemStack held = player.getHeldItem();
        FluidStack inside = fluidIn(held);
        boolean server = !player.worldObj.isRemote;
        if (inside != null && inside.getFluid() != null && inside.amount > 0) {
            Fluid fluid = inside.getFluid();
            int room = handler.fill(side, new FluidStack(fluid, inside.amount), false);
            if (room <= 0) {
                return 0;
            }
            int poured = keepsOwn(held) ? drainHeld(player, fluid, 1, room) : drainHeld(player, fluid, room, room);
            if (poured > 0 && server) {
                handler.fill(side, new FluidStack(fluid, poured), true);
            }
            return poured;
        }
        FluidStack offered = handler.drain(side, Integer.MAX_VALUE, false);
        if (offered == null || offered.getFluid() == null || offered.amount <= 0) {
            return 0;
        }
        Fluid fluid = offered.getFluid();
        int given = fillHeld(player, fluid, offered.amount);
        if (given > 0 && server) {
            handler.drain(side, new FluidStack(fluid, given), true);
        }
        return given;
    }

    /** Whether this call changes the inventory: on the server, outside creative mode. */
    private static boolean changes(EntityPlayer player) {
        return !player.worldObj.isRemote && !player.capabilities.isCreativeMode;
    }

    private static ItemStack one(ItemStack stack) {
        ItemStack one = stack.copy();
        one.stackSize = 1;
        return one;
    }

    /** Swaps one held item for the result: in the hand when it was the last, else into the inventory or dropped. */
    private static void replaceOne(EntityPlayer player, ItemStack held, ItemStack result) {
        int slot = player.inventory.currentItem;
        if (held.stackSize <= 1) {
            player.inventory.setInventorySlotContents(slot, result);
        } else {
            held.stackSize--;
            if (result != null && !player.inventory.addItemStackToInventory(result)) {
                player.dropPlayerItemWithRandomChoice(result, false);
            }
        }
        if (player instanceof EntityPlayerMP) {
            player.inventoryContainer.detectAndSendChanges();
        }
    }
}
