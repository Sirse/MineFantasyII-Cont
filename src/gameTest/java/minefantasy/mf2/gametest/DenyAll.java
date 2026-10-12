package minefantasy.mf2.gametest;

import net.minecraftforge.event.world.BlockEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Denies every break, as a region protection mod would inside its region. */
public final class DenyAll {

    @SubscribeEvent
    public void deny(BlockEvent.BreakEvent event) {
        event.setCanceled(true);
    }
}
