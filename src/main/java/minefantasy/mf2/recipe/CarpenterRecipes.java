package minefantasy.mf2.recipe;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import cpw.mods.fml.common.registry.GameRegistry;
import minefantasy.mf2.api.MineFantasyAPI;
import minefantasy.mf2.api.crafting.NativeRecipes;
import minefantasy.mf2.api.crafting.Salvage;
import minefantasy.mf2.api.crafting.refine.PaintOilRecipe;
import minefantasy.mf2.api.material.CustomMaterial;
import minefantasy.mf2.api.rpg.Skill;
import minefantasy.mf2.api.rpg.SkillList;
import minefantasy.mf2.block.decor.BlockWoodDecor;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.config.ConfigHardcore;
import minefantasy.mf2.item.food.FoodListMF;
import minefantasy.mf2.item.list.ArmourListMF;
import minefantasy.mf2.item.list.ComponentListMF;
import minefantasy.mf2.item.list.CustomToolListMF;
import minefantasy.mf2.item.list.ToolListMF;
import minefantasy.mf2.knowledge.KnowledgeListMF;

public class CarpenterRecipes {

    public static final String basic = "step.wood";
    public static final String chopping = "dig.wood";
    public static final String primitive = "minefantasy2:block.craftprimitive";
    public static final String sewing = "step.cloth";
    public static final String stonemason = "minefantasy2:block.hammercarpenter";
    public static final String snipping = "mob.sheep.shear";
    public static final String sawing = "minefantasy2:block.sawcarpenter";
    public static final String grinding = "dig.gravel";
    public static final String nailHammer = "minefantasy2:block.hammercarpenter";
    public static final String woodHammer = "minefantasy2:block.carpentermallet";
    public static final String mixing = "step.wood";
    public static final String spanner = "minefantasy2:block.twistbolt";

    private static final Skill artisanry = SkillList.artisanry;
    private static final Skill engineering = SkillList.engineering;
    private static final Skill construction = SkillList.construction;
    private static final Skill provisioning = SkillList.provisioning;

    public static void init() {
        /*
         * ArrayList<CustomMaterial> wood = CustomMaterial.getList("wood"); Iterator iteratorWood = wood.iterator();
         * while(iteratorWood.hasNext()) { CustomMaterial customMat = (CustomMaterial) iteratorWood.next(); }
         */
        assembleWoodBasic();
        CustomWoodRecipes.init();
        addDusts();
        addWoodworks();
        addStonemason();
        addCooking();
        addMisc();
        addEngineering();
        if (ConfigHardcore.HCCallowRocks) {
            addPrimitive();
        } else {
            addNonPrimitiveStone();
        }
        MineFantasyAPI.carpenterRecipe(new ItemStack(ToolListMF.researchBook)).tool("hands", -1).time(1).sound(sewing)
                .shaped(new Object[] { "B", 'B', Items.book, });

        if (ConfigHardcore.HCCallowRocks) {
            try (NativeRecipes.Variant v = NativeRecipes.variant("from_cobblestone")) {
                KnowledgeListMF.sharpRocksR = MineFantasyAPI
                        .carpenterRecipe(new ItemStack(ComponentListMF.sharp_rock, 8)).tool("hammer", -1).time(10)
                        .sound(stonemason).shaped(new Object[] { "S", 'S', Blocks.cobblestone, });
            }

        } else {

        }
        Salvage.addSalvage(ToolListMF.dryrocks, Blocks.cobblestone);

        KnowledgeListMF.threadR1 = MineFantasyAPI.carpenterRecipe(new ItemStack(ComponentListMF.thread, 4))
                .research("commodities").tool("hands", -1).time(5).sound(sewing)
                .shaped(new Object[] { "W", "S", 'W', Blocks.wool, 'S', Items.stick, });
        try (NativeRecipes.Variant v = NativeRecipes.variant("from_vine")) {
            KnowledgeListMF.threadR2 = MineFantasyAPI.carpenterRecipe(new ItemStack(ComponentListMF.thread))
                    .research("commodities").tool("hands", -1).time(5).sound(sewing)
                    .shaped(new Object[] { " V ", "VSV", " V ", 'S', Items.stick, 'V', ComponentListMF.vine });
        }
        KnowledgeListMF.stringR = MineFantasyAPI.carpenterRecipe(new ItemStack(Items.string)).research("commodities")
                .tool("hands", -1).time(10).sound(sewing)
                .shaped(new Object[] { "T", "T", "T", "T", 'T', ComponentListMF.thread });

        KnowledgeListMF.lStripsR = MineFantasyAPI.carpenterRecipe(new ItemStack(ComponentListMF.leather_strip, 4))
                .research("commodities").tool("shears", -1).time(10).sound(snipping)
                .shaped(new Object[] { "L", 'L', Items.leather, });

        MineFantasyAPI.carpenterRecipe(new ItemStack(ToolListMF.swordTraining)).skill(artisanry).tool("knife", 1)
                .time(40).sound(nailHammer).shaped(
                        new Object[] { "NI  ", "SIII", "NI  ", 'N', ComponentListMF.nail, 'S', ComponentListMF.plank,
                                'I', Blocks.planks, });

        MineFantasyAPI.carpenterRecipe(new ItemStack(ToolListMF.waraxeTraining)).skill(artisanry).tool("knife", 1)
                .time(30).sound(nailHammer).shaped(
                        new Object[] { " II ", "SSIN", "  I ", 'N', ComponentListMF.nail, 'S', ComponentListMF.plank,
                                'I', Blocks.planks, });
        MineFantasyAPI.carpenterRecipe(new ItemStack(ToolListMF.maceTraining)).skill(artisanry).tool("knife", 1)
                .time(35).sound(nailHammer).shaped(
                        new Object[] { "  II", "SSII", "  N ", 'N', ComponentListMF.nail, 'S', ComponentListMF.plank,
                                'I', Blocks.planks, });
        MineFantasyAPI.carpenterRecipe(new ItemStack(ToolListMF.spearTraining)).skill(artisanry).tool("knife", 1)
                .time(20).sound(nailHammer).shaped(
                        new Object[] { "  N ", "SSSI", "  N ", 'N', ComponentListMF.nail, 'S', ComponentListMF.plank,
                                'I', Blocks.planks, });
        ItemStack scrapWood = ComponentListMF.plank.construct("ScrapWood");
        Salvage.addSalvage(
                ToolListMF.swordTraining,
                new ItemStack(Blocks.planks, 5),
                new ItemStack(ComponentListMF.nail, 2),
                scrapWood);
        Salvage.addSalvage(
                ToolListMF.waraxeTraining,
                new ItemStack(Blocks.planks, 4),
                ComponentListMF.nail,
                scrapWood,
                scrapWood);
        Salvage.addSalvage(
                ToolListMF.maceTraining,
                new ItemStack(Blocks.planks, 4),
                ComponentListMF.nail,
                scrapWood,
                scrapWood);
        Salvage.addSalvage(
                ToolListMF.spearTraining,
                Blocks.planks,
                new ItemStack(ComponentListMF.nail, 2),
                scrapWood,
                scrapWood,
                scrapWood);

        KnowledgeListMF.badBandageR = MineFantasyAPI.carpenterRecipe(new ItemStack(ToolListMF.bandage_crude, 2))
                .skill(provisioning).research("bandage").tool("needle", -1).time(10).sound(sewing)
                .shaped(new Object[] { "LLL", 'L', ComponentListMF.rawhideSmall, });

        try (NativeRecipes.Variant v = NativeRecipes.variant("rawhide_medium")) {
            MineFantasyAPI.carpenterRecipe(new ItemStack(ToolListMF.bandage_crude, 4)).skill(provisioning)
                    .research("bandage").tool("needle", -1).time(20).sound(sewing)
                    .shaped(new Object[] { "LLL", 'L', ComponentListMF.rawhideMedium, });
        }
        try (NativeRecipes.Variant v = NativeRecipes.variant("rawhide_large")) {
            MineFantasyAPI.carpenterRecipe(new ItemStack(ToolListMF.bandage_crude, 6)).skill(provisioning)
                    .research("bandage").tool("needle", -1).time(30).sound(sewing)
                    .shaped(new Object[] { "LLL", 'L', ComponentListMF.rawhideLarge, });
        }
        KnowledgeListMF.bandageR = MineFantasyAPI.carpenterRecipe(new ItemStack(ToolListMF.bandage_wool, 4))
                .skill(provisioning).research("bandage").tool("needle", 1).time(10).sound(sewing)
                .shaped(new Object[] { "CTC", 'T', ComponentListMF.thread, 'C', Blocks.wool, });

        KnowledgeListMF.goodBandageR = MineFantasyAPI.carpenterRecipe(new ItemStack(ToolListMF.bandage_tough))
                .skill(provisioning).research("bandageadv").tool("needle", 2).time(20).sound(sewing).shaped(
                        new Object[] { "T", "L", "B", 'T', ComponentListMF.thread, 'L', ComponentListMF.leather_strip,
                                'B', ToolListMF.bandage_wool });

        KnowledgeListMF.roughHelmetR = MineFantasyAPI.carpenterRecipe(ArmourListMF.armour(ArmourListMF.leather, 1, 0))
                .skill(artisanry).research("craftArmourBasic").tool("needle", -1).time(25).sound(sewing).shaped(
                        new Object[] { "TLT", "S S", 'T', ComponentListMF.thread, 'S', ComponentListMF.leather_strip,
                                'L', Items.leather });
        KnowledgeListMF.roughChestR = MineFantasyAPI.carpenterRecipe(ArmourListMF.armour(ArmourListMF.leather, 1, 1))
                .skill(artisanry).research("craftArmourBasic").tool("needle", -1).time(40).sound(sewing).shaped(
                        new Object[] { "S S", "LLL", "TLT", 'T', ComponentListMF.thread, 'S',
                                ComponentListMF.leather_strip, 'L', Items.leather });
        KnowledgeListMF.roughLegsR = MineFantasyAPI.carpenterRecipe(ArmourListMF.armour(ArmourListMF.leather, 1, 2))
                .skill(artisanry).research("craftArmourBasic").tool("needle", -1).time(35).sound(sewing).shaped(
                        new Object[] { "TLT", "L L", "S S", 'T', ComponentListMF.thread, 'S',
                                ComponentListMF.leather_strip, 'L', Items.leather });
        KnowledgeListMF.roughBootsR = MineFantasyAPI.carpenterRecipe(ArmourListMF.armour(ArmourListMF.leather, 1, 3))
                .skill(artisanry).research("craftArmourBasic").tool("needle", -1).time(20).sound(sewing).shaped(
                        new Object[] { "T T", "S S", 'T', ComponentListMF.thread, 'S',
                                ComponentListMF.leather_strip, });
        Salvage.addSalvage(
                ArmourListMF.armourItem(ArmourListMF.leather, 1, 0),
                new ItemStack(ComponentListMF.thread, 2),
                new ItemStack(ComponentListMF.leather_strip, 2),
                Items.leather);
        Salvage.addSalvage(
                ArmourListMF.armourItem(ArmourListMF.leather, 1, 1),
                new ItemStack(ComponentListMF.thread, 4),
                new ItemStack(ComponentListMF.leather_strip, 2),
                new ItemStack(Items.leather, 4));
        Salvage.addSalvage(
                ArmourListMF.armourItem(ArmourListMF.leather, 1, 2),
                new ItemStack(ComponentListMF.thread, 4),
                new ItemStack(ComponentListMF.leather_strip, 2),
                new ItemStack(Items.leather, 3));
        Salvage.addSalvage(
                ArmourListMF.armourItem(ArmourListMF.leather, 1, 3),
                new ItemStack(ComponentListMF.thread, 4),
                new ItemStack(ComponentListMF.leather_strip, 2));

        KnowledgeListMF.reHelmetR = MineFantasyAPI.carpenterRecipe(ArmourListMF.armour(ArmourListMF.leather, 2, 0))
                .skill(artisanry).research("craftArmourLight").tool("needle", 1).time(50).sound(sewing).shaped(
                        new Object[] { "TTT", "UPU", 'T', ComponentListMF.thread, 'P',
                                ArmourListMF.armour(ArmourListMF.leather, 1, 0), 'U', Items.leather });
        KnowledgeListMF.reChestR = MineFantasyAPI.carpenterRecipe(ArmourListMF.armour(ArmourListMF.leather, 2, 1))
                .skill(artisanry).research("craftArmourLight").tool("needle", 1).time(80).sound(sewing).shaped(
                        new Object[] { "TTT", "UPU", 'T', ComponentListMF.thread, 'P',
                                ArmourListMF.armour(ArmourListMF.leather, 1, 1), 'U', Items.leather });
        KnowledgeListMF.reLegsR = MineFantasyAPI.carpenterRecipe(ArmourListMF.armour(ArmourListMF.leather, 2, 2))
                .skill(artisanry).research("craftArmourLight").tool("needle", 1).time(70).sound(sewing).shaped(
                        new Object[] { "TTT", "UPU", 'T', ComponentListMF.thread, 'P',
                                ArmourListMF.armour(ArmourListMF.leather, 1, 2), 'U', Items.leather });
        KnowledgeListMF.reBootsR = MineFantasyAPI.carpenterRecipe(ArmourListMF.armour(ArmourListMF.leather, 2, 3))
                .skill(artisanry).research("craftArmourLight").tool("needle", 1).time(40).sound(sewing).shaped(
                        new Object[] { "TTT", "UPU", 'T', ComponentListMF.thread, 'P',
                                ArmourListMF.armour(ArmourListMF.leather, 1, 3), 'U', Items.leather });
        Salvage.addSalvage(
                ArmourListMF.armourItem(ArmourListMF.leather, 2, 0),
                ArmourListMF.armourItem(ArmourListMF.leather, 1, 0),
                new ItemStack(ComponentListMF.thread, 3),
                new ItemStack(Items.leather, 2));
        Salvage.addSalvage(
                ArmourListMF.armourItem(ArmourListMF.leather, 2, 1),
                ArmourListMF.armourItem(ArmourListMF.leather, 1, 1),
                new ItemStack(ComponentListMF.thread, 3),
                new ItemStack(Items.leather, 2));
        Salvage.addSalvage(
                ArmourListMF.armourItem(ArmourListMF.leather, 2, 2),
                ArmourListMF.armourItem(ArmourListMF.leather, 1, 2),
                new ItemStack(ComponentListMF.thread, 3),
                new ItemStack(Items.leather, 2));
        Salvage.addSalvage(
                ArmourListMF.armourItem(ArmourListMF.leather, 2, 3),
                ArmourListMF.armourItem(ArmourListMF.leather, 1, 3),
                new ItemStack(ComponentListMF.thread, 3),
                new ItemStack(Items.leather, 2));

        // PADDING
        KnowledgeListMF.padding[0] = MineFantasyAPI.carpenterRecipe(ArmourListMF.armour(ArmourListMF.leather, 4, 0))
                .skill(artisanry).research("craftArmourLight").tool("needle", 1).time(50).sound(sewing).shaped(
                        new Object[] { " W ", "SPS", " S ", 'P', ArmourListMF.armour(ArmourListMF.leather, 1, 0), 'W',
                                Blocks.wool, 'S', ComponentListMF.thread, });
        KnowledgeListMF.padding[1] = MineFantasyAPI.carpenterRecipe(ArmourListMF.armour(ArmourListMF.leather, 4, 1))
                .skill(artisanry).research("craftArmourLight").tool("needle", 1).time(80).sound(sewing).shaped(
                        new Object[] { " W ", "SPS", " S ", 'P', ArmourListMF.armour(ArmourListMF.leather, 1, 1), 'W',
                                Blocks.wool, 'S', ComponentListMF.thread, });
        KnowledgeListMF.padding[2] = MineFantasyAPI.carpenterRecipe(ArmourListMF.armour(ArmourListMF.leather, 4, 2))
                .skill(artisanry).research("craftArmourLight").tool("needle", 1).time(70).sound(sewing).shaped(
                        new Object[] { " W ", "SPS", " S ", 'P', ArmourListMF.armour(ArmourListMF.leather, 1, 2), 'W',
                                Blocks.wool, 'S', ComponentListMF.thread, });
        KnowledgeListMF.padding[3] = MineFantasyAPI.carpenterRecipe(ArmourListMF.armour(ArmourListMF.leather, 4, 3))
                .skill(artisanry).research("craftArmourLight").tool("needle", 1).time(40).sound(sewing).shaped(
                        new Object[] { " W ", "SPS", " S ", 'P', ArmourListMF.armour(ArmourListMF.leather, 1, 3), 'W',
                                Blocks.wool, 'S', ComponentListMF.thread, });
        Salvage.addSalvage(
                ArmourListMF.armourItem(ArmourListMF.leather, 4, 0),
                ArmourListMF.armourItem(ArmourListMF.leather, 1, 0),
                new ItemStack(ComponentListMF.thread, 3),
                Blocks.wool);
        Salvage.addSalvage(
                ArmourListMF.armourItem(ArmourListMF.leather, 4, 1),
                ArmourListMF.armourItem(ArmourListMF.leather, 1, 1),
                new ItemStack(ComponentListMF.thread, 3),
                Blocks.wool);
        Salvage.addSalvage(
                ArmourListMF.armourItem(ArmourListMF.leather, 4, 2),
                ArmourListMF.armourItem(ArmourListMF.leather, 1, 2),
                new ItemStack(ComponentListMF.thread, 3),
                Blocks.wool);
        Salvage.addSalvage(
                ArmourListMF.armourItem(ArmourListMF.leather, 4, 3),
                ArmourListMF.armourItem(ArmourListMF.leather, 1, 3),
                new ItemStack(ComponentListMF.thread, 3),
                Blocks.wool);

        KnowledgeListMF.repairBasicR = MineFantasyAPI.carpenterRecipe(new ItemStack(BlockListMF.repair_basic))
                .skill(artisanry).research("repair_basic").tool("needle", 1).time(20).sound(sewing).shaped(
                        new Object[] { "TTT", "FNH", "SLS", 'T', ComponentListMF.thread, 'S',
                                ComponentListMF.leather_strip, 'L', Items.leather, 'F', Items.flint, 'H',
                                CustomToolListMF.standard_hammer, 'N', ComponentListMF.nail, });
        ItemStack bronzePlate = ComponentListMF.plate.createComm("bronze");
        KnowledgeListMF.repairAdvancedR = MineFantasyAPI.carpenterRecipe(new ItemStack(BlockListMF.repair_advanced))
                .skill(artisanry).research("repair_advanced").tool("needle", 2).time(50).sound(sewing).shaped(
                        new Object[] { "SCS", "PKH", "CSC", 'K', BlockListMF.repair_basic, 'P', bronzePlate, 'H',
                                CustomToolListMF.standard_hammer, 'C', Items.slime_ball, 'S', Items.string, });
        KnowledgeListMF.repairOrnateR = MineFantasyAPI.carpenterRecipe(new ItemStack(BlockListMF.repair_ornate))
                .skill(artisanry).research("repair_ornate").tool("needle", 3).time(100).sound(sewing).shaped(
                        new Object[] { "GDG", "LKL", "GLG", 'K', BlockListMF.repair_advanced, 'G', Items.gold_ingot,
                                'L', new ItemStack(Items.dye, 1, 4), 'D', Items.diamond, });

        Salvage.addSalvage(
                BlockListMF.repair_basic,
                new ItemStack(ComponentListMF.thread, 3),
                ComponentListMF.nail,
                Items.flint,
                Items.leather,
                new ItemStack(ComponentListMF.leather_strip, 2));
        Salvage.addSalvage(
                BlockListMF.repair_advanced,
                BlockListMF.repair_basic,
                bronzePlate,
                new ItemStack(Items.slime_ball, 3),
                new ItemStack(Items.string, 3));
        Salvage.addSalvage(
                BlockListMF.repair_ornate,
                BlockListMF.repair_advanced,
                new ItemStack(Items.gold_ingot, 4),
                Items.diamond,
                new ItemStack(Items.dye, 3, 4));

        KnowledgeListMF.trilogyRecipe = MineFantasyAPI.carpenterRecipe(new ItemStack(ComponentListMF.artefacts, 1, 3))
                .skill(artisanry).research("smeltMaster").tool("hands", -1).time(1).sound(basic).shapeless(
                        new Object[] { new ItemStack(ComponentListMF.artefacts, 1, 0),
                                new ItemStack(ComponentListMF.artefacts, 1, 1),
                                new ItemStack(ComponentListMF.artefacts, 1, 2) });
    }

