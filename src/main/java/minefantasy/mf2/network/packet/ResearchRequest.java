package minefantasy.mf2.network.packet;

import io.netty.buffer.ByteBuf;
import minefantasy.mf2.api.knowledge.InformationBase;
import minefantasy.mf2.api.knowledge.InformationList;
import minefantasy.mf2.api.knowledge.ResearchLogic;
import minefantasy.mf2.network.NetworkUtils;
import net.minecraft.entity.player.EntityPlayer;

public class ResearchRequest extends PacketMF {
    public static final String packetName = "MF2_RequestResearch";
    private EntityPlayer user;
    private int researchID;

    public ResearchRequest(EntityPlayer user, int id) {
        this.researchID = id;
        this.user = user;
    }

    public ResearchRequest() {
    }

    @Override
    public void process(ByteBuf packet, EntityPlayer player) {
        researchID = packet.readInt();
        if (!NetworkUtils.isServer(player)) {
            return;
        }

        InformationBase research = InformationList.knowledgeList.get(researchID);
        if (research != null && research.isEasy()) {
            if (research.onPurchase(player)) {
                ResearchLogic.syncData(player);
            }
        }
    }

    @Override
    public String getChannel() {
        return packetName;
    }

    @Override
    public void write(ByteBuf packet) {
        packet.writeInt(researchID);
    }
}
