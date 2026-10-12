package minefantasy.mf2.mechanics;

import static minefantasy.mf2.gametest.Assert.*;

import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.item.list.ToolListMF;

/** A kill counts as the hunter's own, for the hide and meat a knife retrieves, only when the knife dealt it. */
@GameTestHolder("minefantasy2")
public class HunterKillTest {

    private HunterKillTest() {}

    private static boolean hunterKill(GameTestHelper helper, FakePlayer player, DamageSource source) {
        EntityPig pig = new EntityPig(helper.getWorld());
        pig.setPosition(player.posX, player.posY, player.posZ);
        pig.attackEntityFrom(source, 1000F);
        assertTrue("the pig survived", pig.getHealth() <= 0F);
        return pig.getEntityData().hasKey("hunterKill");
    }

    @GameTest
    public static void onlyAKnifeBlowIsAHunterKill(GameTestHelper helper) {
        FakePlayer player = Modders.fresh(helper, Modders.SMITH);
        player.setCurrentItemOrArmor(0, new ItemStack(ToolListMF.knifeStone));
        assertTrue("a knife blow", hunterKill(helper, player, DamageSource.causePlayerDamage(player)));
        EntityArrow arrow = new EntityArrow(helper.getWorld());
        assertFalse(
                "an arrow shot with a knife in hand",
                hunterKill(helper, player, DamageSource.causeArrowDamage(arrow, player)));
        helper.succeed();
    }
}