    public static void assembleWoodBasic() {
        KnowledgeListMF.carpenterRecipe = GameRegistry.addShapedRecipe(
                new ItemStack(BlockListMF.carpenter),
                new Object[] { "PBP", "P P", 'B', Blocks.crafting_table, 'P', ComponentListMF.plank });
        Salvage.addSalvage(
                BlockListMF.carpenter,
                ComponentListMF.plank.construct("ScrapWood", 4),
                Blocks.crafting_table);

        KnowledgeListMF.nailPlanksR = MineFantasyAPI.carpenterRecipe(new ItemStack(BlockListMF.nailed_planks))
                .skill(construction).research("refined_planks").tool("hammer", 1).time(5).sound(nailHammer).shaped(
                        new Object[] { "N ", "PP", "PP", 'N', ComponentListMF.nail, 'P',
                                ComponentListMF.plank.construct("OakWood"), });
        KnowledgeListMF.nailStairR = MineFantasyAPI.carpenterRecipe(new ItemStack(BlockListMF.nailed_planks_stair))
                .skill(construction).research("refined_planks").tool("hammer", 1).time(5).sound(nailHammer).shaped(
                        new Object[] { "N ", "P ", "PP", 'N', ComponentListMF.nail, 'P',
                                ComponentListMF.plank.construct("OakWood"), });
        KnowledgeListMF.tannerRecipe = MineFantasyAPI.carpenterRecipe(new ItemStack(BlockListMF.tanner))
                .skill(construction).tool("hammer", -1).time(10).sound(nailHammer)
                .shaped(new Object[] { "PPP", "P P", "PPP", 'P', ComponentListMF.plank, });

        KnowledgeListMF.clayWallR = MineFantasyAPI.carpenterRecipe(new ItemStack(BlockListMF.clayWall, 4))
                .skill(construction).research("clay_wall").tool("hammer", 1).time(2).sound(nailHammer).shaped(
                        new Object[] { "NPN", "PCP", "NPN", 'N', ComponentListMF.nail, 'P', ComponentListMF.plank, 'C',
                                Blocks.clay });

        KnowledgeListMF.researchTableRecipe = MineFantasyAPI.carpenterRecipe(new ItemStack(BlockListMF.research))
                .skill(construction).tool("hammer", -1).time(10).sound(nailHammer)
                .shaped(new Object[] { "B", "C", 'B', ToolListMF.researchBook, 'C', BlockListMF.carpenter, });
        KnowledgeListMF.bSalvageR = MineFantasyAPI.carpenterRecipe(new ItemStack(BlockListMF.salvage_basic))
                .skill(construction).tool("hammer", -1).time(10).sound(nailHammer).shaped(
                        new Object[] { "SFS", "PWP", 'W', Blocks.crafting_table, 'S', Blocks.stone, 'F', Items.flint,
                                'P', ComponentListMF.plank });

        KnowledgeListMF.framedGlassR = MineFantasyAPI.carpenterRecipe(new ItemStack(BlockListMF.framed_glass))
                .skill(construction).tool("hammer", -1).time(10).sound(nailHammer)
                .shaped(new Object[] { "PGP", 'P', ComponentListMF.plank, 'G', Blocks.glass });
        KnowledgeListMF.windowR = MineFantasyAPI.carpenterRecipe(new ItemStack(BlockListMF.window)).skill(construction)
                .tool("hammer", -1).time(10).sound(nailHammer)
                .shaped(new Object[] { " P ", "PGP", " P ", 'P', ComponentListMF.plank, 'G', Blocks.glass });

        Salvage.addSalvage(BlockListMF.framed_glass, ComponentListMF.plank.construct("ScrapWood", 2), Blocks.glass);
        Salvage.addSalvage(BlockListMF.window, ComponentListMF.plank.construct("ScrapWood", 4), Blocks.glass);
        Salvage.addSalvage(
                BlockListMF.clayWall,
                ComponentListMF.nail,
                ComponentListMF.plank.construct("ScrapWood"),
                Items.clay_ball);
        Salvage.addSalvage(BlockListMF.tanner, ComponentListMF.plank.construct("ScrapWood", 8));
        Salvage.addSalvage(BlockListMF.research, BlockListMF.carpenter);
        Salvage.addSalvage(
                BlockListMF.salvage_basic,
                Items.flint,
                new ItemStack(Blocks.stone, 2),
                ComponentListMF.plank.construct("ScrapWood", 2),
                Blocks.crafting_table);
    }

