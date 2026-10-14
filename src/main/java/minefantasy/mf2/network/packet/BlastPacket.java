package minefantasy.mf2.network.packet;

import net.minecraft.entity.player.EntityPlayer;

import io.netty.buffer.ByteBuf;
import minefantasy.mf2.MineFantasyII;

/**
 * Tells players near a blast what to draw: its centre, size and filling, and the seed its fragments flew from, so the
 * client traces the same paths the server hurt along.
 */
public class BlastPacket extends PacketMF {

    public static final String packetName = "MF2_Blast";
    private static final int SIZE = 8 * 3 + 4 + 1 + 1 + 1 + 4 + 8;
    /** Larger than any blast the components make, to refuse nonsense rather than draw it. */
    private static final float MAX_RADIUS = 64F;
    private static final int MAX_FRAGMENTS = 64;

    private double x, y, z;
    private float radius;
    private int filling;
    private boolean upward;
    private int fragments;
    private float fragmentRange;
    private long seed;

    public BlastPacket(double x, double y, double z, float radius, int filling, boolean upward, int fragments,
            float fragmentRange, long seed) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
        this.filling = filling;
        this.upward = upward;
        this.fragments = fragments;
        this.fragmentRange = fragmentRange;
        this.seed = seed;
    }

    public BlastPacket() {}

    @Override
    public void process(ByteBuf in, EntityPlayer player) {
        if (player == null || isServer(player)) return;
        BlastPacket blast = read(in);
        if (blast == null) return;
        MineFantasyII.proxy.showBlast(
                player.worldObj,
                blast.x,
                blast.y,
                blast.z,
                blast.radius,
                blast.filling,
                blast.upward,
                blast.fragments,
                blast.fragmentRange,
                blast.seed);
    }

    /** The blast a payload describes, or null for one cut short or out of range. */
    static BlastPacket read(ByteBuf in) {
        if (!hasPayload(in, SIZE)) return null;
        BlastPacket blast = new BlastPacket(
                in.readDouble(),
                in.readDouble(),
                in.readDouble(),
                in.readFloat(),
                in.readByte(),
                in.readBoolean(),
                in.readUnsignedByte(),
                in.readFloat(),
                in.readLong());
        if (!finite(blast.x) || !finite(blast.y) || !finite(blast.z)) return null;
        if (!finite(blast.radius) || !finite(blast.fragmentRange)) return null;
        if (blast.radius <= 0F || blast.radius > MAX_RADIUS) return null;
        if (blast.fragmentRange < 0F || blast.fragmentRange > MAX_RADIUS * 2) return null;
        if (blast.fragments > MAX_FRAGMENTS) return null;
        return blast;
    }

    private static boolean finite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    @Override
    public String getChannel() {
        return packetName;
    }

    @Override
    public void write(ByteBuf out) {
        out.writeDouble(x);
        out.writeDouble(y);
        out.writeDouble(z);
        out.writeFloat(radius);
        out.writeByte(filling);
        out.writeBoolean(upward);
        out.writeByte(fragments);
        out.writeFloat(fragmentRange);
        out.writeLong(seed);
    }
}
