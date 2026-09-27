package minefantasy.mf2.recipe;

import java.util.ArrayList;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import minefantasy.mf2.api.MineFantasyAPI;
import minefantasy.mf2.api.crafting.Salvage;
import minefantasy.mf2.api.material.CustomMaterial;
import minefantasy.mf2.api.rpg.Skill;
import minefantasy.mf2.api.rpg.SkillList;
import minefantasy.mf2.item.list.ComponentListMF;
import minefantasy.mf2.item.list.CustomToolListMF;
import minefantasy.mf2.item.list.ToolListMF;
import minefantasy.mf2.knowledge.KnowledgeListMF;

public class ForgedToolRecipes {

    private static final Skill artisanry = SkillList.artisanry;
    private static final Skill engineering = SkillList.engineering;
    private static final Skill construction = SkillList.construction;

    public static void init() {
        CarpenterRecipes.initTierWood();
        addStandardTools();
        addStandardCrafters();
        addStandardWeapons();
        addComponentTools();
        addMetalComponents();

        ArrayList<CustomMaterial> metal = CustomMaterial.getList("metal");
        for (CustomMaterial customMat : metal) {
            ItemStack bar = ComponentListMF.bar.createComm(customMat.name);
            for (ItemStack ingot : OreDictionary.getOres("ingot" + customMat.name)) {
                KnowledgeListMF.barR.add(
                        MineFantasyAPI.anvilRecipe(bar).hot().tool("hammer", -1)
                                .time((int) (customMat.craftTimeModifier / 2F))
                                .shaped(new Object[] { "I", 'I', ingot, }));
            }

            ItemStack defaultIngot = customMat.getItem();
            if (defaultIngot != null) {
                KnowledgeListMF.baringotR.add(
                        MineFantasyAPI.anvilRecipe(defaultIngot).hot().tool("hammer", -1)
                                .time((int) (customMat.craftTimeModifier / 2F))
                                .shaped(new Object[] { "I", 'I', bar, }));
            }
        }

        KnowledgeListMF.tinderboxR = MineFantasyAPI.anvilRecipe(new ItemStack(ToolListMF.tinderbox)).hot()
                .tool("hammer", 0).stationTier(0).time(10).shaped(
                        new Object[] { " F ", "SWS", " I ", 'F', Items.flint, 'S', Items.stick, 'W', Blocks.wool, 'I',
                                ComponentListMF.bar("Iron"), });
        KnowledgeListMF.flintAndSteelR = MineFantasyAPI.anvilRecipe(new ItemStack(Items.flint_and_steel)).hot()
                .tool("hammer", 0).stationTier(0).time(10).shaped(
                        new Object[] { "  F", "IC ", " I ", 'F', Items.flint, 'C', Items.coal, 'I',
                                ComponentListMF.bar("Steel"), });
        Salvage.addSalvage(ToolListMF.tinderbox, Items.flint, Items.stick, Blocks.wool, ComponentListMF.bar("Iron"));
        Salvage.addSalvage(Items.flint_and_steel, Items.flint, ComponentListMF.bar("Steel"));
    }

    private static void addMetalComponents() {
        int time = 2;
        Item bar = ComponentListMF.bar;
        Item hunk = ComponentListMF.metalHunk;

        KnowledgeListMF.hunkR = MineFantasyAPI.anvilRecipe(new ItemStack(hunk, 4)).skill(artisanry).hot()
                .tool("hammer", 0).stationTier(0).time(time).materialTiers()
                .shaped(new Object[] { "F", "I", 'F', ComponentListMF.flux, 'I', bar, });

        KnowledgeListMF.ingotR = MineFantasyAPI.anvilRecipe(bar).skill(artisanry).hot().tool("hammer", 0).stationTier(0)
                .time(time).materialTiers().shaped(new Object[] { "II", "II", 'I', hunk });

        time = 8;
        int count = 1;
        KnowledgeListMF.bucketR = MineFantasyAPI.anvilRecipe(new ItemStack(Items.bucket, count)).skill(artisanry).hot()
                .tool("hammer", 0).stationTier(0).time(time).shaped(new Object[] { "I I", " I ", 'I', bar, });
    }