    private static void addDusts() {
        MineFantasyAPI.quernRecipe(new ItemStack(Items.dye, 1, 3), new ItemStack(FoodListMF.coca_powder)).register();// ItemDye
        MineFantasyAPI.quernRecipe(Items.wheat, new ItemStack(FoodListMF.flour)).register();
        MineFantasyAPI.quernRecipe(Items.reeds, new ItemStack(FoodListMF.sugarpot)).register();
        MineFantasyAPI.quernRecipe(FoodListMF.breadroll, new ItemStack(FoodListMF.breadcrumbs)).register();

        MineFantasyAPI
                .quernRecipe(FoodListMF.generic_meat_uncooked, new ItemStack(FoodListMF.generic_meat_mince_uncooked))
                .register();
        MineFantasyAPI.quernRecipe(
                FoodListMF.generic_meat_strip_uncooked,
                new ItemStack(FoodListMF.generic_meat_mince_uncooked)).register();
        MineFantasyAPI.quernRecipe(
                FoodListMF.generic_meat_chunk_uncooked,
                new ItemStack(FoodListMF.generic_meat_mince_uncooked)).register();
        MineFantasyAPI.quernRecipe(FoodListMF.generic_meat_cooked, new ItemStack(FoodListMF.generic_meat_mince_cooked))
                .register();
        MineFantasyAPI
                .quernRecipe(FoodListMF.generic_meat_strip_cooked, new ItemStack(FoodListMF.generic_meat_mince_cooked))
                .register();
        MineFantasyAPI
                .quernRecipe(FoodListMF.generic_meat_chunk_cooked, new ItemStack(FoodListMF.generic_meat_mince_cooked))
                .register();

        MineFantasyAPI.quernRecipe(Items.coal, new ItemStack(ComponentListMF.coalDust)).register();
        MineFantasyAPI.quernRecipe(new ItemStack(Items.coal, 1, 1), new ItemStack(ComponentListMF.coalDust)).register();
        MineFantasyAPI.quernRecipe(ComponentListMF.kaolinite, new ItemStack(ComponentListMF.kaolinite_dust)).register();
        MineFantasyAPI.quernRecipe(Items.flint, new ItemStack(ComponentListMF.shrapnel)).register();

        MineFantasyAPI.quernRecipe(ComponentListMF.flux, new ItemStack(ComponentListMF.flux_pot)).register();

        KnowledgeListMF.pieTrayRecipe = MineFantasyAPI.carpenterRecipe(new ItemStack(ComponentListMF.pie_tray_uncooked))
                .tool("hands", -1).time(10).sound(basic).shaped(new Object[] { "CC", 'C', Items.clay_ball, });

        KnowledgeListMF.potRecipe = MineFantasyAPI.carpenterRecipe(new ItemStack(ComponentListMF.clay_pot_uncooked, 8))
                .tool("hands", -1).time(5).sound(basic).shaped(new Object[] { "C  C", " CC ", 'C', Items.clay_ball, });
        KnowledgeListMF.mouldRecipe = MineFantasyAPI
                .carpenterRecipe(new ItemStack(ComponentListMF.ingot_mould_uncooked)).research("crucible")
                .tool("hands", -1).time(10).sound(basic).shaped(new Object[] { "CCC", " C ", 'C', Items.clay_ball, });
        KnowledgeListMF.jugRecipe = MineFantasyAPI.carpenterRecipe(new ItemStack(FoodListMF.jug_uncooked, 4))
                .tool("hands", -1).time(8).sound(basic)
                .shaped(new Object[] { "C  ", "C C", " C ", 'C', Items.clay_ball, });
        KnowledgeListMF.blackpowderRec = MineFantasyAPI.carpenterRecipe(new ItemStack(ComponentListMF.blackpowder, 2))
                .research("blackpowder").tool("hands", -1).time(2).sound(basic).shaped(
                        new Object[] { "NS", "CC", "PP", 'C', ComponentListMF.coalDust, 'N', ComponentListMF.nitre, 'S',
                                ComponentListMF.sulfur, 'P', ComponentListMF.clay_pot, });
        KnowledgeListMF.crudeBombR = MineFantasyAPI.carpenterRecipe(new ItemStack(ToolListMF.bomb_crude))
                .skill(engineering).research("blackpowder").tool("hands", -1).time(5).sound(primitive).shaped(
                        new Object[] { "T", "B", "P",

                                'B', ComponentListMF.blackpowder, 'T', ComponentListMF.thread, 'P', Items.paper, });
        KnowledgeListMF.advblackpowderRec = MineFantasyAPI
                .carpenterRecipe(new ItemStack(ComponentListMF.blackpowder_advanced)).research("advblackpowder")
                .tool("hands", -1).time(10).sound(basic).shaped(
                        new Object[] { " B ", "RGR", " P ", 'B', ComponentListMF.blackpowder, 'G', Items.glowstone_dust,
                                'R', Items.redstone, 'P', ComponentListMF.clay_pot, });
        KnowledgeListMF.magmaRefinedR = MineFantasyAPI
                .carpenterRecipe(new ItemStack(ComponentListMF.magma_cream_refined)).research("firebomb")
                .tool("pestle", -1).time(10).sound(grinding).shaped(
                        new Object[] { "B", "H", "C", "P", 'H', ComponentListMF.dragon_heart, 'B', Items.blaze_powder,
                                'C', Items.magma_cream, 'P', ComponentListMF.clay_pot, });
        Salvage.addSalvage(
                ComponentListMF.magma_cream_refined,
                ComponentListMF.dragon_heart,
                Items.blaze_powder,
                Items.magma_cream,
                ComponentListMF.clay_pot);
    }

    private static void addWoodworks() {
        KnowledgeListMF.refinedPlankBlockR = MineFantasyAPI.carpenterRecipe(new ItemStack(BlockListMF.refined_planks))
                .skill(construction).research("refined_planks").tool("hammer", 1).time(10).sound(nailHammer).shaped(
                        new Object[] { "N ", "PP", "PP", 'N', ComponentListMF.nail, 'P',
                                ComponentListMF.plank.construct("RefinedWood"), });

        KnowledgeListMF.refinedStairR = MineFantasyAPI.carpenterRecipe(new ItemStack(BlockListMF.refined_planks_stair))
                .skill(construction).research("refined_planks").tool("hammer", 1).time(10).sound(nailHammer).shaped(
                        new Object[] { "N ", "P ", "PP", 'N', ComponentListMF.nail, 'P',
                                ComponentListMF.plank.construct("RefinedWood"), });
        Salvage.addSalvage(
                BlockListMF.nailed_planks,
                ComponentListMF.nail,
                ComponentListMF.plank.construct("ScrapWood", 4));
        Salvage.addSalvage(
                BlockListMF.refined_planks,
                ComponentListMF.nail,
                ComponentListMF.plank.construct("RefinedWood", 4));
        Salvage.addSalvage(
                BlockListMF.nailed_planks_stair,
                ComponentListMF.nail,
                ComponentListMF.plank.construct("ScrapWood", 3));
        Salvage.addSalvage(
                BlockListMF.refined_planks_stair,
                ComponentListMF.nail,
                ComponentListMF.plank.construct("RefinedWood", 3));

        KnowledgeListMF.bellowsRecipe = MineFantasyAPI.carpenterRecipe(new ItemStack(BlockListMF.bellows))
                .skill(construction).tool("hammer", 1).time(50).sound(nailHammer).shaped(
                        new Object[] { "NNN", "PPP", "LL ", "PP ", 'N', ComponentListMF.nail, 'P',
                                ComponentListMF.plank.construct("RefinedWood"), 'L', Items.leather, });
        Salvage.addSalvage(
                BlockListMF.bellows,
                new ItemStack(ComponentListMF.nail, 3),
                ComponentListMF.plank.construct("RefinedWood", 5),
                new ItemStack(Items.leather, 2));

        try (NativeRecipes.Variant v = NativeRecipes.variant("from_planks")) {
            KnowledgeListMF.woodTroughRecipe = MineFantasyAPI
                    .carpenterRecipe(((BlockWoodDecor) BlockListMF.trough_wood).construct("ScrapWood"))
                    .skill(construction).tool("hammer", -1).time(20).sound(nailHammer).shaped(
                            new Object[] { "P P", "PPP",

                                    'P', ComponentListMF.plank, });
        }

        KnowledgeListMF.strongRackR = MineFantasyAPI.carpenterRecipe(new ItemStack(BlockListMF.advTanner))
                .skill(construction).tool("hammer", 1).time(80).sound(nailHammer).shaped(
                        new Object[] { "NNN", "PPP", "P P", "PPP", 'N', ComponentListMF.nail, 'P',
                                ComponentListMF.plank.construct("RefinedWood"), });
        Salvage.addSalvage(
                BlockListMF.advTanner,
                ComponentListMF.plank.construct("RefinedWood", 8),
                new ItemStack(ComponentListMF.nail, 3));

        try (NativeRecipes.Variant v = NativeRecipes.variant("paint_brush")) {
            MineFantasyAPI.carpenterRecipe(new ItemStack(BlockListMF.refined_planks)).skill(construction)
                    .research("paint_brush").tool("brush", -1).time(3).sound(sewing)
                    .shaped(new Object[] { "O", "P", 'O', ComponentListMF.plant_oil, 'P', BlockListMF.nailed_planks, });
        }

        PaintOilRecipe.addRecipe(BlockListMF.nailed_planks, BlockListMF.refined_planks);
        PaintOilRecipe.addRecipe(BlockListMF.nailed_planks_stair, BlockListMF.refined_planks_stair);

    }

