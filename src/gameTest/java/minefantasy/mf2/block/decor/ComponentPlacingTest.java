package minefantasy.mf2.block.decor;

import static minefantasy.mf2.gametest.Assert.*;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.gametest.DenyPlacing;
import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.gametest.Overrides;
import minefantasy.mf2.item.list.ComponentListMF;

/** Setting a component down in the world is placing a block: where placing is refused, it stays in hand. */
@GameTestHolder("minefantasy2")
public class ComponentPlacingTest {

    private ComponentPlacingTest() {}

    @GameTest
    public static void aComponentIsSetDownOnlyWherePlacingIsAllowed(GameTestHelper helper) {
        helper.setBlock(1, 0, 1, Blocks.stone);
        helper.setBlock(1, 1, 1, Blocks.air);
        TestPos at = helper.absolute(1, 1, 1);
        FakePlayer player = Modders.fresh(helper, Modders.SMITH);
        player.setPositionAndRotation(at.x() + 0.5, at.y() + 1, at.z() + 0.5, 0F, 90F);
        ItemStack moulds = new ItemStack(ComponentListMF.ingot_mould, 4);
        player.setCurrentItemOrArmor(0, moulds);
        DenyPlacing deny = new DenyPlacing();
        try (Overrides overrides = new Overrides().listen(deny)) {
            moulds.getItem().onItemRightClick(moulds, helper.getWorld(), player);
        }
        assertEquals("the place event was not asked", 1, deny.refused);
        assertSame("set down where placing is refused", Blocks.air, helper.getWorld().getBlock(at.x(), at.y(), at.z()));
        assertEquals(4, moulds.stackSize);
        moulds.getItem().onItemRightClick(moulds, helper.getWorld(), player);
        assertSame(BlockListMF.components, helper.getWorld().getBlock(at.x(), at.y(), at.z()));
        assertEquals(3, moulds.stackSize);
        helper.succeed();
    }
}
