package minefantasy.mf2.network.packet;

import static minefantasy.mf2.gametest.Assert.*;

import net.minecraft.entity.player.EntityPlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import io.netty.buffer.ByteBuf;

/** A packet carries what was written to it and nothing more, at and around the buffer's growth. */
@GameTestHolder("minefantasy2")
public class PacketLengthTest {

    private PacketLengthTest() {}

    private static PacketMF bytes(final int count) {
        return new PacketMF() {

            @Override
            public String getChannel() {
                return "MF2_Test";
            }

            @Override
            public void write(ByteBuf out) {
                for (int i = 0; i < count; i++) out.writeByte(i);
            }

            @Override
            public void process(ByteBuf in, EntityPlayer user) {}
        };
    }

    @GameTest
    public static void aPacketSendsOnlyWhatWasWritten(GameTestHelper helper) {
        for (int count : new int[] { 0, 4, 15, 16, 17, 49, 64, 65 }) {
            FMLProxyPacket packet = bytes(count).generatePacket();
            // What toS3FPacket and toC17Packet send
            byte[] sent = packet.payload().array();
            assertEquals("length of " + count, count, sent.length);
            for (int i = 0; i < count; i++) assertEquals("byte " + i, (byte) i, sent[i]);
        }
        helper.succeed();
    }
}