    private static void addStonemason() {
        KnowledgeListMF.quernR = MineFantasyAPI.carpenterRecipe(new ItemStack(BlockListMF.quern)).tool("hammer", -1)
                .time(10).sound(stonemason).shaped(new Object[] { "FSF", "SSS", 'F', Items.flint, 'S', Blocks.stone, });
        KnowledgeListMF.stoneovenRecipe = MineFantasyAPI.carpenterRecipe(new ItemStack(BlockListMF.oven_stone))
                .tool("hammer", -1).time(10).sound(stonemason)
                .shaped(new Object[] { "S", "C", 'C', BlockListMF.roast, 'S', Blocks.stone, });

        KnowledgeListMF.kitchenBenchRecipe = MineFantasyAPI.carpenterRecipe(new ItemStack(BlockListMF.kitchenBench))
                .tool("hammer", -1).time(200).sound(chopping).shaped(
                        new Object[] { "KSP", "TGT", "TTT", 'K', CustomToolListMF.standard_knife, 'S', FoodListMF.salt,
                                'P', ComponentListMF.plank_cut, 'G', Blocks.stone, 'T', ComponentListMF.plank, });

        KnowledgeListMF.bloomeryR = MineFantasyAPI.carpenterRecipe(new ItemStack(BlockListMF.bloomery)).skill(artisanry)
                .research("bloomery").tool("hammer", -1).time(10).sound(stonemason)
                .shaped(new Object[] { " S ", "S S", "SCS", 'C', Blocks.coal_block, 'S', Blocks.stone, });
        KnowledgeListMF.crucibleRecipe = MineFantasyAPI.carpenterRecipe(new ItemStack(BlockListMF.crucible))
                .skill(artisanry).research("crucible").tool("hammer", -1).time(20).sound(stonemason)
                .shaped(new Object[] { "SSS", "S S", "SSS", 'S', Blocks.stone, });
        KnowledgeListMF.advCrucibleRecipe = MineFantasyAPI.carpenterRecipe(new ItemStack(BlockListMF.crucibleadv))
                .skill(artisanry).research("crucible2").time(40).sound(basic)
                .shaped(new Object[] { "SSS", "SCS", "SSS", 'S', ComponentListMF.fireclay, 'C', BlockListMF.crucible });
        Salvage.addSalvage(BlockListMF.crucible, new ItemStack(Blocks.stone, 8));
        Salvage.addSalvage(BlockListMF.crucibleadv, new ItemStack(ComponentListMF.fireclay, 8), BlockListMF.crucible);

        KnowledgeListMF.chimneyRecipe = MineFantasyAPI.carpenterRecipe(new ItemStack(BlockListMF.chimney_stone, 8))
                .skill(artisanry).tool("hammer", -1).time(30).sound(stonemason)
                .shaped(new Object[] { "S S", "S S", "S S", 'S', Blocks.stone, });
        KnowledgeListMF.wideChimneyRecipe = MineFantasyAPI
                .carpenterRecipe(new ItemStack(BlockListMF.chimney_stone_wide)).skill(artisanry).tool("hammer", -1)
                .time(10).sound(stonemason)
                .shaped(new Object[] { "S", "C", 'C', BlockListMF.chimney_stone, 'S', Blocks.stone, });
        KnowledgeListMF.extractChimneyRecipe = MineFantasyAPI
                .carpenterRecipe(new ItemStack(BlockListMF.chimney_stone_extractor)).skill(artisanry).tool("hammer", -1)
                .time(15).sound(stonemason).shaped(new Object[] { "C", 'C', BlockListMF.chimney_stone_wide, });

        KnowledgeListMF.stoneAnvilRecipe = MineFantasyAPI.carpenterRecipe(new ItemStack(BlockListMF.anvilStone))
                .tool("hammer", -1).time(10).sound(stonemason)
                .shaped(new Object[] { "SS ", "SSS", " S ", 'S', Blocks.stone });
        KnowledgeListMF.forgeRecipe = MineFantasyAPI.carpenterRecipe(new ItemStack(BlockListMF.forge))
                .tool("hammer", -1).time(10).sound(stonemason)
                .shaped(new Object[] { "S S", "SCS", 'C', Items.coal, 'S', Blocks.stone });
        Salvage.addSalvage(BlockListMF.forge, new ItemStack(Blocks.stone, 4), Items.coal);
        Salvage.addSalvage(BlockListMF.anvilStone, new ItemStack(Blocks.stone, 6));

        Salvage.addSalvage(BlockListMF.chimney_stone, Blocks.stone);
        Salvage.addSalvage(BlockListMF.chimney_stone_wide, BlockListMF.chimney_stone, Blocks.stone);
        Salvage.addSalvage(BlockListMF.chimney_stone_extractor, BlockListMF.chimney_stone_wide);
        Salvage.addSalvage(BlockListMF.quern, new ItemStack(Items.flint, 2), new ItemStack(Blocks.stone, 4));
    }