    private static void addComponentTools() {
        Item bar = ComponentListMF.bar;
        Item plank = ComponentListMF.plank;
        Item strip = ComponentListMF.leather_strip;
        Item rivet = ComponentListMF.rivet;
        Item hunk = ComponentListMF.metalHunk;

        int time = 10;

        KnowledgeListMF.nailR = MineFantasyAPI.anvilRecipe(new ItemStack(ComponentListMF.nail, 16)).skill(artisanry)
                .hot().tool("hammer", -1).time(time).shaped(
                        new Object[] { "HH", " H", " H",

                                'H', hunk });
        KnowledgeListMF.rivetR = MineFantasyAPI.anvilRecipe(new ItemStack(ComponentListMF.rivet, 8)).skill(artisanry)
                .hot().tool("hammer", -1).time(time).shaped(
                        new Object[] { "H H", " H ", " H ",

                                'H', hunk });

        KnowledgeListMF.needleR = MineFantasyAPI.anvilRecipe(CustomToolListMF.standard_needle).skill(artisanry)
                .research("tier").hot().tool("hammer", -1).time(time).materialTiers().shaped(
                        new Object[] { "H", "H", "H", "H",

                                'H', hunk });
        Salvage.addSalvage(CustomToolListMF.standard_needle, bar);

        time = 3;
        KnowledgeListMF.crossBoltR = MineFantasyAPI.anvilRecipe(CustomToolListMF.standard_bolt).skill(artisanry)
                .research("tier").hot().tool("hammer", -1).time(time).materialTiers().shaped(
                        new Object[] { "H", "F",

                                'F', ComponentListMF.fletching, 'H', hunk });
        time = 2;
        KnowledgeListMF.arrowheadR = MineFantasyAPI.anvilRecipe(new ItemStack(ComponentListMF.arrowhead, 4))
                .skill(artisanry).research("tier").hot().tool("hammer", -1).time(time).materialTiers().shaped(
                        new Object[] { "H ", "HH", "H ",

                                'H', hunk });
        time = 5;
        KnowledgeListMF.bodkinheadR = MineFantasyAPI.anvilRecipe(new ItemStack(ComponentListMF.bodkinhead, 4))
                .skill(artisanry).research("tier").hot().tool("hammer", -1).time(time).materialTiers().shaped(
                        new Object[] { "H  ", " HH", "H  ",

                                'H', hunk });
        time = 5;
        KnowledgeListMF.broadheadR = MineFantasyAPI.anvilRecipe(new ItemStack(ComponentListMF.broadhead, 4))
                .skill(artisanry).research("tier").hot().tool("hammer", -1).time(time).materialTiers().shaped(
                        new Object[] { "H ", " H", " H", "H ",

                                'H', hunk });
        Salvage.addSalvage(CustomToolListMF.standard_bolt, ComponentListMF.fletching, hunk);
        Salvage.addSalvage(ComponentListMF.arrowhead, hunk);
        Salvage.addSalvage(ComponentListMF.bodkinhead, hunk);
        Salvage.addSalvage(ComponentListMF.broadhead, hunk);

        time = 1;
        KnowledgeListMF.arrowR.add(
                MineFantasyAPI.carpenterRecipe(CustomToolListMF.standard_arrow).skill(artisanry).research("arrows")
                        .time(1).sound("dig.wood").materialTiers().shaped(
                                new Object[] { "H", "F",

                                        'F', ComponentListMF.fletching, 'H', ComponentListMF.arrowhead }));
        KnowledgeListMF.arrowR.add(
                MineFantasyAPI.carpenterRecipe(CustomToolListMF.standard_arrow_bodkin).skill(artisanry)
                        .research("arrowsBodkin").time(1).sound("dig.wood").materialTiers().shaped(
                                new Object[] { "H", "F",

                                        'F', ComponentListMF.fletching, 'H', ComponentListMF.bodkinhead }));
        KnowledgeListMF.arrowR.add(
                MineFantasyAPI.carpenterRecipe(CustomToolListMF.standard_arrow_broad).skill(artisanry)
                        .research("arrowsBroad").time(1).sound("dig.wood").materialTiers().shaped(
                                new Object[] { "H", "F",

                                        'F', ComponentListMF.fletching, 'H', ComponentListMF.broadhead }));
        Salvage.addSalvage(CustomToolListMF.standard_arrow, ComponentListMF.arrowhead, ComponentListMF.fletching);
        Salvage.addSalvage(
                CustomToolListMF.standard_arrow_bodkin,
                ComponentListMF.bodkinhead,
                ComponentListMF.fletching);
        Salvage.addSalvage(CustomToolListMF.standard_arrow_broad, ComponentListMF.broadhead, ComponentListMF.fletching);

    }

