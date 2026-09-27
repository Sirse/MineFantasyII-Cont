package minefantasy.mf2.recipe;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.MineFantasyAPI;
import minefantasy.mf2.api.crafting.Salvage;
import minefantasy.mf2.api.rpg.Skill;
import minefantasy.mf2.api.rpg.SkillList;
import minefantasy.mf2.item.list.ArmourListMF;
import minefantasy.mf2.item.list.ComponentListMF;
import minefantasy.mf2.item.list.CustomArmourListMF;
import minefantasy.mf2.knowledge.KnowledgeListMF;

public class ForgedArmourRecipes {

    private static final Skill artisanry = SkillList.artisanry;
    private static final Skill engineering = SkillList.engineering;
    private static final Skill construction = SkillList.construction;

    public static void init() {
        addMetalComponents();
        assembleChainmail();
        assembleScalemail();
        assembleSplintmail();
        assembleFieldplate();
        assembleCogPlating();
    }

    private static void assembleChainmail() {
        Item helm = ArmourListMF.armourItem(ArmourListMF.leather, 2, 0);
        Item chest = ArmourListMF.armourItem(ArmourListMF.leather, 2, 1);
        Item legs = ArmourListMF.armourItem(ArmourListMF.leather, 2, 2);
        Item boots = ArmourListMF.armourItem(ArmourListMF.leather, 2, 3);

        Item mail = ComponentListMF.chainmesh;
        Item rivet = ComponentListMF.rivet;

        int time = 20;
        KnowledgeListMF.mailHelmetR.add(
                MineFantasyAPI.anvilRecipe(new ItemStack(CustomArmourListMF.standard_chain_helmet)).skill(artisanry)
                        .research("craftArmourMedium").hot().tool("hammer", -1).time(time).materialTiers().shaped(
                                new Object[] { "RMR", "MPM", "RMR",

                                        'R', rivet, 'M', mail, 'P', new ItemStack(helm, 1, 0) }));
        time = 30;
        KnowledgeListMF.mailChestR.add(
                MineFantasyAPI.anvilRecipe(new ItemStack(CustomArmourListMF.standard_chain_chest)).skill(artisanry)
                        .research("craftArmourMedium").hot().tool("hammer", -1).time(time).materialTiers().shaped(
                                new Object[] { "RM MR", "RMPMR", "RM MR",

                                        'R', rivet, 'M', mail, 'P', new ItemStack(chest, 1, 0) }));
        time = 20;
        KnowledgeListMF.mailLegsR.add(
                MineFantasyAPI.anvilRecipe(new ItemStack(CustomArmourListMF.standard_chain_legs)).skill(artisanry)
                        .research("craftArmourMedium").hot().tool("hammer", -1).time(time).materialTiers().shaped(
                                new Object[] { "RMPMR", "RM MR",

                                        'R', rivet, 'M', mail, 'P', new ItemStack(legs, 1, 0) }));
        time = 10;
        KnowledgeListMF.mailBootsR.add(
                MineFantasyAPI.anvilRecipe(new ItemStack(CustomArmourListMF.standard_chain_boots)).skill(artisanry)
                        .research("craftArmourMedium").hot().tool("hammer", -1).time(time).materialTiers().shaped(
                                new Object[] { "R R", "MPM",

                                        'R', rivet, 'M', mail, 'P', new ItemStack(boots, 1, 0) }));
        Salvage.addSalvage(
                CustomArmourListMF.standard_chain_helmet,
                helm,
                new ItemStack(mail, 4),
                new ItemStack(rivet, 4));
        Salvage.addSalvage(
                CustomArmourListMF.standard_chain_chest,
                chest,
                new ItemStack(mail, 6),
                new ItemStack(rivet, 6));
        Salvage.addSalvage(
                CustomArmourListMF.standard_chain_legs,
                legs,
                new ItemStack(mail, 4),
                new ItemStack(rivet, 4));
        Salvage.addSalvage(
                CustomArmourListMF.standard_chain_boots,
                boots,
                new ItemStack(mail, 2),
                new ItemStack(rivet, 2));
    }