    private static void addCooking() {
        String meatRaw = "rawMeat";
        String cookedMeat = "cookedMeat";
        OreDictionary.registerOre(cookedMeat, Items.cooked_beef);
        OreDictionary.registerOre(cookedMeat, Items.cooked_chicken);
        OreDictionary.registerOre(cookedMeat, Items.cooked_porkchop);
        OreDictionary.registerOre(cookedMeat, FoodListMF.wolf_cooked);
        OreDictionary.registerOre(cookedMeat, FoodListMF.horse_cooked);
        OreDictionary.registerOre(cookedMeat, Items.cooked_fished);
        OreDictionary.registerOre(cookedMeat, new ItemStack(Items.cooked_fished, 1, 1));
        addOreD("listAllporkcooked", cookedMeat);
        addOreD("listAllmuttoncooked", cookedMeat);
        addOreD("listAllbeefcooked", cookedMeat);
        addOreD("listAllchickencooked", cookedMeat);
        addOreD("listAllfishcooked", cookedMeat);

        OreDictionary.registerOre(meatRaw, FoodListMF.guts);
        OreDictionary.registerOre(meatRaw, Items.beef);
        OreDictionary.registerOre(meatRaw, Items.chicken);
        OreDictionary.registerOre(meatRaw, Items.porkchop);
        OreDictionary.registerOre(meatRaw, FoodListMF.wolf_raw);
        OreDictionary.registerOre(meatRaw, FoodListMF.horse_raw);
        OreDictionary.registerOre(meatRaw, Items.fish);
        OreDictionary.registerOre(meatRaw, new ItemStack(Items.fish, 1, 1));
        addOreD("listAllporkraw", meatRaw);
        addOreD("listAllmuttonraw", meatRaw);
        addOreD("listAllbeefraw", meatRaw);
        addOreD("listAllchickenraw", meatRaw);
        addOreD("listAllfishraw", meatRaw);

        KnowledgeListMF.curdRecipe = MineFantasyAPI.kitchenRecipe(new ItemStack(FoodListMF.curds)).skill(provisioning)
                .tool("hands", -1).time(10).sound(basic).shaped(
                        new Object[] { "T", "S", "M", "P", 'P', ComponentListMF.clay_pot, 'T', FoodListMF.salt, 'S',
                                FoodListMF.sugarpot, 'M', FoodListMF.jug_milk, });

        KnowledgeListMF.oatsRecipe = MineFantasyAPI.kitchenRecipe(new ItemStack(FoodListMF.oats)).skill(provisioning)
                .tool("knife", -1).time(20).sound(chopping).shaped(
                        new Object[] { "M", "W", "S", "B", 'S', Items.wheat_seeds, 'W', Items.wheat, 'M',
                                FoodListMF.jug_milk, 'B', Items.bowl });
        KnowledgeListMF.doughRecipe = MineFantasyAPI.kitchenRecipe(new ItemStack(FoodListMF.dough)).skill(provisioning)
                .tool("hands", -1).time(10).sound(basic)
                .shaped(new Object[] { "W", "F", 'W', FoodListMF.jug_water, 'F', FoodListMF.flour, });
        KnowledgeListMF.pastryRecipe = MineFantasyAPI.kitchenRecipe(new ItemStack(FoodListMF.pastry))
                .skill(provisioning).tool("hands", -1).time(10).sound(basic)
                .shaped(new Object[] { " S ", "FEF", 'F', FoodListMF.flour, 'E', Items.egg, 'S', FoodListMF.salt, });
        KnowledgeListMF.breadRecipe = MineFantasyAPI.kitchenRecipe(new ItemStack(FoodListMF.raw_bread))
                .skill(provisioning).tool("hands", -1).time(15).sound(basic)
                .shaped(new Object[] { "DDD", 'D', FoodListMF.dough, });
        KnowledgeListMF.sweetrollRecipe = MineFantasyAPI.kitchenRecipe(new ItemStack(FoodListMF.sweetroll_raw))
                .skill(provisioning).research("sweetroll").time(5).sound(basic).shaped(
                        new Object[] { " M ", "FES", "BBB", 'M', FoodListMF.jug_milk, 'S', FoodListMF.sugarpot, 'B',
                                FoodListMF.berries, 'E', Items.egg, 'F', FoodListMF.flour, });
        KnowledgeListMF.icingRecipe = MineFantasyAPI.kitchenRecipe(new ItemStack(FoodListMF.icing)).skill(provisioning)
                .tool("spoon", -1).time(10).sound(mixing).shaped(
                        new Object[] { "W", "S", "B", 'W', FoodListMF.jug_water, 'S', FoodListMF.sugarpot, 'B',
                                ComponentListMF.clay_pot, });
        KnowledgeListMF.chocoRecipe = MineFantasyAPI.kitchenRecipe(new ItemStack(FoodListMF.chocolate))
                .skill(provisioning).research("icing").tool("spoon", -1).time(10).sound(mixing).shaped(
                        new Object[] { " M ", "SCS", " B ", 'C', FoodListMF.coca_powder, 'M', FoodListMF.jug_milk, 'S',
                                FoodListMF.sugarpot, 'B', ComponentListMF.clay_pot, });
        KnowledgeListMF.custardRecipe = MineFantasyAPI.kitchenRecipe(new ItemStack(FoodListMF.custard))
                .skill(provisioning).research("icing").tool("spoon", -1).time(10).sound(mixing).shaped(
                        new Object[] { " M ", "SES", " B ", 'E', Items.egg, 'M', FoodListMF.jug_milk, 'S',
                                FoodListMF.sugarpot, 'B', ComponentListMF.clay_pot, });
        KnowledgeListMF.iceSR = MineFantasyAPI.kitchenRecipe(new ItemStack(FoodListMF.sweetroll)).skill(provisioning)
                .research("sweetroll").tool("knife", -1).time(15).sound(basic)
                .shaped(new Object[] { "I", "R", 'I', FoodListMF.icing, 'R', FoodListMF.sweetroll_uniced, });
        KnowledgeListMF.eclairDoughR = MineFantasyAPI.kitchenRecipe(new ItemStack(FoodListMF.eclair_raw))
                .skill(provisioning).research("eclair").time(8).sound(basic)
                .shaped(new Object[] { "SSS", "PPP", 'P', FoodListMF.pastry, 'S', FoodListMF.sugarpot, });
        KnowledgeListMF.eclairIceR = MineFantasyAPI.kitchenRecipe(new ItemStack(FoodListMF.eclair_empty))
                .skill(provisioning).research("eclair").tool("knife", 2).time(20).sound(basic)
                .shaped(new Object[] { "C", "E", 'C', FoodListMF.chocolate, 'E', FoodListMF.eclair_uniced, });
        KnowledgeListMF.eclairFillR = MineFantasyAPI.kitchenRecipe(new ItemStack(FoodListMF.eclair)).skill(provisioning)
                .research("eclair").tool("knife", 2).time(20).sound(basic)
                .shaped(new Object[] { "C", "E", 'C', FoodListMF.custard, 'E', FoodListMF.eclair_empty, });
        NativeRecipes.eachSource(
                OreDictionary.getOres(meatRaw),
                food -> KnowledgeListMF.meatRecipes.add(
                        MineFantasyAPI.kitchenRecipe(new ItemStack(FoodListMF.generic_meat_uncooked, getSize(food)))
                                .skill(provisioning).tool("knife", -1).time(15).sound(chopping)
                                .shaped(new Object[] { "M", 'M', food, })));
        NativeRecipes.eachSource(
                OreDictionary.getOres(cookedMeat),
                food -> KnowledgeListMF.meatRecipes.add(
                        MineFantasyAPI.kitchenRecipe(new ItemStack(FoodListMF.generic_meat_cooked, 1))
                                .skill(provisioning).tool("knife", -1).time(15).sound(chopping)
                                .shaped(new Object[] { "M", 'M', food, })));
        KnowledgeListMF.meatStripR = MineFantasyAPI.kitchenRecipe(new ItemStack(FoodListMF.generic_meat_strip_uncooked))
                .skill(provisioning).tool("knife", -1).time(5).sound(chopping)
                .shaped(new Object[] { "M", 'M', FoodListMF.generic_meat_uncooked, });
        MineFantasyAPI.kitchenRecipe(new ItemStack(FoodListMF.generic_meat_strip_cooked)).skill(provisioning)
                .tool("knife", -1).time(5).sound(chopping)
                .shaped(new Object[] { "M", 'M', FoodListMF.generic_meat_cooked, });
        KnowledgeListMF.meatHunkR = MineFantasyAPI.kitchenRecipe(new ItemStack(FoodListMF.generic_meat_chunk_uncooked))
                .skill(provisioning).tool("knife", -1).time(5).sound(chopping)
                .shaped(new Object[] { "M", 'M', FoodListMF.generic_meat_strip_uncooked, });
        MineFantasyAPI.kitchenRecipe(new ItemStack(FoodListMF.generic_meat_chunk_cooked)).skill(provisioning)
                .tool("knife", -1).time(5).sound(chopping)
                .shaped(new Object[] { "M", 'M', FoodListMF.generic_meat_strip_cooked, });
        KnowledgeListMF.gutsRecipe = MineFantasyAPI.kitchenRecipe(new ItemStack(FoodListMF.guts)).skill(provisioning)
                .tool("knife", 1).time(8).sound(chopping).shaped(new Object[] { "MMMM", 'M', Items.rotten_flesh, });

        KnowledgeListMF.stewRecipe = MineFantasyAPI.kitchenRecipe(new ItemStack(FoodListMF.stew)).skill(provisioning)
                .tool("knife", -1).time(15).sound(chopping)
                .shaped(new Object[] { "M", "B", 'M', FoodListMF.generic_meat_chunk_cooked, 'B', Items.bowl });
        KnowledgeListMF.jerkyRecipe = MineFantasyAPI.kitchenRecipe(new ItemStack(FoodListMF.jerky, 1))
                .skill(provisioning).research("jerky").tool("knife", 2).time(20).sound(chopping)
                .shaped(new Object[] { "S", "M", 'S', FoodListMF.salt, 'M', FoodListMF.generic_meat_strip_cooked, });
        KnowledgeListMF.saussageR = MineFantasyAPI.kitchenRecipe(new ItemStack(FoodListMF.saussage_raw, 4))
                .skill(provisioning).research("saussage").tool("knife", 2).time(30).sound(chopping).shaped(
                        new Object[] { " G ", "MMM", "BES", 'G', FoodListMF.guts, 'E', Items.egg, 'S', FoodListMF.salt,
                                'B', FoodListMF.breadcrumbs, 'M', FoodListMF.generic_meat_mince_uncooked, });
        KnowledgeListMF.meatPieRecipe = MineFantasyAPI.kitchenRecipe(new ItemStack(FoodListMF.pie_meat_uncooked))
                .skill(provisioning).research("meatpie").tool("knife", 2).time(150).sound(chopping).shaped(
                        new Object[] { " P ", "MMM", " P ", " T ", 'P', FoodListMF.pastry, 'M',
                                FoodListMF.generic_meat_mince_cooked, 'T', FoodListMF.pie_tray, });
        KnowledgeListMF.breadSliceR = MineFantasyAPI.kitchenRecipe(new ItemStack(FoodListMF.breadSlice, 12))
                .skill(provisioning).tool("knife", -1).time(10).sound("step.cloth")
                .shaped(new Object[] { "B", 'B', Items.bread, });
        KnowledgeListMF.sandwitchRecipe = MineFantasyAPI.kitchenRecipe(new ItemStack(FoodListMF.sandwitch_meat))
                .skill(provisioning).research("sandwitch").tool("hands", -1).time(4).sound(chopping).shaped(
                        new Object[] { "B", "C", "M", "B", 'C', FoodListMF.cheese_slice, 'M',
                                FoodListMF.generic_meat_cooked, 'B', FoodListMF.breadSlice });
        KnowledgeListMF.sandwitchBigRecipe = MineFantasyAPI.kitchenRecipe(new ItemStack(FoodListMF.sandwitch_big))
                .skill(provisioning).research("sandwitchBig").tool("knife", 1).time(10).sound(chopping).shaped(
                        new Object[] { "CSC", "MBM", 'S', FoodListMF.salt, 'C', FoodListMF.cheese_slice, 'M',
                                FoodListMF.generic_meat_cooked, 'B', Items.bread });
        KnowledgeListMF.shepardRecipe = MineFantasyAPI.kitchenRecipe(new ItemStack(FoodListMF.pie_shepard_uncooked))
                .skill(provisioning).research("shepardpie").tool("knife", 3).time(200).sound(chopping).shaped(
                        new Object[] { "PFP", "MMM", "CFC", " T ", 'C', Items.carrot, 'P', Items.potato, 'F',
                                FoodListMF.pastry, 'M', FoodListMF.generic_meat_mince_cooked, 'T',
                                FoodListMF.pie_tray, });

        KnowledgeListMF.appleR = MineFantasyAPI.kitchenRecipe(new ItemStack(FoodListMF.pie_apple_uncooked))
                .skill(provisioning).research("applepie").tool("knife", 2).time(120).sound(chopping).shaped(
                        new Object[] { "SPS", "MMM", "SPS", " T ", 'S', FoodListMF.sugarpot, 'P', FoodListMF.pastry,
                                'M', Items.apple, 'T', FoodListMF.pie_tray, });
        KnowledgeListMF.pumpPieR = MineFantasyAPI.kitchenRecipe(new ItemStack(FoodListMF.pie_pumpkin_uncooked))
                .skill(provisioning).research("bread").tool("knife", 1).time(50).sound(chopping).shaped(
                        new Object[] { "SMS", "SPS", " T ", 'S', FoodListMF.sugarpot, 'P', FoodListMF.pastry, 'M',
                                Blocks.pumpkin, 'T', FoodListMF.pie_tray, });
        KnowledgeListMF.berryR = MineFantasyAPI.kitchenRecipe(new ItemStack(FoodListMF.pie_berry_uncooked))
                .skill(provisioning).research("berrypie").tool("knife", 2).time(100).sound(chopping).shaped(
                        new Object[] { "SPS", "MMM", "SPS", " T ", 'S', FoodListMF.sugarpot, 'P', FoodListMF.pastry,
                                'M', FoodListMF.berries, 'T', FoodListMF.pie_tray, });

        KnowledgeListMF.simpCakeR = MineFantasyAPI.kitchenRecipe(new ItemStack(FoodListMF.cake_simple_raw))
                .skill(provisioning).research("bread").tool("spoon", -1).time(15).sound(mixing).shaped(
                        new Object[] { "MMM", "SES", "FFF", " T ", 'F', FoodListMF.flour, 'E', Items.egg, 'M',
                                FoodListMF.jug_milk, 'S', FoodListMF.sugarpot, 'T', FoodListMF.cake_tin, });

        KnowledgeListMF.cakeR = MineFantasyAPI.kitchenRecipe(new ItemStack(FoodListMF.cake_raw)).skill(provisioning)
                .research("cake").tool("spoon", -1).time(20).sound(mixing).shaped(
                        new Object[] { "SMS", "SES", "FFF", " T ", 'F', FoodListMF.flour, 'E', Items.egg, 'M',
                                FoodListMF.jug_milk, 'S', FoodListMF.sugarpot, 'T', FoodListMF.cake_tin, });
        KnowledgeListMF.carrotCakeR = MineFantasyAPI.kitchenRecipe(new ItemStack(FoodListMF.cake_carrot_raw))
                .skill(provisioning).research("carrotcake").tool("spoon", -1).time(25).sound(mixing).shaped(
                        new Object[] { "SMS", "SES", "CCC", "FTF", 'C', Items.carrot, 'F', FoodListMF.flour, 'E',
                                Items.egg, 'M', FoodListMF.jug_milk, 'S', FoodListMF.sugarpot, 'T',
                                FoodListMF.cake_tin, });
        KnowledgeListMF.chocoCakeR = MineFantasyAPI.kitchenRecipe(new ItemStack(FoodListMF.cake_choc_raw))
                .skill(provisioning).research("chococake").tool("spoon", -1).time(25).sound(mixing).shaped(
                        new Object[] { "SMS", "SES", "CCC", "FTF", 'C', FoodListMF.chocolate, 'F', FoodListMF.flour,
                                'E', Items.egg, 'M', FoodListMF.jug_milk, 'S', FoodListMF.sugarpot, 'T',
                                FoodListMF.cake_tin, });
        KnowledgeListMF.bfCakeR = MineFantasyAPI.kitchenRecipe(new ItemStack(FoodListMF.cake_bf_raw))
                .skill(provisioning).research("bfcake").tool("spoon", -1).time(30).sound(mixing).shaped(
                        new Object[] { "SMMS", "SEES", "CBBC", "FTFF", 'B', FoodListMF.berriesJuicy, 'C',
                                FoodListMF.chocolate, 'F', FoodListMF.flour, 'E', Items.egg, 'M', FoodListMF.jug_milk,
                                'S', FoodListMF.sugarpot, 'T', FoodListMF.cake_tin, });
        KnowledgeListMF.simpCakeOut = MineFantasyAPI.kitchenRecipe(new ItemStack(Items.cake)).skill(provisioning)
                .research("bread").tool("knife", -1).time(10).sound(basic)
                .shaped(new Object[] { "I", "R", 'I', FoodListMF.icing, 'R', FoodListMF.cake_simple_uniced, });

        KnowledgeListMF.cakeI = MineFantasyAPI.kitchenRecipe(new ItemStack(BlockListMF.cake_vanilla))
                .skill(provisioning).research("cake").tool("knife", -1).time(60).sound(basic)
                .shaped(new Object[] { "III", " R ", 'I', FoodListMF.icing, 'R', FoodListMF.cake_uniced, });
        KnowledgeListMF.carrotCakeI = MineFantasyAPI.kitchenRecipe(new ItemStack(BlockListMF.cake_carrot))
                .skill(provisioning).research("carrotcake").tool("knife", -1).time(60).sound(basic)
                .shaped(new Object[] { "III", " R ", 'I', FoodListMF.icing, 'R', FoodListMF.cake_carrot_uniced, });
        KnowledgeListMF.chocoCakeI = MineFantasyAPI.kitchenRecipe(new ItemStack(BlockListMF.cake_chocolate))
                .skill(provisioning).research("chococake").tool("knife", -1).time(60).sound(basic).shaped(
                        new Object[] { "ICI", " R ", 'C', FoodListMF.chocolate, 'I', FoodListMF.icing, 'R',
                                FoodListMF.cake_choc_uniced, });
        KnowledgeListMF.bfCakeI = MineFantasyAPI.kitchenRecipe(new ItemStack(BlockListMF.cake_bf)).skill(provisioning)
                .research("bfcake").tool("knife", -1).time(100).sound(basic).shaped(
                        new Object[] { "BBB", "III", "CRC", 'C', FoodListMF.chocolate, 'B', FoodListMF.berries, 'I',
                                FoodListMF.icing, 'R', FoodListMF.cake_bf_uniced, });

        KnowledgeListMF.cheeserollR = MineFantasyAPI.kitchenRecipe(new ItemStack(FoodListMF.cheese_roll))
                .skill(provisioning).research("cheeseroll").tool("knife", 1).time(30).sound(chopping)
                .shaped(new Object[] { "C", "R", 'C', FoodListMF.cheese_slice, 'R', FoodListMF.breadroll, });
    }

