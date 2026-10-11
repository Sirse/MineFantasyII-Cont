package minefantasy.mf2.mixins;

import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.world.WorldSettings;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The gametype the client plays in: vanilla 1.7.10 keeps the field private, and only the client has a controller. */
@Mixin(PlayerControllerMP.class)
public interface PlayerControllerAccessor {

    @Accessor("currentGameType")
    WorldSettings.GameType mf2$getCurrentGameType();
}
