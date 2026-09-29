package minefantasy.mf2.block.tileentity;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

import minefantasy.mf2.network.NetworkUtils;
import minefantasy.mf2.network.packet.StationStatePacket;

/**
 * A block whose players see some of its state: it describes that state as one tag, which reaches players who come near
 * with the chunk's description packet, and the players already watching whenever {@link #sendState} finds it changed.
 * Both are applied by {@link #show}, so what a player sees does not depend on which of the two came last.
 */
public abstract class TileEntityShown extends TileEntity implements StationStatePacket.Shown {

    /** What the watchers last got; null until the first send. */
    private NBTTagCompound sent;

    /** The state players see; null for none. Built on the server, so it must not read client-only fields. */
    protected NBTTagCompound describe() {
        return null;
    }

    /** Applies a described state on the client. */
    @Override
    public void show(NBTTagCompound state) {}

    /** Sends the state to the players watching the block when it differs from what they last got, or always. */
    protected void sendState(boolean always) {
        if (worldObj == null || worldObj.isRemote) {
            return;
        }
        NBTTagCompound state = describe();
        if (state == null || !always && state.equals(sent)) {
            return;
        }
        sent = (NBTTagCompound) state.copy();
        send(state);
    }

    /**
     * Sends a passing moment, such as a lever pull starting its swing, to the players watching now. It is not part of
     * the state, so players who come later do not replay it; {@link #show} gets it like a state and should apply only
     * what it carries.
     */
    protected void sendMoment(NBTTagCompound moment) {
        if (worldObj == null || worldObj.isRemote) {
            return;
        }
        send(moment);
    }

    /** Sends a tag to the players watching the block, for {@link #show} on their side. */
    protected void send(NBTTagCompound tag) {
        NetworkUtils.sendToWatchers(new StationStatePacket(this, tag).generatePacket(), worldObj, xCoord, zCoord);
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound state = describe();
        return state == null ? null : new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, state);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
        show(packet.func_148857_g());
    }
}