    private static void addOreD(String list, String mfList) {
        for (ItemStack stack : OreDictionary.getOres(list)) {
            OreDictionary.registerOre(mfList, stack);
        }
    }

    private static int getSize(ItemStack food) {
        if (food != null && food.getItem() instanceof ItemFood) {
            int feed = ((ItemFood) food.getItem()).func_150905_g(food);
            return Math.max(1, feed - 1);
        }
        return 1;
    }

    private static void addMisc() {
        // Fletching
        KnowledgeListMF.fletchingR = MineFantasyAPI.carpenterRecipe(new ItemStack(ComponentListMF.fletching, 16))
                .skill(artisanry).research("arrows").time(4).sound(chopping).shaped(
                        new Object[] { "T", "F",

                                'F', Items.feather, 'T', ComponentListMF.plank, });
        try (NativeRecipes.Variant v = NativeRecipes.variant("paper")) {
            KnowledgeListMF.fletchingR2 = MineFantasyAPI.carpenterRecipe(new ItemStack(ComponentListMF.fletching, 4))
                    .skill(artisanry).research("arrows").time(4).sound(chopping).shaped(
                            new Object[] { " T ", "PPP",

                                    'P', Items.paper, 'T', ComponentListMF.plank, });
        }

        // BOMBS
        KnowledgeListMF.bombCaseCeramicR = MineFantasyAPI
                .carpenterRecipe(new ItemStack(ComponentListMF.bomb_casing_uncooked, 2)).skill(engineering)
                .research("bombCeramic").time(2).sound(basic)
                .shaped(new Object[] { " C ", "C C", " C ", 'C', Items.clay_ball, });
        KnowledgeListMF.mineCaseCeramicR = MineFantasyAPI
                .carpenterRecipe(new ItemStack(ComponentListMF.mine_casing_uncooked)).skill(engineering)
                .research("mineCeramic").time(2).sound(basic).shaped(
                        new Object[] { " P ", "C C", " C ",

                                'P', Blocks.stone_pressure_plate, 'C', Items.clay_ball, });
        KnowledgeListMF.bombCaseCrystalR = MineFantasyAPI
                .carpenterRecipe(new ItemStack(ComponentListMF.bomb_casing_crystal)).skill(engineering)
                .research("bombCrystal").time(10).sound(basic).shaped(
                        new Object[] { " D ", "R R", " B ", 'B', Items.glass_bottle, 'D',
                                ComponentListMF.diamond_shards, 'R', Items.redstone });
        KnowledgeListMF.mineCaseCrystalR = MineFantasyAPI
                .carpenterRecipe(new ItemStack(ComponentListMF.mine_casing_crystal)).skill(engineering)
                .research("mineCrystal").time(10).sound(basic).shaped(
                        new Object[] { " P ", "RDR", " B ", 'P', Blocks.heavy_weighted_pressure_plate, 'B',
                                Items.glass_bottle, 'D', ComponentListMF.diamond_shards, 'R', Items.redstone });
        Salvage.addSalvage(ComponentListMF.bomb_casing_uncooked, new ItemStack(Items.clay_ball, 2));
        Salvage.addSalvage(
                ComponentListMF.mine_casing_uncooked,
                new ItemStack(Items.clay_ball, 3),
                Blocks.stone_pressure_plate);

        Salvage.addSalvage(
                ComponentListMF.bomb_casing_crystal,
                Items.glass_bottle,
                ComponentListMF.diamond_shards,
                new ItemStack(Items.redstone, 2));
        Salvage.addSalvage(
                ComponentListMF.mine_casing_crystal,
                Blocks.heavy_weighted_pressure_plate,
                Items.glass_bottle,
                ComponentListMF.diamond_shards,
                new ItemStack(Items.redstone, 2));

        KnowledgeListMF.bombFuseR = MineFantasyAPI.carpenterRecipe(new ItemStack(ComponentListMF.bomb_fuse, 8))
                .skill(engineering).research("bombs").time(4).sound(basic).shaped(
                        new Object[] { "R", "C", "S", 'S', ComponentListMF.thread, 'C', ComponentListMF.coalDust, 'R',
                                Items.redstone, });
        KnowledgeListMF.longFuseR = MineFantasyAPI.carpenterRecipe(new ItemStack(ComponentListMF.bomb_fuse_long))
                .skill(engineering).research("bombs").time(1).sound(basic)
                .shaped(new Object[] { "F", "R", "F", 'F', ComponentListMF.bomb_fuse, 'R', Items.redstone, });
        Salvage.addSalvage(ComponentListMF.bomb_fuse_long, new ItemStack(ComponentListMF.bomb_fuse, 2), Items.redstone);

        KnowledgeListMF.thatchR = MineFantasyAPI.carpenterRecipe(new ItemStack(BlockListMF.thatch)).skill(construction)
                .tool("hands", -1).time(1).sound("dig.grass")
                .shaped(new Object[] { "HH", "HH", 'H', new ItemStack(Blocks.tallgrass, 1, 1) });
        KnowledgeListMF.thatchStairR = MineFantasyAPI.carpenterRecipe(new ItemStack(BlockListMF.thatch_stair))
                .skill(construction).tool("hands", -1).time(1).sound("dig.grass")
                .shaped(new Object[] { "H ", "HH", 'H', new ItemStack(Blocks.tallgrass, 1, 1) });
        Salvage.addSalvage(BlockListMF.thatch_stair, new ItemStack(Blocks.tallgrass, 3, 1));
        Salvage.addSalvage(BlockListMF.thatch, new ItemStack(Blocks.tallgrass, 4, 1));

        KnowledgeListMF.apronRecipe = MineFantasyAPI.carpenterRecipe(new ItemStack(ArmourListMF.leatherapron))
                .tool("hands", -1).time(1).sound(sewing)
                .shaped(new Object[] { "LCL", " L ", 'L', Items.leather, 'C', Items.coal, });
        Salvage.addSalvage(ArmourListMF.leatherapron, new ItemStack(Items.leather, 3), Items.coal);

        KnowledgeListMF.hideHelmR = MineFantasyAPI.carpenterRecipe(ArmourListMF.armour(ArmourListMF.leather, 0, 0))
                .tool("hands", -1).time(1).sound(sewing)
                .shaped(new Object[] { "H", "C", "H", 'H', ComponentListMF.hideSmall, 'C', Blocks.wool, });
        KnowledgeListMF.hideChestR = MineFantasyAPI.carpenterRecipe(ArmourListMF.armour(ArmourListMF.leather, 0, 1))
                .tool("hands", -1).time(1).sound(sewing)
                .shaped(new Object[] { "H", "C", 'H', ComponentListMF.hideLarge, 'C', Blocks.wool, });
        KnowledgeListMF.hideLegsR = MineFantasyAPI.carpenterRecipe(ArmourListMF.armour(ArmourListMF.leather, 0, 2))
                .tool("hands", -1).time(1).sound(sewing)
                .shaped(new Object[] { "H", "C", 'H', ComponentListMF.hideMedium, 'C', Blocks.wool, });
        KnowledgeListMF.hideBootsR = MineFantasyAPI.carpenterRecipe(ArmourListMF.armour(ArmourListMF.leather, 0, 3))
                .tool("hands", -1).time(1).sound(sewing)
                .shaped(new Object[] { "H", "C", 'H', ComponentListMF.hideSmall, 'C', Blocks.wool, });

        Salvage.addSalvage(
                ArmourListMF.armourItem(ArmourListMF.leather, 0, 0),
                new ItemStack(ComponentListMF.hideSmall, 2),
                Blocks.wool);
        Salvage.addSalvage(ArmourListMF.armourItem(ArmourListMF.leather, 0, 1), ComponentListMF.hideLarge, Blocks.wool);
        Salvage.addSalvage(
                ArmourListMF.armourItem(ArmourListMF.leather, 0, 2),
                ComponentListMF.hideMedium,
                Blocks.wool);
        Salvage.addSalvage(ArmourListMF.armourItem(ArmourListMF.leather, 0, 3), ComponentListMF.hideSmall, Blocks.wool);

        KnowledgeListMF.bedrollR = MineFantasyAPI.carpenterRecipe(new ItemStack(ToolListMF.bedroll)).tool("needle", -1)
                .time(50).sound(sewing).shaped(
                        new Object[] { "TLT", "CCC", 'C', Blocks.wool, 'T', ComponentListMF.thread, 'L',
                                Items.leather });
        Salvage.addSalvage(
                ToolListMF.bedroll,
                new ItemStack(Blocks.wool, 3),
                Items.leather,
                new ItemStack(ComponentListMF.thread, 3));
    }

