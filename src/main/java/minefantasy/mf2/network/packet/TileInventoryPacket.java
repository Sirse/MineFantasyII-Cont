package minefantasy.mf2.network.packet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import minefantasy.mf2.network.NetworkUtils;
import minefantasy.mf2.util.MFLogUtil;

public class TileInventoryPacket extends PacketMF {

    public static final String packetName = "MF2_TileInvPacket";
    private int[] coords = new int[3];
    private int invSize;
    private IInventory inventory;

    public TileInventoryPacket(IInventory inv, TileEntity tile) {
        this.coords = new int[] { tile.xCoord, tile.yCoord, tile.zCoord };
        this.inventory = inv;
        this.invSize = inv.getSizeInventory();
    }

    public TileInventoryPacket() {}

    @Override
    public void process(ByteBuf packet, EntityPlayer player) {
        if (NetworkUtils.isServer(player)) {
            return;
        }

        int x = packet.readInt();
        int y = packet.readInt();
        int z = packet.readInt();
        int size = packet.readInt();

        TileEntity entity = player.worldObj.getTileEntity(x, y, z);

        if (entity instanceof IInventory) {
            IInventory inv = (IInventory) entity;
            int limit = Math.min(size, inv.getSizeInventory());
            for (int s = 0; s < limit; s++) {
                ItemStack item = ByteBufUtils.readItemStack(packet);
                inv.setInventorySlotContents(s, item);
            }

            for (int s = limit; s < size; s++) {
                ByteBufUtils.readItemStack(packet);
                MFLogUtil.logDebug("Dropped Packet Item " + s);
            }
        }
    }

    @Override
    public String getChannel() {
        return packetName;
    }

    @Override
    public void write(ByteBuf packet) {
        packet.writeInt(coords[0]);
        packet.writeInt(coords[1]);
        packet.writeInt(coords[2]);
        packet.writeInt(invSize);
        for (int s = 0; s < invSize; s++) {
            ByteBufUtils.writeItemStack(packet, inventory.getStackInSlot(s));
        }
    }
}