    private static void assembleScalemail() {
        Item helm = ArmourListMF.armourItem(ArmourListMF.leather, 2, 0);
        Item chest = ArmourListMF.armourItem(ArmourListMF.leather, 2, 1);
        Item legs = ArmourListMF.armourItem(ArmourListMF.leather, 2, 2);
        Item boots = ArmourListMF.armourItem(ArmourListMF.leather, 2, 3);

        ItemStack mail = new ItemStack(ComponentListMF.scalemesh);
        Item rivet = ComponentListMF.rivet;

        int time = 20;
        KnowledgeListMF.scaleHelmetR.add(
                MineFantasyAPI.anvilRecipe(new ItemStack(CustomArmourListMF.standard_scale_helmet)).skill(artisanry)
                        .research("craftArmourMedium").hot().tool("hammer", -1).time(time).materialTiers().shaped(
                                new Object[] { "RMR", "MPM", "RMR",

                                        'R', rivet, 'M', mail, 'P', new ItemStack(helm, 1, 0) }));
        time = 30;
        KnowledgeListMF.scaleChestR.add(
                MineFantasyAPI.anvilRecipe(new ItemStack(CustomArmourListMF.standard_scale_chest)).skill(artisanry)
                        .research("craftArmourMedium").hot().tool("hammer", -1).time(time).materialTiers().shaped(
                                new Object[] { "RM MR", "RMPMR", "RM MR",

                                        'R', rivet, 'M', mail, 'P', new ItemStack(chest, 1, 0) }));
        time = 20;
        KnowledgeListMF.scaleLegsR.add(
                MineFantasyAPI.anvilRecipe(new ItemStack(CustomArmourListMF.standard_scale_legs)).skill(artisanry)
                        .research("craftArmourMedium").hot().tool("hammer", -1).time(time).materialTiers().shaped(
                                new Object[] { "RMPMR", "RM MR",

                                        'R', rivet, 'M', mail, 'P', new ItemStack(legs, 1, 0) }));
        time = 10;
        KnowledgeListMF.scaleBootsR.add(
                MineFantasyAPI.anvilRecipe(new ItemStack(CustomArmourListMF.standard_scale_boots)).skill(artisanry)
                        .research("craftArmourMedium").hot().tool("hammer", -1).time(time).materialTiers().shaped(
                                new Object[] { "R R", "MPM",

                                        'R', rivet, 'M', mail, 'P', new ItemStack(boots, 1, 0) }));
        Salvage.addSalvage(
                CustomArmourListMF.standard_scale_helmet,
                helm,
                new ItemStack(mail.getItem(), 4, mail.getItemDamage()),
                new ItemStack(rivet, 4));
        Salvage.addSalvage(
                CustomArmourListMF.standard_scale_chest,
                chest,
                new ItemStack(mail.getItem(), 6, mail.getItemDamage()),
                new ItemStack(rivet, 6));
        Salvage.addSalvage(
                CustomArmourListMF.standard_scale_legs,
                legs,
                new ItemStack(mail.getItem(), 4, mail.getItemDamage()),
                new ItemStack(rivet, 4));
        Salvage.addSalvage(
                CustomArmourListMF.standard_scale_boots,
                boots,
                new ItemStack(mail.getItem(), 2, mail.getItemDamage()),
                new ItemStack(rivet, 2));
    }

