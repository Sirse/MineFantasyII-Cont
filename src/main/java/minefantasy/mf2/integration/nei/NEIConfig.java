package minefantasy.mf2.integration.nei;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import codechicken.nei.event.NEIRegisterHandlerInfosEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import minefantasy.mf2.MineFantasyII;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.config.ConfigIntegration;
import minefantasy.mf2.item.list.ComponentListMF;

@Optional.Interface(iface = "codechicken.nei.api.IConfigureNEI", modid = "NotEnoughItems")
public class NEIConfig implements IConfigureNEI {

    private static boolean registeredHandlerInfoEvents;

    @Override
    public String getName() {
        return MineFantasyII.NAME + " NEI Plugin";
    }

    @Override
    public String getVersion() {
        return MineFantasyII.VERSION;
    }

    @Override
    public void loadConfig() {
        if (ConfigIntegration.neiIntegration) {
            registerHandlerInfoEvents();

            RecipeHandlerCarpenter handlerCarpenter = new RecipeHandlerCarpenter();
            API.registerRecipeHandler(handlerCarpenter);
            API.registerUsageHandler(handlerCarpenter);

            RecipeHandlerAnvil handlerAnvil = new RecipeHandlerAnvil();
            API.registerRecipeHandler(handlerAnvil);
            API.registerUsageHandler(handlerAnvil);

            RecipeHandlerBloom handlerBloom = new RecipeHandlerBloom();
            API.registerRecipeHandler(handlerBloom);
            API.registerUsageHandler(handlerBloom);

            RecipeHandlerQuern handlerQuern = new RecipeHandlerQuern();
            API.registerRecipeHandler(handlerQuern);
            API.registerUsageHandler(handlerQuern);

            RecipeHandlerTanning handlerTanning = new RecipeHandlerTanning();
            API.registerRecipeHandler(handlerTanning);
            API.registerUsageHandler(handlerTanning);

            RecipeHandlerCooking handlerCooking = new RecipeHandlerCooking();
            API.registerRecipeHandler(handlerCooking);
            API.registerUsageHandler(handlerCooking);

            RecipeHandlerSalvage handlerSalvage = new RecipeHandlerSalvage();
            API.registerRecipeHandler(handlerSalvage);
            API.registerUsageHandler(handlerSalvage);

            RecipeHandlerPaintOil handlerPaintOil = new RecipeHandlerPaintOil();
            API.registerRecipeHandler(handlerPaintOil);
            API.registerUsageHandler(handlerPaintOil);

            RecipeHandlerCrucible handlerCrucible = new RecipeHandlerCrucible();
            API.registerRecipeHandler(handlerCrucible);
            API.registerUsageHandler(handlerCrucible);

            RecipeHandlerBigFurnace handlerBigFurnace = new RecipeHandlerBigFurnace();
            API.registerRecipeHandler(handlerBigFurnace);
            API.registerUsageHandler(handlerBigFurnace);

            RecipeHandlerBlastFurnace handlerBlastFurnace = new RecipeHandlerBlastFurnace();
            API.registerRecipeHandler(handlerBlastFurnace);
            API.registerUsageHandler(handlerBlastFurnace);

            RecipeHandlerBombBench handlerBombBench = new RecipeHandlerBombBench();
            API.registerRecipeHandler(handlerBombBench);
            API.registerUsageHandler(handlerBombBench);

            RecipeHandlerCrossbowBench handlerCrossbowBench = new RecipeHandlerCrossbowBench();
            API.registerRecipeHandler(handlerCrossbowBench);
            API.registerUsageHandler(handlerCrossbowBench);

            registerRecipeCatalysts();

            // GTNH NEI may not fire NEIRegisterHandlerInfosEvent after this plugin loads.
            registerHandlerInfos(new NEIRegisterHandlerInfosEvent());
        }
    }

    private void registerRecipeCatalysts() {
        addCatalyst(BlockListMF.carpenter, "minefantasy2.carpenter", 100);
        addCatalyst(BlockListMF.anvilStone, "minefantasy2.anvil", 100);
        if (BlockListMF.anvil != null) {
            for (int tier = 0; tier < BlockListMF.anvil.length; tier++) {
                addCatalyst(BlockListMF.anvil[tier], "minefantasy2.anvil", 110 + tier);
            }
        }
        addCatalyst(BlockListMF.bloomery, "minefantasy2.bloomery", 100);
        addCatalyst(BlockListMF.quern, "minefantasy2.quern", 100);
        addCatalyst(BlockListMF.tanner, "minefantasy2.tanning", 100);
        addCatalyst(BlockListMF.advTanner, "minefantasy2.tanning", 110);
        addCatalyst(BlockListMF.engTanner, "minefantasy2.tanning", 120);
        addCatalyst(BlockListMF.firepit, "minefantasy2.cooking", 100);
        addCatalyst(BlockListMF.oven_stone, "minefantasy2.cooking", 110);
        addCatalyst(BlockListMF.salvage_basic, "minefantasy2.salvage", 100);
        addCatalyst(ComponentListMF.plant_oil, "minefantasy2.paint_oil", 100);
        addCatalyst(BlockListMF.crucible, "minefantasy2.crucible", 100);
        addCatalyst(BlockListMF.crucibleadv, "minefantasy2.crucible", 110);
        addCatalyst(BlockListMF.crucibleauto, "minefantasy2.crucible", 120);
        addCatalyst(BlockListMF.cruciblemythic, "minefantasy2.crucible", 130);
        addCatalyst(BlockListMF.cruciblemaster, "minefantasy2.crucible", 140);
        addCatalyst(BlockListMF.furnace_stone, "minefantasy2.big_furnace", 100);
        addCatalyst(BlockListMF.blast_chamber, "minefantasy2.blast_furnace", 100);
        addCatalyst(BlockListMF.bombBench, "minefantasy2.bomb_bench", 100);
        addCatalyst(BlockListMF.crossbowBench, "minefantasy2.crossbow_bench", 100);
    }

