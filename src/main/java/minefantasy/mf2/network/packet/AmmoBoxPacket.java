package minefantasy.mf2.network.packet;

import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import minefantasy.mf2.block.tileentity.decor.TileEntityAmmoBox;
import minefantasy.mf2.network.NetworkUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

public class AmmoBoxPacket extends PacketMF {
    public static final String packetName = "MF2_AmmoBoxPkt";
    private int[] coords = new int[3];
    private int stock;
    private ItemStack ammo;
    private int angle;

    public AmmoBoxPacket(TileEntityAmmoBox tile) {
        this.coords = new int[]{tile.xCoord, tile.yCoord, tile.zCoord};
        this.ammo = tile.ammo;
        this.stock = tile.stock;
        this.angle = tile.angle;
    }

    public AmmoBoxPacket() {
    }

    @Override
    public void process(ByteBuf packet, EntityPlayer player) {
        if (NetworkUtils.isServer(player)) {
            return;
        }

        coords = NetworkUtils.readCoords(packet);
        int newStock = packet.readInt();
        ItemStack newAmmo = ByteBufUtils.readItemStack(packet);
        int newAngle = packet.readInt();

        TileEntity entity = player.worldObj.getTileEntity(coords[0], coords[1], coords[2]);
        if (!(entity instanceof TileEntityAmmoBox)) {
            return;
        }

        TileEntityAmmoBox tile = (TileEntityAmmoBox) entity;
        boolean changed = tile.setContentsValidated(newAmmo, newStock);

        tile.angle = newAngle;
        if (changed && player.worldObj != null) {
            player.worldObj.markBlockForUpdate(coords[0], coords[1], coords[2]);
        }
    }

    @Override
    public String getChannel() {
        return packetName;
    }

    @Override
    public void write(ByteBuf packet) {
        NetworkUtils.writeCoords(packet, coords[0], coords[1], coords[2]);
        packet.writeInt(stock);
        ByteBufUtils.writeItemStack(packet, ammo);
        packet.writeInt(angle);
    }
}