    private static void addStandardTools() {
        Item bar = ComponentListMF.bar;
        Item plank = ComponentListMF.plank;
        Item strip = ComponentListMF.leather_strip;
        Item rivet = ComponentListMF.rivet;

        int time = 15;
        KnowledgeListMF.pickR = MineFantasyAPI.anvilRecipe(CustomToolListMF.standard_pick).skill(artisanry)
                .research("tier").hot().tool("hammer", -1).time(time).materialTiers().shaped(
                        new Object[] { "L I", "PPI", "L I",

                                'I', bar, 'P', plank, 'L', strip, });
        Salvage.addSalvage(CustomToolListMF.standard_pick, stack(bar, 3), stack(plank, 2), stack(strip, 2));

        time = 15;
        KnowledgeListMF.axeR = MineFantasyAPI.anvilRecipe(CustomToolListMF.standard_axe).skill(artisanry)
                .research("tier").hot().tool("hammer", -1).time(time).materialTiers().shaped(
                        new Object[] { "LII", "PPI", "L  ",

                                'I', bar, 'P', plank, 'L', strip, });
        Salvage.addSalvage(CustomToolListMF.standard_axe, stack(bar, 3), stack(plank, 2), stack(strip, 2));

        time = 12;
        KnowledgeListMF.hoeR = MineFantasyAPI.anvilRecipe(CustomToolListMF.standard_hoe).skill(artisanry)
                .research("tier").hot().tool("hammer", -1).time(time).materialTiers().shaped(
                        new Object[] { "L I", "PPI", "L  ",

                                'I', bar, 'P', plank, 'L', strip, });
        Salvage.addSalvage(CustomToolListMF.standard_hoe, stack(bar, 2), stack(plank, 2), stack(strip, 2));

        time = 10;
        KnowledgeListMF.spadeR = MineFantasyAPI.anvilRecipe(CustomToolListMF.standard_spade).skill(artisanry)
                .research("tier").hot().tool("hammer", -1).time(time).materialTiers().shaped(
                        new Object[] { "L  ", "PPI", "L  ",

                                'I', bar, 'P', plank, 'L', strip, });
        Salvage.addSalvage(CustomToolListMF.standard_spade, stack(bar, 1), stack(plank, 2), stack(strip, 2));

        // ADVANCED
        time = 25;
        KnowledgeListMF.hvyPickR = MineFantasyAPI.anvilRecipe(CustomToolListMF.standard_hvypick).skill(artisanry)
                .research("tier").hot().tool("hvyhammer", -1).time(time).materialTiers().shaped(
                        new Object[] { "LR I", "PPII", "LRII",

                                'R', rivet, 'I', bar, 'P', plank, 'L', strip, });
        Salvage.addSalvage(
                CustomToolListMF.standard_hvypick,
                stack(bar, 5),
                stack(plank, 2),
                stack(strip, 2),
                stack(rivet, 2));

        time = 15;
        KnowledgeListMF.handpickR = MineFantasyAPI.anvilRecipe(CustomToolListMF.standard_handpick).skill(artisanry)
                .research("tier").hot().tool("hammer", -1).time(time).materialTiers().shaped(
                        new Object[] { "LI ", "PIR", "L  ",

                                'R', rivet, 'I', bar, 'P', plank, 'L', strip, });
        Salvage.addSalvage(
                CustomToolListMF.standard_handpick,
                stack(bar, 2),
                stack(plank, 1),
                stack(strip, 2),
                stack(rivet, 1));

        time = 25;
        KnowledgeListMF.hvyShovelR = MineFantasyAPI.anvilRecipe(CustomToolListMF.standard_hvyshovel).skill(artisanry)
                .research("tier").hot().tool("hvyhammer", -1).time(time).materialTiers().shaped(
                        new Object[] { "LRII", "PPII", "LRII",

                                'R', rivet, 'I', bar, 'P', plank, 'L', strip, });
        Salvage.addSalvage(
                CustomToolListMF.standard_hvyshovel,
                stack(bar, 6),
                stack(plank, 2),
                stack(strip, 2),
                stack(rivet, 2));

        time = 15;
        KnowledgeListMF.trowR = MineFantasyAPI.anvilRecipe(CustomToolListMF.standard_trow).skill(artisanry)
                .research("tier").hot().tool("hammer", -1).time(time).materialTiers().shaped(
                        new Object[] { "L  ", "PIR", "L  ",

                                'R', rivet, 'I', bar, 'P', plank, 'L', strip, });
        Salvage.addSalvage(
                CustomToolListMF.standard_trow,
                stack(bar, 1),
                stack(plank, 1),
                stack(strip, 2),
                stack(rivet, 1));

        time = 30;
        KnowledgeListMF.scytheR = MineFantasyAPI.anvilRecipe(CustomToolListMF.standard_scythe).skill(artisanry)
                .research("tier").hot().tool("hvyhammer", -1).time(time).materialTiers().shaped(
                        new Object[] { "   I ", "L PIR", "PPPIR",

                                'R', rivet, 'I', bar, 'P', plank, 'L', strip, });
        Salvage.addSalvage(
                CustomToolListMF.standard_scythe,
                stack(bar, 3),
                stack(plank, 4),
                stack(strip, 1),
                stack(rivet, 2));

        time = 14;
        KnowledgeListMF.mattockR = MineFantasyAPI.anvilRecipe(CustomToolListMF.standard_mattock).skill(artisanry)
                .research("tier").hot().tool("hammer", -1).time(time).materialTiers().shaped(
                        new Object[] { "L I", "PPI", "LIR",

                                'I', bar, 'P', plank, 'L', strip, 'R', rivet });
        Salvage.addSalvage(
                CustomToolListMF.standard_mattock,
                stack(bar, 3),
                stack(rivet, 1),
                stack(plank, 2),
                stack(strip, 2));

        time = 15;
        KnowledgeListMF.lumberR = MineFantasyAPI.anvilRecipe(CustomToolListMF.standard_lumber).skill(artisanry)
                .research("tier").hot().tool("hvyHammer", -1).time(time).materialTiers().shaped(
                        new Object[] { "L IIR", "PPPIR", "L   R",

                                'I', bar, 'P', plank, 'L', strip, 'R', rivet });
        Salvage.addSalvage(
                CustomToolListMF.standard_lumber,
                stack(bar, 3),
                stack(rivet, 3),
                stack(plank, 3),
                stack(strip, 2));
    }

