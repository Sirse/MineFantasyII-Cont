package minefantasy.mf2.config;

import minefantasy.mf2.api.knowledge.InformationBase;
import minefantasy.mf2.api.knowledge.ResearchLogic;
import minefantasy.mf2.api.rpg.RPGElements;
import minefantasy.mf2.block.tileentity.TileEntityResearch;
import minefantasy.mf2.block.tileentity.TileEntityRoast;
import minefantasy.mf2.hunger.HungerSystemMF;
import minefantasy.mf2.mechanics.MonsterUpgrader;

public class ConfigHardcore extends ConfigurationBaseMF {

    public static final String CATEGORY_CRAFTING = "1: HARDCORE CRAFTING";
    public static final String CATEGORY_RESEARCH = "2: Research";
    public static final String CATEGORY_FOOD = "3: Food and Hunting";
    public static final String CATEGORY_MOB = "4: Monster Upgrades";
    public static final String CATEGORY_USER = "5: Player Debuffs";
    public static final String CATEGORY_SKILLS = "6: Skills";
    public static boolean HCCreduceIngots = true;
    public static boolean HCCWeakItems = true;
    public static boolean HCCallowRocks = true;
    public static boolean HCCRemoveCraft = true;
    public static boolean HCCRemoveBooksCraft = false;
    public static boolean giveStarterBook = true;
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
        RPGElements.levelSpeedModifier = config.getFloat(
                "XP Gain Modifier",
                CATEGORY_SKILLS,
                1F,
                0F,
                100F,
                "Multiplies the skill xp every action gives");
        RPGElements.levelUpModifier = config.getFloat(
                "Level Cost Rise",
                CATEGORY_SKILLS,
                1.5F,
                0F,
                10F,
                "How steeply each skill level costs more xp than the last: 0 makes every level cost the same");
        HCCreduceIngots = config.get(
                CATEGORY_CRAFTING,
                "Hardcore Ingots",
                true,
                "Some Metals (Like iron, steel and direct ore smelts) Must be worked manually on an anvil rather than smelted. They may also cost more! Big furnace still works.")
                .getBoolean();

        // Moved to Crafting
        for (String moved : new String[] { "Hot burns", "Hardcore Quench" }) {
            dropKey(CATEGORY_CRAFTING, moved);
        }
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
                "Skill books recipes will be disabled, but you still can find them in the world.").getBoolean();
        HCCRemoveTalismansCraft = config
                .get(
                        CATEGORY_CRAFTING,
                        "Remove Talismans Recipes",
                        false,
                        "Research talismans recipes will be disabled, but you still can find them in the world.")
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
        giveStarterBook = config.get(
                CATEGORY_RESEARCH,
                "Give Starter Book",
                true,
                "Give players the research book when they first join the world.").getBoolean();
        TileEntityResearch.SHELF_REACH = config.getInt(
                "Bookshelf Reach",
                CATEGORY_RESEARCH,
                5,
                0,
                16,
                "How far across bookshelves around a research table count");
        TileEntityResearch.SHELF_HEIGHT = config.getInt(
                "Bookshelf Height",
                CATEGORY_RESEARCH,
                3,
                0,
                16,
                "How far up and down bookshelves around a research table count");
        TileEntityResearch.SHELF_MOST = config.getFloat(
                "Bookshelf Most Boost",
                CATEGORY_RESEARCH,
                1.5F,
                0F,
                100F,
                "The most bookshelves add to research speed: 1.5 means up to 2.5 times as fast");
        TileEntityResearch.SHELF_SCALE = config.getFloat(
                "Bookshelf Scale",
                CATEGORY_RESEARCH,
                10F,
                0.1F,
                1000F,
                "Bookshelves that bring about two thirds of the most boost");
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
        HungerSystemMF.slowdownRate = config.getInt(
                "Hunger slow rate",
                CATEGORY_FOOD,
                3,
                0,
                100,
                "how many added points per haunch it slows by. Default is 3: meaning it takes 3 additional haunches to remove 1, meaning each haunch takes 4 times to be removed");
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
        zombieWepChance = config.getFloat(
                "Zombie Weapon Spawn Chance Modifier",
                CATEGORY_MOB,
                1.0F,
                0F,
                100F,
                "Multiplier for zombie forged-weapon chance and MF-armoured zombie variants. Difficulty still modifies the final chance.");
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
        MonsterUpgrader.swordSkeleton = config.get(CATEGORY_MOB, "Skeleton Swords", true, "Some Skeletons use swords")
                .getBoolean();
    }
}
