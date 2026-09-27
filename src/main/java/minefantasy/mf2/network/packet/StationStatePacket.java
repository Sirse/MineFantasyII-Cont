package minefantasy.mf2.network.packet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import minefantasy.mf2.network.NetworkUtils;

/**
 * A station's state for the players watching it: the same tag its description packet carries, applied the same way, so
 * what a player sees does not depend on which of the two reached them last.
 */
public class StationStatePacket extends PacketMF {

    public static final String packetName = "MF2_StationState";

    /** A station that sends its state this way. */
    public interface Shown {

        /** Applies the state on the client. */
        void show(NBTTagCompound state);
    }

    private int[] coords = new int[3];
    private NBTTagCompound state;

    public StationStatePacket(TileEntity station, NBTTagCompound state) {
        this.coords = new int[] { station.xCoord, station.yCoord, station.zCoord };
        this.state = state;
    }

    public StationStatePacket() {}

    @Override
    public void process(ByteBuf packet, EntityPlayer player) {
        if (NetworkUtils.isServer(player)) {
            return;
        }
        // Locals, not fields: this handler instance is shared by every player through packetList
        int[] at = NetworkUtils.readCoords(packet);
        NBTTagCompound read = ByteBufUtils.readTag(packet);
        TileEntity entity = player.worldObj.getTileEntity(at[0], at[1], at[2]);
        if (entity instanceof Shown && read != null) {
            ((Shown) entity).show(read);
        }
    }

    @Override
    public String getChannel() {
        return packetName;
    }

    @Override
    public void write(ByteBuf packet) {
        NetworkUtils.writeCoords(packet, coords[0], coords[1], coords[2]);
        ByteBufUtils.writeTag(packet, state);
    }
}
