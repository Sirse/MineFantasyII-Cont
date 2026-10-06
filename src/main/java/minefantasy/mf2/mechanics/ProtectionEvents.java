package minefantasy.mf2.mechanics;

import java.util.Iterator;

import net.minecraft.world.ChunkPosition;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.event.world.WorldEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import minefantasy.mf2.entity.EntityFireBlast;

/** Forge event handlers that keep MineFantasy's world changes within protection. */
public final class ProtectionEvents {

    /**
     * A fire blast's explosion is set off by the blast, not by whoever is behind it, so protection cannot see the
     * player or creature it acts for. Only for those explosions are the blocks the shooter may not change taken out of
     * what it breaks and, in vanilla's explosion, sets alight. Any other explosion is left to its own events.
     */
    @SubscribeEvent
    public void filterFireBlast(ExplosionEvent.Detonate event) {
        if (event.world.isRemote || !(event.explosion.exploder instanceof EntityFireBlast)) return;
        EntityFireBlast blast = (EntityFireBlast) event.explosion.exploder;
        Iterator<ChunkPosition> iterator = event.getAffectedBlocks().iterator();
        while (iterator.hasNext()) {
            ChunkPosition position = iterator.next();
            int x = position.chunkPosX;
            int y = position.chunkPosY;
            int z = position.chunkPosZ;
            if (!event.world.blockExists(x, y, z) || !blast.mayChange(x, y, z)) iterator.remove();
        }
    }

    /** Drops the owners' stand-ins of a world as it unloads. */
    @SubscribeEvent
    public void worldUnloaded(WorldEvent.Unload event) {
        if (event.world instanceof WorldServer) ActionOwner.unload((WorldServer) event.world);
    }
}