    private static void assembleSplintmail() {
        Item helm = ArmourListMF.armourItem(ArmourListMF.leather, 4, 0);
        Item chest = ArmourListMF.armourItem(ArmourListMF.leather, 4, 1);
        Item legs = ArmourListMF.armourItem(ArmourListMF.leather, 4, 2);
        Item boots = ArmourListMF.armourItem(ArmourListMF.leather, 4, 3);

        ItemStack mail = new ItemStack(ComponentListMF.splintmesh);
        Item rivet = ComponentListMF.rivet;

        int time = 20;
        KnowledgeListMF.splintHelmetR.add(
                MineFantasyAPI.anvilRecipe(new ItemStack(CustomArmourListMF.standard_splint_helmet)).skill(artisanry)
                        .research("craftArmourMedium").hot().tool("hammer", -1).time(time).materialTiers().shaped(
                                new Object[] { "RMR", "MPM", "RMR",

                                        'R', rivet, 'M', mail, 'P', new ItemStack(helm, 1, 0) }));
        time = 30;
        KnowledgeListMF.splintChestR.add(
                MineFantasyAPI.anvilRecipe(new ItemStack(CustomArmourListMF.standard_splint_chest)).skill(artisanry)
                        .research("craftArmourMedium").hot().tool("hammer", -1).time(time).materialTiers().shaped(
                                new Object[] { "RM MR", "RMPMR", "RM MR",

                                        'R', rivet, 'M', mail, 'P', new ItemStack(chest, 1, 0) }));
        time = 20;
        KnowledgeListMF.splintLegsR.add(
                MineFantasyAPI.anvilRecipe(new ItemStack(CustomArmourListMF.standard_splint_legs)).skill(artisanry)
                        .research("craftArmourMedium").hot().tool("hammer", -1).time(time).materialTiers().shaped(
                                new Object[] { "RMPMR", "RM MR",

                                        'R', rivet, 'M', mail, 'P', new ItemStack(legs, 1, 0) }));
        time = 10;
        KnowledgeListMF.splintBootsR.add(
                MineFantasyAPI.anvilRecipe(new ItemStack(CustomArmourListMF.standard_splint_boots)).skill(artisanry)
                        .research("craftArmourMedium").hot().tool("hammer", -1).time(time).materialTiers().shaped(
                                new Object[] { "R R", "MPM",

                                        'R', rivet, 'M', mail, 'P', new ItemStack(boots, 1, 0) }));
        Salvage.addSalvage(
                CustomArmourListMF.standard_splint_helmet,
                helm,
                new ItemStack(mail.getItem(), 4, mail.getItemDamage()),
                new ItemStack(rivet, 4));
        Salvage.addSalvage(
                CustomArmourListMF.standard_splint_chest,
                chest,
                new ItemStack(mail.getItem(), 6, mail.getItemDamage()),
                new ItemStack(rivet, 6));
        Salvage.addSalvage(
                CustomArmourListMF.standard_splint_legs,
                legs,
                new ItemStack(mail.getItem(), 4, mail.getItemDamage()),
                new ItemStack(rivet, 4));
        Salvage.addSalvage(
                CustomArmourListMF.standard_splint_boots,
                boots,
                new ItemStack(mail.getItem(), 2, mail.getItemDamage()),
                new ItemStack(rivet, 2));
    }

    private static void assembleFieldplate() {
        Item helm = ArmourListMF.armourItem(ArmourListMF.leather, 4, 0);
        Item chest = ArmourListMF.armourItem(ArmourListMF.leather, 4, 1);
        Item legs = ArmourListMF.armourItem(ArmourListMF.leather, 4, 2);
        Item boots = ArmourListMF.armourItem(ArmourListMF.leather, 4, 3);

        ItemStack plate = new ItemStack(ComponentListMF.plate);
        Item rivet = ComponentListMF.rivet;

        int time = 40;
        KnowledgeListMF.plateHelmetR.add(
                MineFantasyAPI.anvilRecipe(new ItemStack(CustomArmourListMF.standard_plate_helmet)).skill(artisanry)
                        .research("craftArmourHeavy").tool("hammer", -1).time(time).materialTiers().shaped(
                                new Object[] { " R ", "PHP", " R ",

                                        'R', rivet, 'P', plate, 'H', new ItemStack(helm, 1, 0), }));
        time = 60;
        KnowledgeListMF.plateChestR.add(
                MineFantasyAPI.anvilRecipe(new ItemStack(CustomArmourListMF.standard_plate_chest)).skill(artisanry)
                        .research("craftArmourHeavy").tool("hammer", -1).time(time).materialTiers().shaped(
                                new Object[] { "RP PR", "RPCPR",

                                        'R', rivet, 'P', plate, 'C', new ItemStack(chest, 1, 0), }));
        time = 40;
        KnowledgeListMF.plateLegsR.add(
                MineFantasyAPI.anvilRecipe(new ItemStack(CustomArmourListMF.standard_plate_legs)).skill(artisanry)
                        .research("craftArmourHeavy").tool("hammer", -1).time(time).materialTiers().shaped(
                                new Object[] { "RPLPR", "RP PR",

                                        'R', rivet, 'P', plate, 'L', new ItemStack(legs, 1, 0), }));
        time = 20;
        KnowledgeListMF.plateBootsR.add(
                MineFantasyAPI.anvilRecipe(new ItemStack(CustomArmourListMF.standard_plate_boots)).skill(artisanry)
                        .research("craftArmourHeavy").tool("hammer", -1).time(time).materialTiers().shaped(
                                new Object[] { "R R", "PBP",

                                        'R', rivet, 'P', plate, 'B', new ItemStack(boots, 1, 0), }));

        Salvage.addSalvage(
                CustomArmourListMF.standard_plate_helmet,
                helm,
                new ItemStack(plate.getItem(), 2, plate.getItemDamage()),
                new ItemStack(rivet, 2));
        Salvage.addSalvage(
                CustomArmourListMF.standard_plate_chest,
                chest,
                new ItemStack(plate.getItem(), 4, plate.getItemDamage()),
                new ItemStack(rivet, 4));
        Salvage.addSalvage(
                CustomArmourListMF.standard_plate_legs,
                legs,
                new ItemStack(plate.getItem(), 4, plate.getItemDamage()),
                new ItemStack(rivet, 4));
        Salvage.addSalvage(
                CustomArmourListMF.standard_plate_boots,
                boots,
                new ItemStack(plate.getItem(), 2, plate.getItemDamage()),
                new ItemStack(rivet, 2));

    }

