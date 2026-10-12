package minefantasy.mf2.gametest;

import java.util.function.BooleanSupplier;

import net.minecraft.tileentity.TileEntity;

/** Runs a tile entity's update the number of ticks a test needs, in place of hand-written loops. */
public final class Ticks {

    private Ticks() {}

    /** Updates the tile the given number of ticks. */
    public static void tick(TileEntity tile, int times) {
        for (int left = times; left > 0; left--) {
            tile.updateEntity();
        }
    }

    /** Updates every tile once per tick, for the given number of ticks. */
    public static void tick(int times, TileEntity... tiles) {
        for (int left = times; left > 0; left--) {
            for (TileEntity tile : tiles) {
                tile.updateEntity();
            }
        }
    }

    /** Updates the tile until it is done or the ticks run out; false when it never got there. */
    public static boolean tickUntil(TileEntity tile, BooleanSupplier done, int maxTimes) {
        for (int left = maxTimes; left > 0 && !done.getAsBoolean(); left--) {
            tile.updateEntity();
        }
        return done.getAsBoolean();
    }

    /** Updates every tile once per tick until it is done or the ticks run out; false when it never got there. */
    public static boolean tickUntil(BooleanSupplier done, int maxTimes, TileEntity... tiles) {
        for (int left = maxTimes; left > 0 && !done.getAsBoolean(); left--) {
            for (TileEntity tile : tiles) {
                tile.updateEntity();
            }
        }
        return done.getAsBoolean();
    }
}
