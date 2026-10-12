package minefantasy.mf2.gametest;

import net.minecraft.block.Block;
import net.minecraftforge.event.world.BlockEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Denies placing but not breaking, as some protection mods do. */
public final class DenyPlacing {

    public int refused;
    public Block placed;
    public Block worldBlockDuringEvent;

    @SubscribeEvent
    public void deny(BlockEvent.PlaceEvent event) {
        refused++;
        placed = event.block;
        worldBlockDuringEvent = event.world.getBlock(event.x, event.y, event.z);
        event.setCanceled(true);
    }
}
