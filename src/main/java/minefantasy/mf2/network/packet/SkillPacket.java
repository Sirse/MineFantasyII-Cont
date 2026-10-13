package minefantasy.mf2.network.packet;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import minefantasy.mf2.api.rpg.RPGElements;
import minefantasy.mf2.api.rpg.Skill;
import minefantasy.mf2.network.NetworkUtils;

/** Every skill of a player in one packet: name, level, xp and the xp to the next level. */
public class SkillPacket extends PacketMF {

    public static final String packetName = "MF2_SkillSync";
    /** More skills than any pack registers; a longer list is a broken packet. */
    private static final int MAX_SKILLS = 256;
    private final List<String> names = new ArrayList<>();
    private final List<int[]> values = new ArrayList<>();

    public SkillPacket(EntityPlayer user) {
        for (Skill skill : RPGElements.skillsList) {
            if (skill == null) continue;
            NBTTagCompound tag = RPGElements.getSkill(user, skill.skillName);
            names.add(skill.skillName);
            values.add(new int[] { tag.getInteger("level"), tag.getInteger("xp"), tag.getInteger("xpMax") });
        }
    }

    public SkillPacket() {}

    @Override
    public void process(ByteBuf packet, EntityPlayer player) {
        if (NetworkUtils.isServer(player)) {
            return;
        }
        int count = packet.readInt();
        if (count < 0 || count > MAX_SKILLS) {
            return;
        }
        for (int i = 0; i < count; i++) {
            String name = ByteBufUtils.readUTF8String(packet);
            int level = packet.readInt();
            int xp = packet.readInt();
            int xpMax = packet.readInt();
            // Only skills this side knows, so a stray name makes no tag
            if (!RPGElements.isSkill(name)) continue;
            NBTTagCompound tag = RPGElements.getSkill(player, name);
            tag.setInteger("level", level);
            tag.setInteger("xp", xp);
            tag.setInteger("xpMax", xpMax);
        }
    }

    @Override
    public String getChannel() {
        return packetName;
    }

    @Override
    public void write(ByteBuf packet) {
        packet.writeInt(names.size());
        for (int i = 0; i < names.size(); i++) {
            int[] value = values.get(i);
            ByteBufUtils.writeUTF8String(packet, names.get(i));
            packet.writeInt(value[0]);
            packet.writeInt(value[1]);
            packet.writeInt(value[2]);
        }
    }
}
