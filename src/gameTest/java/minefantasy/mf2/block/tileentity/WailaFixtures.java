package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.gametest.Assert.*;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;

import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.integration.waila.WailaData;
import minefantasy.mf2.integration.waila.WailaProvider;

/** Drives the server's Waila callback as a looking player would; looking must never change or dirty anything. */
final class WailaFixtures {

    private WailaFixtures() {}

    static FakePlayer near(GameTestHelper helper, int x, int y, int z) {
        FakePlayer player = Modders.fresh(helper, Modders.SMITH);
        player.setPosition(x + 0.5D, y + 1D, z + 0.5D);
        return player;
    }

    static FakePlayer near(GameTestHelper helper, TileEntity tile) {
        return near(helper, tile.xCoord, tile.yCoord, tile.zCoord);
    }

    static NBTTagCompound saved(TileEntity tile) {
        NBTTagCompound tag = new NBTTagCompound();
        tile.writeToNBT(tag);
        return tag;
    }

    /** What the tooltip gets, asked three times; fails when asking changed saved state or dirtied a clean chunk. */
    static NBTTagCompound inspect(TileEntity tile, FakePlayer player) {
        NBTTagCompound before = saved(tile);
        tile.getWorldObj().getChunkFromBlockCoords(tile.xCoord, tile.zCoord).isModified = false;
        NBTTagCompound data = null;
        for (int repeat = 0; repeat < 3; repeat++) {
            data = new WailaProvider().getNBTData(
                    player,
                    tile,
                    new NBTTagCompound(),
                    tile.getWorldObj(),
                    tile.xCoord,
                    tile.yCoord,
                    tile.zCoord).getCompoundTag(WailaData.KEY);
        }
        assertEquals("tooltip changed saved state", before, saved(tile));
        assertFalse(
                "tooltip dirtied a clean chunk",
                tile.getWorldObj().getChunkFromBlockCoords(tile.xCoord, tile.zCoord).isModified);
        return data;
    }

    /** {@link #inspect} by a player standing on the block. */
    static NBTTagCompound inspectNear(TileEntity tile, FakePlayer player) {
        player.setPosition(tile.xCoord + .5, tile.yCoord + 1, tile.zCoord + .5);
        return inspect(tile, player);
    }
}
