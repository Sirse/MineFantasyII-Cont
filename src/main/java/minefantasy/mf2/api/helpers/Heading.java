package minefantasy.mf2.api.helpers;

import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;

/** Which way an entity faces, in quarter turns from south: 0 south, 1 west, 2 north, 3 east. */
public final class Heading {

    private Heading() {}

    public static int of(Entity entity) {
        return MathHelper.floor_double(entity.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
    }

    /** The quarter turn facing back towards the entity. */
    public static int towards(Entity entity) {
        return (of(entity) + 2) & 3;
    }
}
