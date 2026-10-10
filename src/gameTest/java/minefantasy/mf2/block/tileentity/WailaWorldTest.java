package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.gametest.Assert.*;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.knowledge.InformationBase;
import minefantasy.mf2.api.knowledge.InformationList;
import minefantasy.mf2.api.knowledge.ResearchArtefacts;
import minefantasy.mf2.api.knowledge.ResearchLogic;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.tileentity.decor.TileEntityAmmoBox;
import minefantasy.mf2.block.tileentity.decor.TileEntityRack;
import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.integration.waila.WailaData;
import minefantasy.mf2.integration.waila.WailaProvider;

@GameTestHolder("minefantasy2")
public class WailaWorldTest {

    private static NBTTagCompound inspect(GameTestHelper helper, TileEntity tile, FakePlayer player) {
        player.setPosition(tile.xCoord + .5, tile.yCoord + 1, tile.zCoord + .5);
        NBTTagCompound before = new NBTTagCompound();
        tile.writeToNBT(before);
        tile.getWorldObj().getChunkFromBlockCoords(tile.xCoord, tile.zCoord).isModified = false;
        NBTTagCompound result = null;
        for (int i = 0; i < 3; i++) result = new WailaProvider().getNBTData(
                player,
                tile,
                new NBTTagCompound(),
                tile.getWorldObj(),
                tile.xCoord,
                tile.yCoord,
                tile.zCoord).getCompoundTag(WailaData.KEY);
        NBTTagCompound after = new NBTTagCompound();
        tile.writeToNBT(after);
        assertEquals("view changed state", before, after);
        assertFalse(
                "view dirtied chunk",
                tile.getWorldObj().getChunkFromBlockCoords(tile.xCoord, tile.zCoord).isModified);
        return result;
    }

    @GameTest
    public static void storageUsesActualCountAndRackSlots(GameTestHelper helper) {
        FakePlayer player = Modders.fresh(helper, Modders.NOVICE);
        helper.setBlock(2, 2, 2, BlockListMF.components);
        TileEntityComponent pile = helper.assertTileEntityPresent(TileEntityComponent.class, 2, 2, 2);
        pile.setItem(new ItemStack(Items.iron_ingot), "bar", "bar", 64, 40);
        NBTTagCompound data = inspect(helper, pile, player);
        assertEquals(40, data.getInteger("Count"));
        assertEquals(64, data.getInteger("Limit"));
        assertEquals(1, pile.item.stackSize);
        helper.setBlock(3, 2, 2, BlockListMF.ammo_box_basic);
        TileEntityAmmoBox box = helper.assertTileEntityPresent(TileEntityAmmoBox.class, 3, 2, 2);
        box.ammo = new ItemStack(Items.arrow);
        box.stock = 100;
        data = inspect(helper, box, player);
        assertEquals(100, data.getInteger("Count"));
        assertEquals(box.getMaxAmmo(box.ammo), data.getInteger("Limit"));
        helper.setBlock(4, 2, 2, BlockListMF.rack_wood);
        TileEntityRack rack = helper.assertTileEntityPresent(TileEntityRack.class, 4, 2, 2);
        rack.setInventorySlotContents(0, new ItemStack(Items.iron_sword));
        data = inspect(helper, rack, player);
        assertEquals(1, data.getInteger("Count"));
        assertEquals(4, data.getInteger("Limit"));
        assertEquals(1, data.getTagList("Results", 10).tagCount());
        helper.succeed();
    }

    @GameTest
    public static void berriesAndRoadShowCurrentStateWithoutHarvesting(GameTestHelper helper) {
        FakePlayer player = Modders.fresh(helper, Modders.NOVICE);
        helper.setBlock(2, 2, 2, BlockListMF.berryBush);
        assertEquals(1, berryTip(0).size());
        assertEquals(1, berryTip(1).size());
        assertFalse(berryTip(0).equals(berryTip(1)));
        helper.setBlock(3, 2, 2, BlockListMF.road);
        TileEntityRoad road = helper.assertTileEntityPresent(TileEntityRoad.class, 3, 2, 2);
        road.setSurface(Blocks.cobblestone, 0);
        road.isLocked = true;
        NBTTagCompound data = inspect(helper, road, player);
        assertTrue(data.getBoolean("Locked"));
        assertEquals(
                new ItemStack(Blocks.cobblestone).getItem(),
                ItemStack.loadItemStackFromNBT(data.getCompoundTag("Surface")).getItem());
        helper.succeed();
    }