    public static void addCrossbows() {
        // CROSSBOWS
        KnowledgeListMF.crossHandleWoodR = MineFantasyAPI
                .carpenterRecipe(new ItemStack(ComponentListMF.crossbow_handle_wood)).skill(engineering)
                .research("crossShafts").tool("hammer", 2).time(150).sound(nailHammer).shaped(
                        new Object[] { "N N", "PP ", " P ", 'P', ComponentListMF.plank.construct("RefinedWood"), 'N',
                                ComponentListMF.nail });
        KnowledgeListMF.crossStockWoodR = MineFantasyAPI
                .carpenterRecipe(new ItemStack(ComponentListMF.crossbow_stock_wood)).skill(engineering)
                .research("crossShafts").tool("hammer", 2).time(300).sound(nailHammer).shaped(
                        new Object[] { "NN N", "PPPP", " PPP", 'P', ComponentListMF.plank.construct("RefinedWood"), 'N',
                                ComponentListMF.nail });
        KnowledgeListMF.crossStockIronR = MineFantasyAPI
                .carpenterRecipe(new ItemStack(ComponentListMF.crossbow_stock_iron)).skill(engineering)
                .research("crossShaftAdvanced").tool("spanner", 2).time(300).sound(spanner).shaped(
                        new Object[] { " BBB", "BOGG", "SWSS", "    ", 'O', Blocks.obsidian, 'G',
                                ComponentListMF.tungsten_gears, 'W', ComponentListMF.crossbow_stock_wood, 'S',
                                ComponentListMF.iron_strut, 'B', ComponentListMF.bolt, });

        KnowledgeListMF.crossHeadLightR = MineFantasyAPI
                .carpenterRecipe(new ItemStack(ComponentListMF.cross_arms_light)).skill(engineering)
                .research("crossHeads").tool("hammer", 2).time(200).sound(nailHammer).shaped(
                        new Object[] { "PPP", "NSN", " P ", 'P', ComponentListMF.plank.construct("RefinedWood"), 'N',
                                ComponentListMF.nail, 'S', Items.string, });
        KnowledgeListMF.crossHeadMediumR = MineFantasyAPI
                .carpenterRecipe(new ItemStack(ComponentListMF.cross_arms_basic)).skill(engineering)
                .research("crossHeads").tool("hammer", 2).time(250).sound(nailHammer).shaped(
                        new Object[] { "NNN", "PAP", 'N', ComponentListMF.nail, 'P',
                                ComponentListMF.plank.construct("RefinedWood"), 'A',
                                ComponentListMF.cross_arms_light, });
        KnowledgeListMF.crossHeadHeavyR = MineFantasyAPI
                .carpenterRecipe(new ItemStack(ComponentListMF.cross_arms_heavy)).skill(engineering)
                .research("crossHeads").tool("hammer", 2).time(350).sound(nailHammer).shaped(
                        new Object[] { "NNN", "PAP", 'N', ComponentListMF.nail, 'P',
                                ComponentListMF.plank.construct("RefinedWood"), 'A',
                                ComponentListMF.cross_arms_basic, });
        KnowledgeListMF.crossHeadAdvancedR = MineFantasyAPI
                .carpenterRecipe(new ItemStack(ComponentListMF.cross_arms_advanced)).skill(engineering)
                .research("crossHeadAdvanced").tool("hammer", 2).time(350).sound(nailHammer).shaped(
                        new Object[] { "NRN", "RGR", " A ", 'G', ComponentListMF.tungsten_gears, 'N',
                                ComponentListMF.nail, 'R', ComponentListMF.steel_tube, 'A',
                                ComponentListMF.cross_arms_basic, });

        KnowledgeListMF.crossAmmoR = MineFantasyAPI.carpenterRecipe(new ItemStack(ComponentListMF.cross_ammo))
                .skill(engineering).research("crossAmmo").tool("hammer", 2).time(200).sound(nailHammer).shaped(
                        new Object[] { "NNN", "P P", "PGP", "PPP", 'G', ComponentListMF.tungsten_gears, 'P',
                                ComponentListMF.plank.construct("RefinedWood"), 'N', ComponentListMF.nail, });
        KnowledgeListMF.crossScopeR = MineFantasyAPI.carpenterRecipe(new ItemStack(ComponentListMF.cross_scope))
                .skill(engineering).research("crossScope").tool("spanner", 2).time(150).sound(spanner).shaped(
                        new Object[] { "BSB", "GP ", 'G', ComponentListMF.tungsten_gears, 'S', ToolListMF.spyglass, 'P',
                                ComponentListMF.plank.construct("RefinedWood"), 'B', ComponentListMF.bolt, });
        Salvage.addSalvage(
                ComponentListMF.cross_arms_light,
                new ItemStack(ComponentListMF.nail, 2),
                Items.string,
                ComponentListMF.plank.construct("RefinedWood", 4));
        Salvage.addSalvage(
                ComponentListMF.cross_arms_basic,
                new ItemStack(ComponentListMF.nail, 3),
                ComponentListMF.cross_arms_light,
                ComponentListMF.plank.construct("RefinedWood", 2));
        Salvage.addSalvage(
                ComponentListMF.cross_arms_heavy,
                new ItemStack(ComponentListMF.nail, 3),
                ComponentListMF.cross_arms_basic,
                ComponentListMF.plank.construct("RefinedWood", 2));
        Salvage.addSalvage(
                ComponentListMF.cross_arms_advanced,
                ComponentListMF.tungsten_gears,
                new ItemStack(ComponentListMF.nail, 2),
                ComponentListMF.cross_arms_basic,
                new ItemStack(ComponentListMF.steel_tube, 3));

        Salvage.addSalvage(
                ComponentListMF.cross_scope,
                ComponentListMF.tungsten_gears,
                ToolListMF.spyglass,
                new ItemStack(ComponentListMF.bolt, 2),
                ComponentListMF.plank.construct("RefinedWood"));
        Salvage.addSalvage(
                ComponentListMF.cross_ammo,
                ComponentListMF.tungsten_gears,
                new ItemStack(ComponentListMF.nail, 3),
                ComponentListMF.plank.construct("RefinedWood", 7));
        Salvage.addSalvage(
                ComponentListMF.crossbow_handle_wood,
                new ItemStack(ComponentListMF.nail, 2),
                ComponentListMF.plank.construct("RefinedWood", 3));
        Salvage.addSalvage(
                ComponentListMF.crossbow_stock_wood,
                new ItemStack(ComponentListMF.nail, 3),
                ComponentListMF.plank.construct("RefinedWood", 7));
        Salvage.addSalvage(
                ComponentListMF.crossbow_stock_iron,
                new ItemStack(ComponentListMF.tungsten_gears, 2),
                Blocks.obsidian,
                new ItemStack(ComponentListMF.bolt, 4),
                new ItemStack(ComponentListMF.iron_strut, 3),
                ComponentListMF.crossbow_stock_wood);
    }

    private static void addEngineering() {
        addCrossbows();
        KnowledgeListMF.bombBenchCraft = MineFantasyAPI.carpenterRecipe(new ItemStack(BlockListMF.bombBench))
                .skill(engineering).research("bombs").tool("spanner", 0).time(150).sound(spanner).shaped(
                        new Object[] { "BFB", "BCB", 'B', ComponentListMF.bolt, 'F', ComponentListMF.iron_frame, 'C',
                                BlockListMF.carpenter, });
        KnowledgeListMF.bombPressCraft = MineFantasyAPI.carpenterRecipe(new ItemStack(BlockListMF.bombPress))
                .skill(engineering).research("bpress").tool("spanner", 3).time(200).sound(spanner).shaped(
                        new Object[] { "BFB", "GGL", "SPS", 'S', ComponentListMF.iron_strut, 'B', ComponentListMF.bolt,
                                'F', ComponentListMF.iron_frame, 'L', Blocks.lever, 'P',
                                new ItemStack(CustomToolListMF.standard_spanner, 1, 0), 'G',
                                ComponentListMF.bronze_gears, });

        KnowledgeListMF.crossBenchCraft = MineFantasyAPI.carpenterRecipe(new ItemStack(BlockListMF.crossbowBench))
                .skill(engineering).research("crossbows").tool("spanner", 0).time(200).sound(spanner).shaped(
                        new Object[] { " F ", "PSP", "NCN", 'F', ComponentListMF.iron_frame, 'P', ComponentListMF.plank,
                                'N', ComponentListMF.bolt, 'S', Items.string, 'C', BlockListMF.carpenter, });

        KnowledgeListMF.engTannerR = MineFantasyAPI.carpenterRecipe(new ItemStack(BlockListMF.engTanner))
                .skill(engineering).research("engTanner").tool("spanner", 3).time(300).sound(spanner).shaped(
                        new Object[] { "BLB", "SPS", "GGG", "SFS", 'S', ComponentListMF.iron_strut, 'B',
                                ComponentListMF.bolt, 'F', ComponentListMF.iron_frame, 'L', Blocks.lever, 'P',
                                new ItemStack(CustomToolListMF.standard_knife, 1, 0), 'G',
                                ComponentListMF.bronze_gears, });
        ItemStack blackPlate = ComponentListMF.plate.createComm("blackSteel");
        KnowledgeListMF.advancedForgeR = MineFantasyAPI.carpenterRecipe(new ItemStack(BlockListMF.forge_metal))
                .skill(engineering).research("advforge").tool("spanner", 4).time(400).sound(spanner).shaped(
                        new Object[] { " T  ", "FRRF", "PPPP", "BBBB", 'B', ComponentListMF.bolt, 'F',
                                ComponentListMF.iron_frame, 'T', ToolListMF.engin_anvil_tools, 'P', blackPlate, 'R',
                                Blocks.redstone_block, });
        ItemStack steelPlate = ComponentListMF.plate.createComm("steel");
        KnowledgeListMF.autoCrucibleR = MineFantasyAPI.carpenterRecipe(new ItemStack(BlockListMF.crucibleauto))
                .skill(engineering).research("advcrucible").tool("spanner", 4).time(200).sound(spanner).shaped(
                        new Object[] { " T ", "PCP", "PGP", "BBB", 'B', ComponentListMF.bolt, 'C',
                                BlockListMF.crucibleadv, 'G', ComponentListMF.tungsten_gears, 'T',
                                ToolListMF.engin_anvil_tools, 'P', steelPlate });
        KnowledgeListMF.spyglassR = MineFantasyAPI.carpenterRecipe(new ItemStack(ToolListMF.spyglass))
                .skill(engineering).research("spyglass").tool("spanner", 1).time(300).sound(spanner).shaped(
                        new Object[] { " T ", "BCB", "GPG", 'C', ComponentListMF.bronze_gears, 'G', Blocks.glass, 'B',
                                ComponentListMF.bolt, 'T', ToolListMF.engin_anvil_tools, 'P',
                                ComponentListMF.steel_tube, });

        KnowledgeListMF.syringeR = MineFantasyAPI.carpenterRecipe(new ItemStack(ToolListMF.syringe_empty))
                .skill(engineering).research("syringe").tool("spanner", 1).time(200).sound(spanner).shaped(
                        new Object[] { "E", "T", "B", "N", 'E', ToolListMF.engin_anvil_tools, 'T',
                                ComponentListMF.steel_tube, 'B', Items.glass_bottle, 'N',
                                new ItemStack(CustomToolListMF.standard_needle), });

        KnowledgeListMF.parachuteR = MineFantasyAPI.carpenterRecipe(new ItemStack(ToolListMF.parachute))
                .skill(engineering).research("parachute").tool("needle", 1).time(350).sound(sewing).shaped(
                        new Object[] { "TTT", "CCC", "BEB", "BLB", 'E', ToolListMF.engin_anvil_tools, 'T',
                                ComponentListMF.thread, 'B', ComponentListMF.leather_strip, 'L', Items.leather, 'C',
                                Blocks.wool, });
        KnowledgeListMF.cogShaftR = MineFantasyAPI.carpenterRecipe(new ItemStack(ComponentListMF.cogwork_shaft))
                .skill(engineering).research("cogArmour").tool("spanner", 4).time(150).sound(spanner).shaped(
                        new Object[] { "BPB", "SGS", "BFB",

                                'P', Blocks.piston, 'G', ComponentListMF.tungsten_gears, 'B', ComponentListMF.bolt, 'F',
                                ComponentListMF.iron_frame, 'S', ComponentListMF.iron_strut, });
        Salvage.addSalvage(
                ComponentListMF.cogwork_shaft,
                new ItemStack(ComponentListMF.iron_strut, 2),
                new ItemStack(ComponentListMF.bolt, 4),
                ComponentListMF.iron_frame,
                Blocks.piston,
                ComponentListMF.tungsten_gears);

        Salvage.addSalvage(
                BlockListMF.crucibleauto,
                new ItemStack(ComponentListMF.bolt, 3),
                ComponentListMF.tungsten_gears,
                BlockListMF.crucibleadv,
                new ItemStack(steelPlate.getItem(), 4, steelPlate.getItemDamage()));
        Salvage.addSalvage(
                BlockListMF.bombBench,
                new ItemStack(ComponentListMF.bolt, 4),
                ComponentListMF.iron_frame,
                BlockListMF.carpenter);
        Salvage.addSalvage(
                BlockListMF.crossbowBench,
                new ItemStack(ComponentListMF.nail, 2),
                ComponentListMF.plank.construct("ScrapWood", 2),
                Items.string,
                BlockListMF.carpenter);
        Salvage.addSalvage(
                BlockListMF.bombPress,
                new ItemStack(ComponentListMF.iron_strut, 2),
                new ItemStack(ComponentListMF.bolt, 2),
                new ItemStack(ComponentListMF.bronze_gears, 2),
                Blocks.lever,
                ComponentListMF.iron_frame);
        Salvage.addSalvage(
                BlockListMF.engTanner,
                new ItemStack(ComponentListMF.iron_strut, 4),
                new ItemStack(ComponentListMF.bolt, 2),
                new ItemStack(ComponentListMF.bronze_gears, 3),
                CustomToolListMF.standard_needle,
                Blocks.lever,
                ComponentListMF.iron_frame);
        Salvage.addSalvage(
                BlockListMF.forge_metal,
                new ItemStack(ComponentListMF.bolt, 4),
                new ItemStack(blackPlate.getItem(), 4, blackPlate.getItemDamage()),
                new ItemStack(ComponentListMF.iron_frame, 2),
                new ItemStack(Blocks.redstone_block, 2));
        Salvage.addSalvage(
                ToolListMF.spyglass,
                new ItemStack(ComponentListMF.bolt, 2),
                new ItemStack(Blocks.glass, 2),
                ComponentListMF.steel_tube,
                ComponentListMF.bronze_gears);
        Salvage.addSalvage(
                ToolListMF.syringe_empty,
                Items.glass_bottle,
                CustomToolListMF.standard_needle,
                ComponentListMF.steel_tube);
        Salvage.addSalvage(
                ToolListMF.parachute,
                new ItemStack(ComponentListMF.thread, 3),
                new ItemStack(Blocks.wool, 3),
                new ItemStack(ComponentListMF.leather_strip, 4),
                Items.leather);
    }

