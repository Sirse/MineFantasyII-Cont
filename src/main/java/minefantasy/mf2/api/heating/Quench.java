package minefantasy.mf2.api.heating;

import java.util.Random;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.helpers.ItemQuality;
import minefantasy.mf2.api.material.CustomMaterial;
import minefantasy.mf2.api.rpg.RPGElements;
import minefantasy.mf2.api.rpg.SkillList;

/**
 * Quenching a hot piece: the medium, the heat it is quenched from, its metal and the smith's skill decide whether it
 * hardens, adding quality, or cracks, becoming inferior and losing durability. Only pieces that do not stack, those
 * with a quality, are hardened; the rest just cool.
 */
public final class Quench {

    /** NBT key of the medium a piece was quenched in. */
    public static final String NBT_MEDIUM = "MF_Quench";
    /** NBT key set when quenching cracked the piece. */
    public static final String NBT_CRACKED = "MF_QuenchCracked";
    /** The share of durability a crack takes. */
    public static final float CRACK_WEAR = 0.25F;
    /** The share of the crack chance the greatest skill takes away. */
    public static final float SKILL_SAFETY = 0.6F;

    private Quench() {}

    /** Where a piece is quenched: the medium and how much riskier than a proper bath it is. */
    public static final class Source {

        public final QuenchMedium medium;
        public final float risk;

        public Source(QuenchMedium medium, float risk) {
            if (medium == null) {
                throw new IllegalArgumentException("A quench source needs a medium");
            }
            this.medium = medium;
            this.risk = risk;
        }
    }

    /** The heat a piece is quenched from, against its working heat. */
    public enum Heat {
        /** Below working heat: it does not harden. */
        COLD,
        /** Within working heat, as it should be. */
        RIGHT,
        /** Past its unstable heat: grown grain, brittle. */
        OVERHEATED
    }

    public static Heat heatOf(ItemStack hot) {
        int work = Heatable.getWorkTemp(hot);
        int unstable = Heatable.getUnstableTemp(hot);
        int temp = Heatable.getTemp(hot);
        if (unstable <= work) {
            // A piece made hot without a heat profile has no window to miss
            return Heat.RIGHT;
        }
        if (temp <= work) {
            return Heat.COLD;
        }
        return temp > unstable ? Heat.OVERHEATED : Heat.RIGHT;
    }

    /** Whether the heat is in the middle fifth of the working heat, as the narrowest metals ask. */
    static boolean inTheMiddle(ItemStack hot) {
        int work = Heatable.getWorkTemp(hot);
        int unstable = Heatable.getUnstableTemp(hot);
        if (unstable <= work) {
            return true;
        }
        float at = (Heatable.getTemp(hot) - work) / (float) (unstable - work);
        return at >= 0.4F && at <= 0.6F;
    }

    /**
     * Cools the hot piece in the source, returning the cold piece, or null when the stack holds none. Cracks only
     * happen under the hardcore quench rule; the smith may be null.
     */
    public static ItemStack cool(ItemStack hot, Source source, EntityPlayer smith, Random rand) {
        ItemStack cold = Heatable.getItem(hot);
        if (cold == null || !ItemQuality.applies(cold)) {
            return cold;
        }
        CustomMaterial material = CustomToolHelper.getCustomPrimaryMaterial(cold);
        QuenchResponse response = QuenchResponse.of(material == null ? null : material.name);
        if (!response.matters()) {
            return cold;
        }
        float bonus = response.bonus(source.medium);
        float crack = response.crack(source.medium) / 100F * source.risk;
        Heat heat = heatOf(hot);
        if (heat == Heat.COLD) {
            bonus = 0F;
            crack *= 0.5F;
        } else if (heat == Heat.OVERHEATED) {
            bonus *= 0.5F;
            crack *= 3F;
        }
        if (response.narrowWindow && !inTheMiddle(hot)) {
            bonus = 0F;
            crack = 1F;
        }
        if (smith != null) {
            int max = SkillList.artisanry.getMaxLevel();
            float skill = max > 0 ? Math.min(1F, RPGElements.getLevel(smith, SkillList.artisanry) / (float) max) : 0F;
            crack *= 1F - SKILL_SAFETY * skill;
        }
        if (!Heatable.HCCquenchRuin) {
            crack = 0F;
        }

        NBTTagCompound tag = tag(cold);
        tag.setString(NBT_MEDIUM, source.medium.key());
        if (crack > 0F && rand.nextFloat() < crack) {
            tag.setBoolean(NBT_CRACKED, true);
            ItemQuality.setGrade(cold, ItemQuality.Grade.INFERIOR);
            if (cold.isItemStackDamageable()) {
                int wear = (int) (cold.getMaxDamage() * CRACK_WEAR);
                cold.setItemDamage(Math.min(cold.getMaxDamage() - 1, cold.getItemDamage() + wear));
            }
            return cold;
        }
        if (response.wideSpread) {
            bonus *= rand.nextFloat() * 2F;
        }
        if (bonus != 0F) {
            float quality = ItemQuality.get(cold) + bonus;
            ItemQuality.set(cold, Math.max(ItemQuality.MIN, Math.min(ItemQuality.MAX, quality)));
        }
        return cold;
    }

    private static NBTTagCompound tag(ItemStack item) {
        if (!item.hasTagCompound()) {
            item.setTagCompound(new NBTTagCompound());
        }
        return item.getTagCompound();
    }
}