    @GameTest
    public static void researchProgressBelongsToViewerAndCurrentItem(GameTestHelper helper) {
        FakePlayer player = Modders.fresh(helper, Modders.SCHOLAR);
        helper.setBlock(2, 2, 2, BlockListMF.research);
        TileEntityResearch table = helper.assertTileEntityPresent(TileEntityResearch.class, 2, 2, 2);
        ItemStack artifact = new ItemStack(Items.book);
        table.setInventorySlotContents(0, artifact);
        table.study.startOverIfChanged(artifact, player.getUniqueID().toString());
        table.study.progress = 4;
        table.maxProgress = 10;
        assertTrue(inspect(helper, table, player).getBoolean("ShowProgress"));
        FakePlayer other = Modders.fresh(helper, Modders.NOVICE);
        assertFalse(inspect(helper, table, other).hasKey("Progress"));
        table.setInventorySlotContents(0, new ItemStack(Items.paper));
        assertFalse(inspect(helper, table, player).hasKey("Progress"));
        helper.succeed();
    }

    @GameTest
    public static void researchNamesRespectViewerKnowledge(GameTestHelper helper) {
        FakePlayer player = Modders.fresh(helper, Modders.NOVICE);
        InformationBase hidden = null;
        for (InformationBase base : InformationList.nameMap.values()) {
            if (base.parentInfo != null && !ResearchLogic.hasInfoUnlocked(player, base)
                    && !ResearchLogic.canPurchase(player, base)) {
                hidden = base;
                break;
            }
        }
        assertTrue("no locked research", hidden != null);
        ItemStack artifact = new ItemStack(Items.book, 1, 32760);
        ResearchArtefacts.addArtefact(artifact, hidden);
        helper.setBlock(2, 2, 2, BlockListMF.research);
        TileEntityResearch table = helper.assertTileEntityPresent(TileEntityResearch.class, 2, 2, 2);
        table.setInventorySlotContents(0, artifact);
        NBTTagCompound data = inspect(helper, table, player);
        assertTrue(data.getBoolean("Unknown"));
        assertEquals(0, data.getTagList("Research", 8).tagCount());
        for (InformationBase parent = hidden.parentInfo; parent != null; parent = parent.parentInfo) {
            ResearchLogic.forceUnlock(player, parent);
        }
        data = inspect(helper, table, player);
        assertFalse(data.getBoolean("Unknown"));
        assertEquals(1, data.getTagList("Research", 8).tagCount());
        helper.succeed();
    }

    private static java.util.List<String> berryTip(int metadata) {
        mcp.mobius.waila.api.IWailaDataAccessor accessor = (mcp.mobius.waila.api.IWailaDataAccessor) java.lang.reflect.Proxy
                .newProxyInstance(
                        WailaWorldTest.class.getClassLoader(),
                        new Class<?>[] { mcp.mobius.waila.api.IWailaDataAccessor.class },
                        (proxy, method, args) -> method.getName().equals("getBlock") ? BlockListMF.berryBush
                                : method.getName().equals("getMetadata") ? metadata : new NBTTagCompound());
        mcp.mobius.waila.api.IWailaConfigHandler config = (mcp.mobius.waila.api.IWailaConfigHandler) java.lang.reflect.Proxy
                .newProxyInstance(
                        WailaWorldTest.class.getClassLoader(),
                        new Class<?>[] { mcp.mobius.waila.api.IWailaConfigHandler.class },
                        (proxy, method, args) -> true);
        return new WailaProvider().getWailaBody(null, new java.util.ArrayList<>(), accessor, config);
    }
}