    private static void addStandardCrafters() {
        Item bar = ComponentListMF.bar;
        Item plank = ComponentListMF.plank;
        Item strip = ComponentListMF.leather_strip;
        Item rivet = ComponentListMF.rivet;

        int time = 10;
        KnowledgeListMF.hammerR = MineFantasyAPI.anvilRecipe(CustomToolListMF.standard_hammer).skill(artisanry)
                .research("tier").hot().tool("hammer", 0).stationTier(0).time(time).materialTiers().shaped(
                        new Object[] { "I", "L", "P",

                                'I', bar, 'P', plank, 'L', strip, });
        Salvage.addSalvage(CustomToolListMF.standard_hammer, bar, plank, strip);

        time = 15;
        KnowledgeListMF.hvyHammerR = MineFantasyAPI.anvilRecipe(CustomToolListMF.standard_hvyhammer).skill(artisanry)
                .research("tier").hot().tool("hammer", -1).time(time).materialTiers().shaped(
                        new Object[] { " II", "RLI", " P ",

                                'R', rivet, 'I', bar, 'P', plank, 'L', strip, });
        Salvage.addSalvage(
                CustomToolListMF.standard_hvyhammer,
                stack(bar, 3),
                stack(plank, 1),
                stack(strip, 1),
                stack(rivet, 1));

        time = 10;
        KnowledgeListMF.tongsR = MineFantasyAPI.anvilRecipe(CustomToolListMF.standard_tongs).skill(artisanry)
                .research("tier").hot().tool("hammer", -1).time(time).materialTiers().shaped(
                        new Object[] { "I ", " I",

                                'I', bar, });
        Salvage.addSalvage(CustomToolListMF.standard_tongs, stack(bar, 2));

        time = 10;
        KnowledgeListMF.knifeR = MineFantasyAPI.anvilRecipe(CustomToolListMF.standard_knife).skill(artisanry)
                .research("tier").hot().tool("hammer", -1).time(time).materialTiers().shaped(
                        new Object[] { "I ", "PL",

                                'I', bar, 'P', plank, 'L', strip, });
        Salvage.addSalvage(CustomToolListMF.standard_knife, bar, plank, strip);

        time = 12;
        KnowledgeListMF.shearsR = MineFantasyAPI.anvilRecipe(CustomToolListMF.standard_shears).skill(artisanry)
                .research("tier").hot().tool("hammer", -1).time(time).materialTiers().shaped(
                        new Object[] { " I ", "PLI", " P ",

                                'I', bar, 'P', plank, 'L', Items.leather, });
        Salvage.addSalvage(CustomToolListMF.standard_shears, stack(bar, 2), stack(plank, 2), Items.leather);

        time = 20;
        KnowledgeListMF.sawsR = MineFantasyAPI.anvilRecipe(CustomToolListMF.standard_saw).skill(artisanry)
                .research("tier").hot().tool("hammer", -1).time(time).materialTiers().shaped(
                        new Object[] { "PIII", "LI  ",

                                'I', bar, 'P', plank, 'L', strip, });
        Salvage.addSalvage(CustomToolListMF.standard_saw, stack(bar, 4), stack(plank, 1), stack(strip, 1));

        time = 15;
        KnowledgeListMF.spannerR = MineFantasyAPI.anvilRecipe(CustomToolListMF.standard_spanner).skill(artisanry)
                .research("tier").hot().tool("hammer", -1).time(time).materialTiers().shaped(
                        new Object[] { "  I ", "  II", "LP  ", " L  ",

                                'I', bar, 'P', plank, 'L', strip, });
        Salvage.addSalvage(CustomToolListMF.standard_spanner, stack(bar, 3), stack(plank, 1), stack(strip, 2));
    }

