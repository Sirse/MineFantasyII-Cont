package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.gametest.Assert.*;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.crafting.exotic.SpecialForging;
import minefantasy.mf2.api.knowledge.ResearchLogic;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.item.list.CustomToolListMF;
import minefantasy.mf2.knowledge.KnowledgeListMF;

/**
 * Dragonforging, as the book tells it: a smith who has learned it, forging on an anvil near a forge that holds a dragon
 * heart, makes the dragonforged version of a weapon; the craft takes that heart.
 */
@GameTestHolder("minefantasy2")
public class DragonforgeTest {

    private DragonforgeTest() {}

    private static FakePlayer smith(GameTestHelper helper, boolean learned) {
        FakePlayer smith = Modders.fresh(helper, Modders.SCHOLAR);
        if (learned) {
            ResearchLogic.forceUnlock(smith, KnowledgeListMF.smeltDragonforge);
        }
        return smith;
    }

    private static TileEntityForge forge(GameTestHelper helper, int x, int y, int z, float heart) {
        helper.setBlock(x, y, z, BlockListMF.forge);
        TileEntityForge forge = helper.assertTileEntityPresent(TileEntityForge.class, x, y, z);
        forge.dragonHeartPower = heart;
        return forge;
    }

    @GameTest
    public static void theWeaponsHaveDragonforgedVersions(GameTestHelper helper) {
        for (Item base : new Item[] { CustomToolListMF.standard_dagger, CustomToolListMF.standard_sword,
                CustomToolListMF.standard_mace, CustomToolListMF.standard_waraxe, CustomToolListMF.standard_spear,
                CustomToolListMF.standard_katana }) {
            Item dragon = SpecialForging.getDragonCraft(new ItemStack(base));
            assertNotNull(base.getUnlocalizedName() + " has no dragonforged version", dragon);
            assertNotSame(base, dragon);
        }
        helper.succeed();
    }

    @GameTest
    public static void aHeartIsFoundOnlyNearAndOnlyByOneWhoLearnedIt(GameTestHelper helper) {
        helper.setBlock(5, 1, 5, BlockListMF.anvilStone);
        TileEntityAnvilMF anvil = helper.assertTileEntityPresent(TileEntityAnvilMF.class, 5, 1, 5);

        TileEntityForge cold = forge(helper, 7, 1, 5, 0F);
        assertNull("a forge without a heart counted", anvil.heartedForge(smith(helper, true)));

        TileEntityForge far = forge(helper, 10, 1, 5, 1F);
        assertNull("a heart five blocks away counted", anvil.heartedForge(smith(helper, true)));

        cold.dragonHeartPower = 1F;
        assertNull("a smith who never learned it found the heart", anvil.heartedForge(smith(helper, false)));
        assertNull(anvil.heartedForge(null));
        assertSame(cold, anvil.heartedForge(smith(helper, true)));
        assertTrue(far.dragonHeartPower > 0);
        helper.succeed();
    }
}
