package minefantasy.mf2.network.packet;

import java.util.ArrayList;

import net.minecraft.entity.player.EntityPlayer;

import io.netty.buffer.ByteBuf;
import minefantasy.mf2.api.knowledge.InformationBase;
import minefantasy.mf2.api.knowledge.InformationList;
import minefantasy.mf2.api.knowledge.ResearchLogic;
import minefantasy.mf2.network.NetworkUtils;

public class KnowledgePacket extends PacketMF {

    public static final String packetName = "MF2_KnowledgeSync";
    private EntityPlayer user;

    public KnowledgePacket(EntityPlayer user) {
        this.user = user;
    }

    public KnowledgePacket() {}

    @Override
    public void process(ByteBuf packet, EntityPlayer player) {
        if (NetworkUtils.isServer(player)) {
            return;
        }
        apply(packet, player);
    }

    /**
     * Makes the player's research what the server sent, locking what it no longer has too. The whole packet is read
     * before anything changes, so a short one changes nothing.
     */
    static void apply(ByteBuf packet, EntityPlayer player) {
        int size = InformationList.knowledgeList.size();
        ArrayList<Object[]> completed = new ArrayList<>();
        for (int a = 0; a < size; a++) {
            boolean unlocked = packet.readBoolean();
            int artefactCount = packet.readInt();

            InformationBase base = InformationList.knowledgeList.get(a);
            if (base != null) {
                completed.add(new Object[] { base, unlocked, artefactCount });
            }
        }
        for (Object[] entry : completed) {
            InformationBase base = (InformationBase) entry[0];
            ResearchLogic.setArtefactCount(base.getUnlocalisedName(), player, (Integer) entry[2]);
            ResearchLogic.setUnlocked(player, base, (Boolean) entry[1]);
        }
    }

    @Override
    public String getChannel() {
        return packetName;
    }

    @Override
    public void write(ByteBuf packet) {
        int size = InformationList.knowledgeList.size();

        for (int a = 0; a < size; a++) {
            InformationBase base = InformationList.knowledgeList.get(a);
            packet.writeBoolean(ResearchLogic.hasInfoUnlocked(user, base));
            packet.writeInt(ResearchLogic.getArtefactCount(base.getUnlocalisedName(), user));
        }
    }
}
