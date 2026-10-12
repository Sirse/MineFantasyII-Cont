package minefantasy.mf2.gametest;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Denies every block interaction, as a region protection mod would inside its region. */
public final class DenyInteract {

    @SubscribeEvent
    public void deny(PlayerInteractEvent event) {
        event.setCanceled(true);
    }
}
