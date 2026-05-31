package minefantasy.mf2.mechanics;

import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.world.WorldServer;

import minefantasy.mf2.api.weapon.WeaponClass;
import minefantasy.mf2.network.packet.HitSoundPacket;

public class HitSoundGenerator {

    public static void makeHitSound(ItemStack weapon, Entity target) {
        if (target == null || target.worldObj == null) {
            return;
        }
        WeaponClass WC = WeaponClass.findClassForAny(weapon);
        String type = "blunt";

        String material = getMaterial(weapon);
        if (WC != null) {
            type = WC.getSound();
            String sndString = "minefantasy2:weapon.hit." + type + "." + material;

            if (!target.worldObj.isRemote) {
                ((WorldServer) target.worldObj).getEntityTracker()
                        .func_151248_b(target, new HitSoundPacket(sndString, target).generatePacket());
            }
        }
    }

    public static String getMaterial(ItemStack itemstack) {
        if (itemstack == null) {
            return "metal";
        }
        Item item = itemstack.getItem();
        String material = "metal";

        if (item instanceof ItemTool) {
            material = getMaterialFromName(((ItemTool) item).getToolMaterialName(), material);
        }

        if (item instanceof ItemSword) {
            material = getMaterialFromName(((ItemSword) item).getToolMaterialName(), material);
        }

        return material;
    }

    private static String getMaterialFromName(String toolMaterialName, String fallback) {
        if (toolMaterialName == null) {
            return fallback;
        }
        if ("WOOD".equalsIgnoreCase(toolMaterialName)) {
            return "wood";
        }
        if ("STONE".equalsIgnoreCase(toolMaterialName)) {
            return "stone";
        }
        return fallback;
    }
}
