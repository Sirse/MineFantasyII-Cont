package minefantasy.mf2.network.packet;

import net.minecraft.entity.player.EntityPlayer;

import io.netty.buffer.ByteBuf;
import minefantasy.mf2.api.knowledge.InformationBase;
import minefantasy.mf2.api.knowledge.InformationList;
import minefantasy.mf2.api.knowledge.ResearchLogic;
import minefantasy.mf2.network.NetworkUtils;

public class ResearchRequest extends PacketMF {

    public static final String packetName = "MF2_RequestResearch";
    private static final String LAST_REQUEST_TICK_NBT = "MF2_LastResearchReq";
    private static final long REQUEST_COOLDOWN_TICKS = 2L;
    private EntityPlayer user;
    private int researchID;

    public ResearchRequest(EntityPlayer user, int id) {
        this.researchID = id;
        this.user = user;
    }

    public ResearchRequest() {}

    @Override
    public void process(ByteBuf packet, EntityPlayer player) {
        if (!NetworkUtils.isServer(player)) {
            return;
        }
        researchID = packet.readInt();
        if (researchID < 0 || researchID >= InformationList.knowledgeList.size()) {
            return;
        }
        long now = player.worldObj.getTotalWorldTime();
        long last = player.getEntityData().getLong(LAST_REQUEST_TICK_NBT);
        if (now - last < REQUEST_COOLDOWN_TICKS) {
            return;
        }
        player.getEntityData().setLong(LAST_REQUEST_TICK_NBT, now);

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
