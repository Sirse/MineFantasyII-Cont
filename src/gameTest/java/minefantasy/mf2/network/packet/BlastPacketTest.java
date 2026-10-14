package minefantasy.mf2.network.packet;

import static minefantasy.mf2.gametest.Assert.*;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/** A blast arrives as it was sent, and one cut short or out of range is not drawn. */
@GameTestHolder("minefantasy2")
public class BlastPacketTest {

    private BlastPacketTest() {}

    private static ByteBuf written(BlastPacket packet) {
        ByteBuf out = Unpooled.buffer();
        packet.write(out);
        return out;
    }

    private static BlastPacket blast(double x, float radius, int fragments, float range) {
        return new BlastPacket(x, 64.5D, -12.25D, radius, 2, true, fragments, range, 1234567890123L);
    }

    @GameTest
    public static void aBlastArrivesAsItWasSent(GameTestHelper helper) {
        BlastPacket sent = blast(100.5D, 5F, 24, 7.5F);
        BlastPacket read = BlastPacket.read(written(sent));
        assertNotNull("a sound blast was refused", read);
        assertEquals("the blast changed on the way", written(sent), written(read));
        helper.succeed();
    }

    @GameTest
    public static void aBlastCutShortOrOutOfRangeIsRefused(GameTestHelper helper) {
        ByteBuf whole = written(blast(0D, 5F, 24, 7.5F));
        assertNull("a payload cut short was read", BlastPacket.read(whole.slice(0, whole.readableBytes() - 1)));
        assertNull("not a number", BlastPacket.read(written(blast(Double.NaN, 5F, 24, 7.5F))));
        assertNull("infinite", BlastPacket.read(written(blast(Double.POSITIVE_INFINITY, 5F, 24, 7.5F))));
        assertNull("no radius", BlastPacket.read(written(blast(0D, 0F, 24, 7.5F))));
        assertNull("too wide", BlastPacket.read(written(blast(0D, 1000F, 24, 7.5F))));
        assertNull("too many fragments", BlastPacket.read(written(blast(0D, 5F, 200, 7.5F))));
        assertNull("fragments flying too far", BlastPacket.read(written(blast(0D, 5F, 24, 1000F))));
        helper.succeed();
    }
}
