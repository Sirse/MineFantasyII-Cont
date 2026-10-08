package minefantasy.mf2.mixins;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The item a player holds in use: vanilla's getter for it is client only. */
@Mixin(EntityPlayer.class)
public interface EntityPlayerAccessor {

    @Accessor("itemInUse")
    ItemStack mf2$getItemInUse();
}