    private static void addCatalyst(Block block, String handlerId, int priority) {
        if (block != null) {
            API.addRecipeCatalyst(new ItemStack(block), handlerId, priority);
        }
    }

    private static void addCatalyst(Item item, String handlerId, int priority) {
        if (item != null) {
            API.addRecipeCatalyst(new ItemStack(item), handlerId, priority);
        }
    }

    private void registerHandlerInfoEvents() {
        if (registeredHandlerInfoEvents) {
            return;
        }
        registeredHandlerInfoEvents = true;
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void registerHandlerInfos(NEIRegisterHandlerInfosEvent event) {
        event.registerHandlerInfo(
                "minefantasy2.carpenter",
                MineFantasyII.MODID,
                MineFantasyII.NAME,
                builder -> builder.setDisplayStack(stack(BlockListMF.carpenter)).setMaxRecipesPerPage(1));
        event.registerHandlerInfo(
                "minefantasy2.anvil",
                MineFantasyII.MODID,
                MineFantasyII.NAME,
                builder -> builder.setDisplayStack(stack(getAnvilBlock())).setMaxRecipesPerPage(1));
        event.registerHandlerInfo(
                "minefantasy2.bloomery",
                MineFantasyII.MODID,
                MineFantasyII.NAME,
                builder -> builder.setDisplayStack(stack(BlockListMF.bloomery)).setMaxRecipesPerPage(1));
        event.registerHandlerInfo(
                "minefantasy2.quern",
                MineFantasyII.MODID,
                MineFantasyII.NAME,
                builder -> builder.setDisplayStack(stack(BlockListMF.quern)).setMaxRecipesPerPage(1));
        event.registerHandlerInfo(
                "minefantasy2.tanning",
                MineFantasyII.MODID,
                MineFantasyII.NAME,
                builder -> builder.setDisplayStack(stack(BlockListMF.tanner)).setMaxRecipesPerPage(1));
        event.registerHandlerInfo(
                "minefantasy2.cooking",
                MineFantasyII.MODID,
                MineFantasyII.NAME,
                builder -> builder.setDisplayStack(stack(BlockListMF.firepit)).setWidth(RecipeHandlerCooking.WIDTH)
                        .setHeight(RecipeHandlerCooking.HEIGHT).setMaxRecipesPerPage(1));
        event.registerHandlerInfo(
                "minefantasy2.salvage",
                MineFantasyII.MODID,
                MineFantasyII.NAME,
                builder -> builder.setDisplayStack(stack(BlockListMF.salvage_basic)).setMaxRecipesPerPage(1));
        event.registerHandlerInfo(
                "minefantasy2.paint_oil",
                MineFantasyII.MODID,
                MineFantasyII.NAME,
                builder -> builder.setDisplayStack(stack(ComponentListMF.plant_oil)).setMaxRecipesPerPage(1));
        event.registerHandlerInfo(
                "minefantasy2.crucible",
                MineFantasyII.MODID,
                MineFantasyII.NAME,
                builder -> builder.setDisplayStack(stack(BlockListMF.crucible)).setMaxRecipesPerPage(1));
        event.registerHandlerInfo(
                "minefantasy2.big_furnace",
                MineFantasyII.MODID,
                MineFantasyII.NAME,
                builder -> builder.setDisplayStack(stack(BlockListMF.furnace_stone)).setMaxRecipesPerPage(1));
        event.registerHandlerInfo(
                "minefantasy2.blast_furnace",
                MineFantasyII.MODID,
                MineFantasyII.NAME,
                builder -> builder.setDisplayStack(stack(BlockListMF.blast_chamber)).setMaxRecipesPerPage(1));
        event.registerHandlerInfo(
                "minefantasy2.bomb_bench",
                MineFantasyII.MODID,
                MineFantasyII.NAME,
                builder -> builder.setDisplayStack(stack(BlockListMF.bombBench))
                        .setWidth(RecipeHandlerBombBench.getWidth()).setHeight(RecipeHandlerBombBench.getHeight())
                        .setMaxRecipesPerPage(1));
        event.registerHandlerInfo(
                "minefantasy2.crossbow_bench",
                MineFantasyII.MODID,
                MineFantasyII.NAME,
                builder -> builder.setDisplayStack(stack(BlockListMF.crossbowBench))
                        .setWidth(RecipeHandlerCrossbowBench.getWidth())
                        .setHeight(RecipeHandlerCrossbowBench.getHeight()).setMaxRecipesPerPage(1));
    }

    private static Block getAnvilBlock() {
        if (BlockListMF.anvil != null) {
            for (Block block : BlockListMF.anvil) {
                if (block != null) {
                    return block;
                }
            }
        }
        return BlockListMF.anvilStone != null ? BlockListMF.anvilStone : Blocks.anvil;
    }

    private static ItemStack stack(Block block) {
        return block != null ? new ItemStack(block) : new ItemStack(Blocks.crafting_table);
    }

    private static ItemStack stack(Item item) {
        return item != null ? new ItemStack(item) : new ItemStack(Blocks.crafting_table);
    }
}
