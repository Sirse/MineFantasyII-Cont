package minefantasy.mf2.item.gadget;

public enum EnumExplosiveType {

    BASIC("basic", 24F, 1.0F, 0, 0F),
    SHRAPNEL("shrapnel", 24F, 1.0F, 24, 4F),
    FIRE("fire", 32F, 1.0F, 16, 2F);

    public String name;
    /** Damage of the blast itself, blunt, which blast protection lessens. */
    public float damage;
    public float range;
    /**
     * Fragments thrown, each hurting the first living thing in its path as a blunt and piercing projectile, which
     * projectile protection lessens.
     */
    public final int fragments;
    public final float fragmentDamage;

    private EnumExplosiveType(String name, float damage, float range, int fragments, float fragmentDamage) {
        this.name = name;
        this.damage = damage;
        this.range = range;
        this.fragments = fragments;
        this.fragmentDamage = fragmentDamage;
    }

    public static EnumExplosiveType getType(byte i) {
        if (i == 1) {
            return SHRAPNEL;
        }
        if (i == 2) {
            return FIRE;
        }
        return BASIC;
    }
}
