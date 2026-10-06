package minefantasy.mf2.mixins;

import java.util.ArrayDeque;
import java.util.Deque;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.server.management.ItemInWorldManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import minefantasy.mf2.mechanics.BlockEvents;

/**
 * Settles what mining gives once vanilla's harvest really removed the block: Forge 1.7.10 has no event after a break,
 * and a break that drops nothing (stone by hand) raises none at all.
 */
@Mixin(ItemInWorldManager.class)
public abstract class ItemInWorldManagerMixin {

    /** What each harvest in progress takes, or null until it gets that far; nested harvests stack. */
    @Unique
    private final Deque<Object[]> mf2$harvests = new ArrayDeque<Object[]>();

    @Inject(method = "tryHarvestBlock", at = @At("HEAD"))
    private void mf2$beginHarvest(int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        mf2$harvests.push(new Object[0]);
    }

    /**
     * Notes what is harvested where vanilla reads it itself: after the break event and the tool's own say, which may
     * have changed the block or the tool, and before the tool wears or breaks.
     */
    @Inject(
            method = "tryHarvestBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;playAuxSFXAtEntity(Lnet/minecraft/entity/player/EntityPlayer;IIIII)V"))
    private void mf2$noteHarvest(int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        ItemInWorldManager self = (ItemInWorldManager) (Object) this;
        ItemStack held = self.thisPlayerMP.getHeldItem();
        mf2$harvests.pop();
        mf2$harvests.push(
                new Object[] { self.theWorld.getBlock(x, y, z), self.theWorld.getBlockMetadata(x, y, z),
                        held == null ? null : held.copy() });
    }

    @Inject(method = "tryHarvestBlock", at = @At("RETURN"))
    private void mf2$settleHarvest(int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        Object[] noted = mf2$harvests.pop();
        if (cir.getReturnValue() && noted.length == 3) {
            ItemInWorldManager self = (ItemInWorldManager) (Object) this;
            BlockEvents.successfulPlayerBreak(
                    self.theWorld,
                    x,
                    y,
                    z,
                    (Block) noted[0],
                    (Integer) noted[1],
                    self.thisPlayerMP,
                    (ItemStack) noted[2]);
        }
    }
}