    private static void assembleCogPlating() {

        ItemStack minorPiece = new ItemStack(ComponentListMF.plate);
        ItemStack majorPiece = new ItemStack(ComponentListMF.plate_huge);

        int time = 4;
        KnowledgeListMF.hugePlateR.add(
                MineFantasyAPI.anvilRecipe(majorPiece).skill(engineering).research("cogArmour").hot()
                        .tool("hvyhammer", -1).time(time).materialTiers()
                        .shaped(new Object[] { " RR ", "RIIR", 'R', ComponentListMF.rivet, 'I', minorPiece }));

        Salvage.addSalvage(
                majorPiece,
                new ItemStack(minorPiece.getItem(), 2, minorPiece.getItemDamage()),
                new ItemStack(ComponentListMF.rivet, 4));

        time = 25;
        KnowledgeListMF.cogPlateR.add(
                MineFantasyAPI.anvilRecipe(new ItemStack(ComponentListMF.cogwork_armour)).skill(engineering)
                        .research("cogArmour").hot().tool("hvyhammer", -1).time(time).materialTiers().shaped(
                                new Object[] { "  P  ", "pPPPp", "p P p", " pPp ",

                                        'p', minorPiece, 'P', majorPiece, }));

        Salvage.addSalvage(
                ComponentListMF.cogwork_armour,
                new ItemStack(minorPiece.getItem(), 6, minorPiece.getItemDamage()),
                new ItemStack(majorPiece.getItem(), 6, majorPiece.getItemDamage()));
    }

    private static void addMetalComponents() {
        Item hunk = ComponentListMF.metalHunk;
        Item bar = ComponentListMF.bar;

        int time = 3;
        KnowledgeListMF.mailRecipes.add(
                MineFantasyAPI.anvilRecipe(new ItemStack(ComponentListMF.chainmesh)).skill(artisanry).hot()
                        .tool("hammer", -1).time(time).materialTiers().shaped(
                                new Object[] { " H ", "H H", " H ",

                                        'H', hunk }));
        time = 3;
        KnowledgeListMF.scaleRecipes.add(
                MineFantasyAPI.anvilRecipe(new ItemStack(ComponentListMF.scalemesh)).skill(artisanry).hot()
                        .tool("hammer", -1).time(time).materialTiers().shaped(
                                new Object[] { "HHH", " H ",

                                        'H', hunk }));
        time = 4;
        KnowledgeListMF.splintRecipes.add(
                MineFantasyAPI.anvilRecipe(new ItemStack(ComponentListMF.splintmesh)).skill(artisanry).hot()
                        .tool("hammer", -1).time(time).materialTiers().shaped(
                                new Object[] { "RHR", " H ", " H ", " H ",

                                        'H', hunk, 'R', ComponentListMF.rivet, }));
        time = 4;
        KnowledgeListMF.plateRecipes.add(
                MineFantasyAPI.anvilRecipe(ComponentListMF.plate).skill(artisanry).hot().tool("hvyhammer", -1)
                        .time(time).materialTiers()
                        .shaped(new Object[] { "FF", "II", 'F', ComponentListMF.flux, 'I', bar }));

        Salvage.addSalvage(ComponentListMF.chainmesh, hunk);
        Salvage.addSalvage(ComponentListMF.scalemesh, hunk);
        Salvage.addSalvage(ComponentListMF.splintmesh, hunk, new ItemStack(ComponentListMF.rivet, 2));
        Salvage.addSalvage(ComponentListMF.plate, new ItemStack(bar, 2));
    }
}
