package minefantasy.mf2.client;

import java.util.List;

import net.minecraft.entity.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.oredict.OreDictionary;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import minefantasy.mf2.MineFantasyII;
import minefantasy.mf2.api.armour.ISpecialArmourMF;
import minefantasy.mf2.api.armour.ItemArmourMFBase;
import minefantasy.mf2.api.helpers.*;
import minefantasy.mf2.api.helpers.ItemQuality;
import minefantasy.mf2.api.knowledge.ResearchLogic;
import minefantasy.mf2.api.material.CustomMaterial;
import minefantasy.mf2.api.rpg.RPGElements;
import minefantasy.mf2.api.weapon.WeaponClass;
import minefantasy.mf2.item.ClientItemsMF;
import minefantasy.mf2.item.weapon.ItemWeaponMF;
import minefantasy.mf2.mechanics.*;

/** What item tooltips add: materials, research, quality, armour protection, damage types and crafting stats. */
public class TooltipEvents {

    /** Lists the ore dictionary names of every item, for pack makers. */
    public static boolean displayOreDict;

    @SubscribeEvent
    public void setTooltip(ItemTooltipEvent event) {
        if (!event.entity.worldObj.isRemote) {
            return;
        }

        if (event.itemStack != null) {
            boolean saidArtefact = false;
            int[] ids = OreDictionary.getOreIDs(event.itemStack);
            boolean hasInfo = false;
            if (ids != null) {
                for (int id : ids) {
                    String s = OreDictionary.getOreName(id);
                    if (s != null) {
                        if (!hasInfo && s.startsWith("ingot")) {
                            String s2 = s.substring(5, s.length());
                            CustomMaterial material = CustomMaterial.getMaterial(s2);
                            if (material != null) hasInfo = true;

                            CustomToolHelper.addComponentString(event.itemStack, event.toolTip, material);
                        }
                        if (s.startsWith("Artefact-")) {
                            if (!saidArtefact) {
                                String knowledge = s.substring(9).toLowerCase();

                                if (!ResearchLogic.hasInfoUnlocked(event.entityPlayer, knowledge)) {
                                    saidArtefact = true;
                                    event.toolTip.add(
                                            EnumChatFormatting.AQUA
                                                    + StatCollector.translateToLocal("info.hasKnowledge"));
                                }
                            }
                        } else if (displayOreDict) {
                            event.toolTip.add("oreDict: " + s);
                        }
                    }
                }
            }

            ItemQuality.Grade grade = ItemQuality.getGrade(event.itemStack);
            if (grade == ItemQuality.Grade.INFERIOR) {
                event.toolTip.add(EnumChatFormatting.RED + StatCollector.translateToLocal("attribute.inferior.name"));
            } else if (grade == ItemQuality.Grade.SUPERIOR) {
                event.toolTip.add(EnumChatFormatting.GREEN + StatCollector.translateToLocal("attribute.superior.name"));
            }
            if (event.itemStack.getItem() instanceof ItemArmor
                    && (!(event.itemStack.getItem() instanceof ItemArmourMFBase) || ClientItemsMF.showSpecials(
                            event.itemStack,
                            event.entityPlayer,
                            event.toolTip,
                            event.showAdvancedItemTooltips))) {
                addArmourDR(event.itemStack, event.entityPlayer, event.toolTip, event.showAdvancedItemTooltips);
            }
            if (ArmourCalculator.advancedDamageTypes && ArmourCalculator.getRatioForWeapon(event.itemStack) != null) {
                displayWeaponTraits(ArmourCalculator.getRatioForWeapon(event.itemStack), event.toolTip);
            }
            if (ToolHelper.shouldShowTooltip(event.itemStack)) {
                showCrafterTooltip(event.itemStack, event.toolTip);
            }
            if (event.itemStack.hasTagCompound() && event.itemStack.getTagCompound().hasKey("MF_CraftedByName")) {
                String name = event.itemStack.getTagCompound().getString("MF_CraftedByName");
                boolean special = MineFantasyII.isNameModder(name);// Mod creators have highlights

                event.toolTip.add(
                        (special ? EnumChatFormatting.GREEN : "") + StatCollector.translateToLocal(
                                "attribute.mfcraftedbyname.name") + ": " + name + EnumChatFormatting.GRAY);
            }
            WeaponClass WC = WeaponClass.findClassForAny(event.itemStack);
            if (WC != null && RPGElements.isSystemActive && WC.parentSkill != null) {
                event.toolTip.add(StatCollector.translateToLocal("weaponclass." + WC.name.toLowerCase()));
                float skillMod = RPGElements.getWeaponModifier(event.entityPlayer, WC.parentSkill) * 100F;
                if (skillMod > 100) event.toolTip.add(
                        StatCollector.translateToLocal("rpg.skillmod")
                                + ItemWeaponMF.decimal_format.format(skillMod - 100)
                                + "%");

            }
        }
    }

