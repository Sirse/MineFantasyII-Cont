package minefantasy.mf2.config;

import minefantasy.mf2.api.heating.Heatable;
import minefantasy.mf2.api.knowledge.InformationBase;
import minefantasy.mf2.api.knowledge.ResearchLogic;
import minefantasy.mf2.block.tileentity.TileEntityRoast;
import minefantasy.mf2.hunger.HungerSystemMF;
import minefantasy.mf2.mechanics.CombatMechanics;

public class ConfigHardcore extends ConfigurationBaseMF {

    public static final String CATEGORY_CRAFTING = "1: HARDCORE CRAFTING";
    public static final String CATEGORY_RESEARCH = "2: Research";
    public static final String CATEGORY_FOOD = "3: Food and Hunting";
    public static final String CATEGORY_MOB = "4: Monster Upgrades";
    public static final String CATEGORY_USER = "5: Player Debuffs";
    public static boolean HCCreduceIngots = true;
    public static boolean HCChotBurn = true;
    public static boolean HCCWeakItems = true;
    public static boolean HCCallowRocks = true;
    public static boolean HCCRemoveCraft = true;
    public static boolean HCCRemoveBooksCraft = false;
    public static boolean HCCRemoveTalismansCraft = false;
    public static boolean hunterKnife;
    public static boolean lessHunt;
    public static boolean preventCook;
    public static boolean upgradeZombieWep;
    public static float zombieWepChance;
    public static boolean spiderRiders;
    public static boolean fastZombies;
    public static boolean critLimp;

    @Override
    protected void loadConfig() {
        HCCreduceIngots = config.get(
                CATEGORY_CRAFTING,
                "Hardcore Ingots",
                true,
                "Some Metals (Like iron, steel and direct ore smelts) Must be worked manually on an anvil rather than smelted. They may also cost more! Big furnace still works.")
                .getBoolean();
        HCChotBurn = config.get(
                CATEGORY_CRAFTING,
                "Hot burns",
                true,
                "You cannot hold hot items (apron or not), tongs must be used.").getBoolean();
        Heatable.HCCquenchRuin = config
                .get(CATEGORY_CRAFTING, "Hardcore Quench", true, "Hot items can be damaged if a trough is not used.")
                .getBoolean();
        HCCWeakItems = config.get(
                CATEGORY_CRAFTING,
                "Weaken Basic items",
                true,
                "This will significantly reduce the durability of basic items (made on basic crafting table), they can still be crafted but are practically useless.")
                .getBoolean();
        HCCallowRocks = config.get(
                CATEGORY_CRAFTING,
                "Allow Stone-Age",
                true,
                "Allows punching stone for sharp rocks, and using them on leaves for sticks/vines: These make primitive stone tools")
                .getBoolean();
        HCCRemoveCraft = config.get(
                CATEGORY_CRAFTING,
                "Remove Recipes",
                true,
                "Some recipes (Such as Bread, or Flint and Steel) will be removed, since MF has its own recipe for such items.")
                .getBoolean();
        HCCRemoveBooksCraft = config.get(
                CATEGORY_CRAFTING,
                "Remove Books Recipes",
                false,
                "Skill books recipes will be disabled, but you still can find them it the world.").getBoolean();
        HCCRemoveTalismansCraft = config
                .get(
                        CATEGORY_CRAFTING,
                        "Remove Talismans Recipes",
                        false,
                        "Research talismans recipes will be disabled, but you still can find them it the world.")
                .getBoolean();

        ResearchLogic.knowledgelyr = config.get(
                CATEGORY_RESEARCH,
                "###CHANGE RESEARCH ID###",
                0,
                "This changes the research ID, removing all entries").getInt();
        InformationBase.unlockAll = config.get(
                CATEGORY_RESEARCH,
                "Unlock entries",
                false,
                "If you don't want to research, this will unlock all entries.").getBoolean();
        InformationBase.easyResearch = config.get(
                CATEGORY_RESEARCH,
                "Baby-Mode Research",
                false,
                "This removes the process of examining artefacts, research is unlocked by clicking entries in the book.")
                .getBoolean();

        hunterKnife = config.get(
                CATEGORY_FOOD,
                "Restrict to hunting weapon",
                false,
                "This option means animals ONLY drop meat and hide when killed with a hunting weapon such as a knife, only the killing blow counts")
                .getBoolean();
        lessHunt = config
                .get(
                        CATEGORY_FOOD,
                        "Reduce Meat Drops",
                        false,
                        "This will alter the stack size of animal meat drops, meaning they only drop 1 every time")
                .getBoolean();
        preventCook = config.get(
                CATEGORY_FOOD,
                "Prevent furnace food",
                false,
                "Stop food and ceramic from being cooked in a furnace").getBoolean();
        HungerSystemMF.slowdownRate = config.get(
                CATEGORY_FOOD,
                "Hunger slow rate",
                3,
                "how many added points per haunch is slows by. Default is 3: meaning it's takes 3 additional haunches to remove 1, meaning each haunch takes 4 times to be removed")
                .getInt();
        TileEntityRoast.enableOverheat = config.get(
                CATEGORY_FOOD,
                "Burn at high temperature",
                true,
                "Cooking food on a stove or oven will automatically burn at high temperatures").getBoolean();

        upgradeZombieWep = config.get(
                CATEGORY_MOB,
                "Give Zombie Weapon",
                true,
                "Zombies have a chance of spawning with forged weapons. It also affects MF-armoured zombie variants.")
                .getBoolean();
        zombieWepChance = (float) config.get(
                CATEGORY_MOB,
                "Zombie Weapon Spawn Chance Modifier",
                1.0F,
                "Multiplier for zombie forged-weapon chance and MF-armoured zombie variants. Difficulty still modifies the final chance.")
                .getDouble();
        spiderRiders = config
                .get(CATEGORY_MOB, "Enable Spider Riders", true, "Allow witches/creepers to spawn riding spiders")
                .getBoolean();
        fastZombies = config
                .get(
                        CATEGORY_MOB,
                        "Speed up zombies",
                        true,
                        "Speed up zombies (Sure it's not as real.. but it makes them a bit more dangerous)")
                .getBoolean();
        critLimp = config.get(
                CATEGORY_MOB,
                "Critical Injury Limp",
                true,
                "This means when you're badly wounded, you slow down and limp").getBoolean();
        CombatMechanics.swordSkeleton = config.get(CATEGORY_MOB, "Skeleton Swords", true, "Some Skeletons use swords")
                .getBoolean();
    }
}