    private static void addNonPrimitiveStone() {
        KnowledgeListMF.stoneKnifeR = MineFantasyAPI.carpenterRecipe(new ItemStack(ToolListMF.knifeStone))
                .tool("hands", -1).time(4).sound(primitive)
                .shaped(new Object[] { "R", "R", "S", 'R', Blocks.cobblestone, 'S', ComponentListMF.plank, });
        KnowledgeListMF.stoneHammerR = MineFantasyAPI.carpenterRecipe(new ItemStack(ToolListMF.hammerStone))
                .tool("hands", -1).time(4).sound(primitive)
                .shaped(new Object[] { "R", "S", 'R', Blocks.cobblestone, 'S', ComponentListMF.plank, });

        KnowledgeListMF.stoneTongsR = MineFantasyAPI.carpenterRecipe(new ItemStack(ToolListMF.tongsStone))
                .tool("hands", -1).time(4).sound(primitive)
                .shaped(new Object[] { "R ", "SR", 'R', Blocks.cobblestone, 'S', ComponentListMF.plank, });
        KnowledgeListMF.boneNeedleR = MineFantasyAPI.carpenterRecipe(new ItemStack(ToolListMF.needleBone))
                .tool("hands", -1).time(4).sound(primitive).shaped(new Object[] { "B", 'B', Items.bone, });

        Salvage.addSalvage(
                ToolListMF.knifeStone,
                new ItemStack(Blocks.cobblestone, 2),
                ComponentListMF.plank.construct("ScrapWood"));
        Salvage.addSalvage(ToolListMF.hammerStone, Blocks.cobblestone, ComponentListMF.plank.construct("ScrapWood"));
        Salvage.addSalvage(
                ToolListMF.tongsStone,
                new ItemStack(Blocks.cobblestone, 2),
                ComponentListMF.plank.construct("ScrapWood"));
        Salvage.addSalvage(ToolListMF.needleBone, Items.bone);
    }

    private static void addPrimitive() {
        KnowledgeListMF.dirtRockR = MineFantasyAPI.carpenterRecipe(new ItemStack(ComponentListMF.sharp_rock))
                .tool("hands", -1).time(1).sound("minecraft:dig.gravel")
                .shaped(new Object[] { "D", 'D', Blocks.dirt, });

        KnowledgeListMF.stonePickR = MineFantasyAPI.carpenterRecipe(new ItemStack(ToolListMF.pickStone))
                .tool("hands", -1).time(5).sound(primitive).shaped(
                        new Object[] { "RVR", " S ", " S ", 'R', ComponentListMF.sharp_rock, 'V', ComponentListMF.vine,
                                'S', Items.stick });
        KnowledgeListMF.stoneAxeR = MineFantasyAPI.carpenterRecipe(new ItemStack(ToolListMF.axeStone)).tool("hands", -1)
                .time(5).sound(primitive).shaped(
                        new Object[] { "RV", "RS", " S", 'R', ComponentListMF.sharp_rock, 'V', ComponentListMF.vine,
                                'S', Items.stick });
        KnowledgeListMF.stoneSpadeR = MineFantasyAPI.carpenterRecipe(new ItemStack(ToolListMF.spadeStone))
                .tool("hands", -1).time(5).sound(primitive).shaped(
                        new Object[] { "VR", " S", " S", 'R', ComponentListMF.sharp_rock, 'V', ComponentListMF.vine,
                                'S', Items.stick });
        KnowledgeListMF.stoneHoeR = MineFantasyAPI.carpenterRecipe(new ItemStack(ToolListMF.hoeStone)).tool("hands", -1)
                .time(5).sound(primitive).shaped(
                        new Object[] { "RV", " S", " S", 'R', ComponentListMF.sharp_rock, 'V', ComponentListMF.vine,
                                'S', Items.stick });
        KnowledgeListMF.stoneSwordR = MineFantasyAPI.carpenterRecipe(new ItemStack(ToolListMF.swordStone))
                .tool("hands", -1).time(8).sound(primitive).shaped(
                        new Object[] { "R ", "R ", "SV", 'R', ComponentListMF.sharp_rock, 'V', ComponentListMF.vine,
                                'S', Items.stick });
        KnowledgeListMF.stoneWarR = MineFantasyAPI.carpenterRecipe(new ItemStack(ToolListMF.waraxeStone))
                .tool("hands", -1).time(8).sound(primitive).shaped(
                        new Object[] { "VRV", "RS", " S", 'R', ComponentListMF.sharp_rock, 'V', ComponentListMF.vine,
                                'S', Items.stick });
        KnowledgeListMF.stoneMaceR = MineFantasyAPI.carpenterRecipe(new ItemStack(ToolListMF.maceStone))
                .tool("hands", -1).time(8).sound(primitive).shaped(
                        new Object[] { " V ", "RSR", " S ", 'R', ComponentListMF.sharp_rock, 'V', ComponentListMF.vine,
                                'S', Items.stick });
        KnowledgeListMF.stoneSpearR = MineFantasyAPI.carpenterRecipe(new ItemStack(ToolListMF.spearStone))
                .tool("hands", -1).time(8).sound(primitive).shaped(
                        new Object[] { "R", "V", "S", "S", 'R', ComponentListMF.sharp_rock, 'V', ComponentListMF.vine,
                                'S', Items.stick });
        KnowledgeListMF.stoneKnifeR = MineFantasyAPI.carpenterRecipe(new ItemStack(ToolListMF.knifeStone))
                .tool("hands", -1).time(4).sound(primitive).shaped(
                        new Object[] { "R ", "SV", 'R', ComponentListMF.sharp_rock, 'V', ComponentListMF.vine, 'S',
                                Items.stick });
        KnowledgeListMF.stoneHammerR = MineFantasyAPI.carpenterRecipe(new ItemStack(ToolListMF.hammerStone))
                .tool("hands", -1).time(4).sound(primitive).shaped(
                        new Object[] { "R", "V", "S", 'R', ComponentListMF.sharp_rock, 'V', ComponentListMF.vine, 'S',
                                Items.stick });

        KnowledgeListMF.stoneTongsR = MineFantasyAPI.carpenterRecipe(new ItemStack(ToolListMF.tongsStone))
                .tool("hands", -1).time(4).sound(primitive).shaped(
                        new Object[] { " R", "SV", 'R', ComponentListMF.sharp_rock, 'V', ComponentListMF.vine, 'S',
                                Items.stick });
        KnowledgeListMF.boneNeedleR = MineFantasyAPI.carpenterRecipe(new ItemStack(ToolListMF.needleBone))
                .tool("hands", -1).time(4).sound(primitive).shaped(new Object[] { "B", 'B', Items.bone, });

        Salvage.addSalvage(
                ToolListMF.pickStone,
                new ItemStack(ComponentListMF.sharp_rock, 2),
                new ItemStack(Items.stick, 2),
                ComponentListMF.vine);
        Salvage.addSalvage(
                ToolListMF.axeStone,
                new ItemStack(ComponentListMF.sharp_rock, 2),
                new ItemStack(Items.stick, 2),
                ComponentListMF.vine);
        Salvage.addSalvage(
                ToolListMF.spadeStone,
                ComponentListMF.sharp_rock,
                new ItemStack(Items.stick, 2),
                ComponentListMF.vine);
        Salvage.addSalvage(
                ToolListMF.hoeStone,
                ComponentListMF.sharp_rock,
                new ItemStack(Items.stick, 2),
                ComponentListMF.vine);
        Salvage.addSalvage(
                ToolListMF.swordStone,
                Items.stick,
                new ItemStack(ComponentListMF.sharp_rock, 2),
                ComponentListMF.vine);
        Salvage.addSalvage(
                ToolListMF.waraxeStone,
                new ItemStack(ComponentListMF.sharp_rock, 2),
                new ItemStack(Items.stick, 2),
                new ItemStack(ComponentListMF.vine, 2));
        Salvage.addSalvage(
                ToolListMF.maceStone,
                new ItemStack(ComponentListMF.sharp_rock, 2),
                new ItemStack(Items.stick, 2),
                ComponentListMF.vine);
        Salvage.addSalvage(
                ToolListMF.spearStone,
                ComponentListMF.sharp_rock,
                new ItemStack(Items.stick, 2),
                ComponentListMF.vine);
        Salvage.addSalvage(ToolListMF.knifeStone, ComponentListMF.sharp_rock, Items.stick, ComponentListMF.vine);
        Salvage.addSalvage(ToolListMF.hammerStone, ComponentListMF.sharp_rock, Items.stick, ComponentListMF.vine);
        Salvage.addSalvage(ToolListMF.tongsStone, ComponentListMF.sharp_rock, Items.stick, ComponentListMF.vine);
        Salvage.addSalvage(ToolListMF.needleBone, Items.bone);
    }

    public static void initTierWood() {
        String basic = CarpenterRecipes.basic;

        float time = 4;
        Item plank = ComponentListMF.plank;

        KnowledgeListMF.spoonR = MineFantasyAPI.carpenterRecipe(CustomToolListMF.standard_spoon).skill(artisanry)
                .tool("hands", -1).time(1 + (int) (1 * time)).sound(basic).materialTiers()
                .shaped(new Object[] { "W", "S", 'W', plank, 'S', Items.stick });
        Salvage.addSalvage(CustomToolListMF.standard_spoon, plank, Items.stick);
        KnowledgeListMF.malletR = MineFantasyAPI.carpenterRecipe(CustomToolListMF.standard_mallet).skill(artisanry)
                .tool("hands", -1).time(1 + (int) (2 * time)).sound(basic).materialTiers()
                .shaped(new Object[] { "WW", " S", 'W', plank, 'S', Items.stick });
        Salvage.addSalvage(CustomToolListMF.standard_mallet, new ItemStack(plank, 2), Items.stick);
        Salvage.addSalvage(CustomToolListMF.standard_spoon, plank, Items.stick);

        KnowledgeListMF.refinedPlankR.add(
                MineFantasyAPI.carpenterRecipe(ComponentListMF.plank.construct("RefinedWood")).skill(construction)
                        .tool("hands", -1).time(1).sound(basic).shaped(
                                new Object[] { "O", "P", 'O', ComponentListMF.plant_oil, 'P',
                                        (ComponentListMF.plank) }));
        try (NativeRecipes.Variant v = NativeRecipes.variant("paint_brush")) {
            KnowledgeListMF.easyPaintPlank.add(
                    MineFantasyAPI.carpenterRecipe(ComponentListMF.plank.construct("RefinedWood", 4))
                            .skill(construction).research("paint_brush").tool("brush", -1).time(2).sound(sewing).shaped(
                                    new Object[] { " O  ", "PPPP", 'O', ComponentListMF.plant_oil, 'P',
                                            (ComponentListMF.plank) }));
        }
    }

    static void addSawPlanks(ItemStack planks, CustomMaterial material) {
        MineFantasyAPI.carpenterRecipe((ComponentListMF.plank).construct(material.name, 4)).skill(construction)
                .research("commodities").tool("saw", -1).time(10).sound(sawing)
                .shaped(new Object[] { "P", 'P', planks.copy() });
    }
}