    private void displayWeaponTraits(float[] ratio, List<String> list) {
        int cutting = (int) (ratio[0] / (ratio[0] + ratio[1] + ratio[2]) * 100F);
        int piercing = (int) (ratio[2] / (ratio[0] + ratio[1] + ratio[2]) * 100F);
        int blunt = (int) (ratio[1] / (ratio[0] + ratio[1] + ratio[2]) * 100F);

        addDamageType(list, cutting, "cutting");
        addDamageType(list, piercing, "piercing");
        addDamageType(list, blunt, "blunt");
    }

    private void addDamageType(List<String> list, int value, String name) {
        if (value > 0) {
            String s = StatCollector.translateToLocal("attribute.weapon." + name);
            if (value < 100) {
                s += " " + value + "%";
            }
            list.add(s);
        }
    }

    private void showCrafterTooltip(ItemStack tool, List<String> list) {
        String toolType = ToolHelper.getCrafterTool(tool);
        int tier = ToolHelper.getCrafterTier(tool);
        float efficiency = ToolHelper.getCrafterEfficiency(tool);

        list.add(
                StatCollector.translateToLocal("attribute.mfcrafttool.name") + ": "
                        + StatCollector.translateToLocal("tooltype." + toolType));
        list.add(StatCollector.translateToLocal("attribute.mfcrafttier.name") + ": " + tier);
        list.add(StatCollector.translateToLocal("attribute.mfcrafteff.name") + ": " + efficiency);
    }

    public static void addArmourDR(ItemStack armour, EntityPlayer user, List<String> list, boolean extra) {
        list.add("");
        String AC = ArmourCalculator.getArmourClass(armour);
        if (AC != null) {
            list.add(StatCollector.translateToLocal("attribute.armour." + AC));
        }
        if (armour.getItem() instanceof ISpecialArmourMF) {
            if (ArmourCalculator.advancedDamageTypes) {
                list.add(EnumChatFormatting.BLUE + StatCollector.translateToLocal("attribute.armour.protection"));
                addSingleDR(armour, user, 0, list, extra, true);
                addSingleDR(armour, user, 2, list, extra, true);
                addSingleDR(armour, user, 1, list, extra, true);
            } else {
                addSingleDR(armour, user, 0, list, extra, false);
            }
        }
    }

    public static void addSingleDR(ItemStack armour, EntityPlayer user, int id, List<String> list, boolean extra,
            boolean advanced) {
        int slot = ((ItemArmor) armour.getItem()).armorType;
        String attatch = "";

        int rating = (int) (ArmourCalculator.getDRForDisplayPiece(armour, id) * 100F);
        int equipped = (int) (ArmourCalculator.getDRForDisplayPiece(user.getCurrentArmor(3 - slot), id) * 100F);

        if (rating > 0 || equipped > 0) {
            if (equipped > 0 && rating != equipped) {
                float d = rating - equipped;
                if (d > 0) {
                    attatch += EnumChatFormatting.DARK_GREEN;
                }
                if (d < 0) {
                    attatch += EnumChatFormatting.RED;
                }
                String d2 = ItemWeaponMF.decimal_format.format(d);
                attatch += " (" + (d > 0 ? "+" : "") + d2 + ")";
            }
            if (advanced) {
                list.add(
                        EnumChatFormatting.BLUE + StatCollector.translateToLocal("attribute.armour.rating." + id)
                                + " "
                                + rating
                                + attatch);
            } else {
                list.add(
                        EnumChatFormatting.BLUE + StatCollector.translateToLocal("attribute.armour.protection")
                                + ": "
                                + rating
                                + attatch);
            }
        }
    }
}
