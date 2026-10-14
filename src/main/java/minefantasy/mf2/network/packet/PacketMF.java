package minefantasy.mf2.network.packet;

import net.minecraft.entity.player.EntityPlayer;

import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public abstract class PacketMF {

    public final FMLProxyPacket generatePacket() {
        ByteBuf buf = Unpooled.buffer(16);
        write(buf);
        // Forge sends the whole backing array, so hand it one holding exactly what was written
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        return new FMLProxyPacket(Unpooled.wrappedBuffer(data), getChannel());
    }

    public abstract String getChannel();

    public abstract void write(ByteBuf out);

    public abstract void process(ByteBuf in, EntityPlayer user);

    protected static boolean isServer(EntityPlayer player) {
        return player != null && !player.worldObj.isRemote;
    }

    protected static boolean hasPayload(ByteBuf buf, int bytes) {
        return buf != null && buf.readableBytes() >= bytes;
    }
}