    private static void addStandardWeapons() {
        Item bar = ComponentListMF.bar;
        Item plank = ComponentListMF.plank;
        Item strip = ComponentListMF.leather_strip;
        Item rivet = ComponentListMF.rivet;

        int time = 15;
        KnowledgeListMF.daggerR = MineFantasyAPI.anvilRecipe(CustomToolListMF.standard_dagger).skill(artisanry)
                .research("tier").hot().tool("hammer", -1).time(time).materialTiers().shaped(
                        new Object[] { "L  ", "PII", "L  ",

                                'I', bar, 'P', plank, 'L', strip, });
        Salvage.addSalvage(CustomToolListMF.standard_dagger, stack(bar, 2), stack(plank, 1), stack(strip, 2));

        time = 25;
        KnowledgeListMF.swordR = MineFantasyAPI.anvilRecipe(CustomToolListMF.standard_sword).skill(artisanry)
                .research("tier").hot().tool("hammer", -1).time(time).materialTiers().shaped(
                        new Object[] { "LI  ", "PIII", "LI  ",

                                'I', bar, 'P', plank, 'L', strip, });
        Salvage.addSalvage(CustomToolListMF.standard_sword, stack(bar, 5), stack(plank, 1), stack(strip, 2));

        time = 20;
        KnowledgeListMF.waraxeR = MineFantasyAPI.anvilRecipe(CustomToolListMF.standard_waraxe).skill(artisanry)
                .research("tier").hot().tool("hammer", -1).time(time).materialTiers().shaped(
                        new Object[] { "LII", "PPI", "L I",

                                'I', bar, 'P', plank, 'L', strip, });
        Salvage.addSalvage(CustomToolListMF.standard_waraxe, stack(bar, 4), stack(plank, 2), stack(strip, 2));

        KnowledgeListMF.maceR = MineFantasyAPI.anvilRecipe(CustomToolListMF.standard_mace).skill(artisanry)
                .research("tier").hot().tool("hammer", -1).time(time).materialTiers().shaped(
                        new Object[] { "L II", "PPII", "L   ",

                                'I', bar, 'P', plank, 'L', strip, });
        Salvage.addSalvage(CustomToolListMF.standard_mace, stack(bar, 4), stack(plank, 2), stack(strip, 2));

        KnowledgeListMF.spearR = MineFantasyAPI.anvilRecipe(CustomToolListMF.standard_spear).skill(artisanry)
                .research("tier").hot().tool("hammer", -1).time(time).materialTiers().shaped(
                        new Object[] { " LLI ", "PPPPI", " LLI ",

                                'I', bar, 'P', plank, 'L', strip, });
        Salvage.addSalvage(CustomToolListMF.standard_spear, stack(bar, 3), stack(plank, 4), stack(strip, 4));

        // HEAVY
        time = 30;
        KnowledgeListMF.katanaR = MineFantasyAPI.anvilRecipe(CustomToolListMF.standard_katana).skill(artisanry)
                .research("tier").hot().tool("hvyhammer", -1).time(time).materialTiers().shaped(
                        new Object[] { "LR   I", "PIIII ", "LI    ",

                                'R', rivet, 'I', bar, 'P', plank, 'L', strip, });
        Salvage.addSalvage(CustomToolListMF.standard_katana, stack(bar, 6), plank, stack(strip, 2), rivet);

        time = 40;
        KnowledgeListMF.gswordR = MineFantasyAPI.anvilRecipe(CustomToolListMF.standard_greatsword).skill(artisanry)
                .research("tier").hot().tool("hvyhammer", -1).time(time).materialTiers().shaped(
                        new Object[] { "LIR   ", "PIIIII", "LIR   ",

                                'R', rivet, 'I', bar, 'P', plank, 'L', strip, });
        Salvage.addSalvage(
                CustomToolListMF.standard_greatsword,
                stack(bar, 7),
                plank,
                stack(strip, 2),
                stack(rivet, 2));

        time = 30;
        KnowledgeListMF.battleaxeR = MineFantasyAPI.anvilRecipe(CustomToolListMF.standard_battleaxe).skill(artisanry)
                .research("tier").hot().tool("hvyhammer", -1).time(time).materialTiers().shaped(
                        new Object[] { "LLIIR", "PPPIR", "LLIIR",

                                'R', rivet, 'I', bar, 'P', plank, 'L', strip, });
        Salvage.addSalvage(
                CustomToolListMF.standard_battleaxe,
                stack(bar, 5),
                stack(plank, 3),
                stack(strip, 4),
                stack(rivet, 3));

        KnowledgeListMF.whammerR = MineFantasyAPI.anvilRecipe(CustomToolListMF.standard_warhammer).skill(artisanry)
                .research("tier").hot().tool("hvyhammer", -1).time(time).materialTiers().shaped(
                        new Object[] { "LL IIR", "PPPIIR", "LL  IR",

                                'R', rivet, 'I', bar, 'P', plank, 'L', strip, });
        Salvage.addSalvage(
                CustomToolListMF.standard_warhammer,
                stack(bar, 5),
                stack(plank, 3),
                stack(strip, 4),
                stack(rivet, 3));

        KnowledgeListMF.halbeardR = MineFantasyAPI.anvilRecipe(CustomToolListMF.standard_halbeard).skill(artisanry)
                .research("tier").hot().tool("hvyhammer", -1).time(time).materialTiers().shaped(
                        new Object[] { "LLRII", "PPPPI", "LLRI ",

                                'R', rivet, 'I', bar, 'P', plank, 'L', strip, });
        Salvage.addSalvage(
                CustomToolListMF.standard_halbeard,
                stack(bar, 4),
                stack(plank, 4),
                stack(strip, 4),
                stack(rivet, 2));

        time = 25;
        KnowledgeListMF.bowR = MineFantasyAPI.anvilRecipe(CustomToolListMF.standard_bow).skill(artisanry)
                .research("tier").hot().tool("hammer", -1).time(time).materialTiers().shaped(
                        new Object[] { "PSSSP", " PLP ",

                                'I', bar, 'S', Items.string, 'P', plank, 'L', strip, });
        Salvage.addSalvage(CustomToolListMF.standard_bow, stack(plank, 4), strip, stack(Items.string, 3));

        time = 60;
        KnowledgeListMF.lanceR = MineFantasyAPI.anvilRecipe(CustomToolListMF.standard_lance).skill(artisanry)
                .research("tier").hot().tool("hvyhammer", -1).time(time).materialTiers().shaped(
                        new Object[] { "IR    ", "IIIIII", "IR    ",

                                'R', rivet, 'I', bar, });
        Salvage.addSalvage(CustomToolListMF.standard_lance, stack(bar, 8), stack(rivet, 2));
    }

    private static ItemStack stack(Item item, int count) {
        return new ItemStack(item, count);
    }
}
