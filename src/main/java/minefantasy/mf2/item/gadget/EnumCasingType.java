package minefantasy.mf2.item.gadget;

public enum EnumCasingType {

    CRUDE("crude", 0.5F, 3F, 1.0F, true, true),
    CERAMIC("ceramic", 1.0F, 5F, 1.0F, true, false),
    IRON("iron", 1.5F, 4F, 2.0F, true, false),
    OBSIDIAN("obsidian", 2.0F, 4F, 1.5F, false, false),
    CRYSTAL("crystal", 1.5F, 5F, 0.75F, true, false);

    public String name;
    public float damageModifier;
    public float rangeModifier;
    public float weightModifier;
    /** Whether the blast weakens past half its radius; obsidian hurts fully to the edge. */
    public final boolean falloff;
    /** Whether a blast nearby sets it off: a crude casing is too poorly made to hold. */
    public final boolean setOffByBlasts;

    private EnumCasingType(String name, float damage, float range, float weight, boolean falloff,
            boolean setOffByBlasts) {
        this.name = name;
        this.damageModifier = damage;
        this.rangeModifier = range;
        this.weightModifier = weight;
        this.falloff = falloff;
        this.setOffByBlasts = setOffByBlasts;
    }

    public static EnumCasingType getType(byte i) {
        if (i == -1) {
            return CRUDE;
        }
        if (i == 1) {
            return IRON;
        }
        if (i == 2) {
            return OBSIDIAN;
        }
        if (i == 3) {
            return CRYSTAL;
        }
        return CERAMIC;
    }
}
