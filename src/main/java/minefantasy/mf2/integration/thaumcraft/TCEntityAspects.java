package minefantasy.mf2.integration.thaumcraft;

import minefantasy.mf2.MineFantasyII;
import minefantasy.mf2.util.MFLogUtil;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.ThaumcraftApi.EntityTags;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * Aspects for the MF mobs. Describes them only; no ability, drop or behaviour changes.
 * <p>
 * FML registers a mod entity in EntityList as "modid.name", and scanning matches that string.
 */
public class TCEntityAspects {

    public static void registerAll() {
        int written = 0;
        written += add(
                "MF_Dragon",
                new AspectList().add(Aspect.BEAST, 8).add(Aspect.FIRE, 8).add(Aspect.FLIGHT, 6).add(Aspect.MAGIC, 4)
                        .add(Aspect.DEATH, 2));
        written += add(
                "MF_Minotaur",
                new AspectList().add(Aspect.BEAST, 6).add(Aspect.MAN, 3).add(Aspect.EARTH, 2).add(Aspect.WEAPON, 2));
        written += add("MF_Hound", new AspectList().add(Aspect.BEAST, 4).add(Aspect.MOTION, 2).add(Aspect.SENSES, 2));
        // Cogwork armour is a machine a player climbs into, not an animal: metal and mechanism, no beast.
        written += add(
                "MF_CogSuit",
                new AspectList().add(Aspect.METAL, 8).add(Aspect.MECHANISM, 6).add(Aspect.ARMOR, 4)
                        .add(Aspect.MOTION, 2));
        MFLogUtil.log("Thaumcraft aspects registered for " + written + " MF mobs");
    }

    private static int add(String name, AspectList aspects) {
        String registryName = MineFantasyII.MODID + "." + name;
        // Same rule as for items: a description someone else already wrote is left alone.
        for (EntityTags existing : ThaumcraftApi.scanEntities) {
            if (existing != null && registryName.equals(existing.entityName)) {
                MFLogUtil.logDebug("Aspects for entity " + registryName + " already registered, leaving them alone");
                return 0;
            }
        }
        ThaumcraftApi.registerEntityTag(registryName, aspects);
        return 1;
    }
}
