package minefantasy.mf2.network.packet;

import static minefantasy.mf2.gametest.Assert.*;

import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import minefantasy.mf2.api.knowledge.InformationBase;
import minefantasy.mf2.api.knowledge.InformationList;
import minefantasy.mf2.api.knowledge.ResearchLogic;
import minefantasy.mf2.gametest.Modders;

/** The client's research becomes what the server sent, locked entries included. */
@GameTestHolder("minefantasy2")
public class KnowledgePacketTest {

    private KnowledgePacketTest() {}

    private static InformationBase lockable() {
        for (InformationBase base : InformationList.knowledgeList) {
            if (base != null && !base.isPreUnlocked()) return base;
        }
        throw new AssertionError("no research that starts locked");
    }

    @GameTest
    public static void researchTheServerNoLongerHasIsLockedOnTheClient(GameTestHelper helper) {
        FakePlayer player = Modders.fresh(helper, Modders.SMITH);
        InformationBase research = lockable();
        ResearchLogic.setUnlocked(player, research, false);
        ByteBuf sent = Unpooled.buffer();
        new KnowledgePacket(player).write(sent);

        // The client still has it from before, say a reset on the server
        ResearchLogic.forceUnlock(player, research);
        KnowledgePacket.apply(sent, player);
        assertFalse("the client kept research the server locked", ResearchLogic.hasInfoUnlocked(player, research));

        ResearchLogic.forceUnlock(player, research);
        ByteBuf withIt = Unpooled.buffer();
        new KnowledgePacket(player).write(withIt);
        ResearchLogic.setUnlocked(player, research, false);
        KnowledgePacket.apply(withIt, player);
        assertTrue("the client did not take unlocked research", ResearchLogic.hasInfoUnlocked(player, research));
        helper.succeed();
    }
}
