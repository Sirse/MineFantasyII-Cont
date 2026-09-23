package minefantasy.mf2.integration.nei;

import java.util.function.Consumer;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import codechicken.nei.event.NEIRegisterHandlerInfosEvent;
import codechicken.nei.recipe.DefaultOverlayHandler;
import codechicken.nei.recipe.HandlerInfo;
import cpw.mods.fml.common.Optional;
import minefantasy.mf2.MineFantasyII;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.client.gui.GuiCarpenterMF;
import minefantasy.mf2.client.gui.GuiKitchenBench;
import minefantasy.mf2.config.ConfigIntegration;
import minefantasy.mf2.config.ConfigKitchen;
import minefantasy.mf2.item.list.ComponentListMF;

@Optional.Interface(iface = "codechicken.nei.api.IConfigureNEI", modid = "NotEnoughItems")
public class NEIConfig implements IConfigureNEI {

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
            RecipeHandlerCarpenter handlerCarpenter = new RecipeHandlerCarpenter();
            API.registerRecipeHandler(handlerCarpenter);
            API.registerUsageHandler(handlerCarpenter);
            API.registerGuiOverlay(
                    GuiCarpenterMF.class,
                    "minefantasy2.carpenter",
                    NEILayout.BENCH_OVERLAY_X,
                    NEILayout.BENCH_OVERLAY_Y);
            API.registerGuiOverlayHandler(
                    GuiCarpenterMF.class,
                    new DefaultOverlayHandler(NEILayout.BENCH_OVERLAY_X, NEILayout.BENCH_OVERLAY_Y),
                    "minefantasy2.carpenter");

            if (ConfigKitchen.enableBench) {
                RecipeHandlerKitchen handlerKitchen = new RecipeHandlerKitchen();
                API.registerRecipeHandler(handlerKitchen);
                API.registerUsageHandler(handlerKitchen);
                API.registerGuiOverlay(
                        GuiKitchenBench.class,
                        "minefantasy2.kitchen",
                        NEILayout.BENCH_OVERLAY_X,
                        NEILayout.BENCH_OVERLAY_Y);
                API.registerGuiOverlayHandler(
                        GuiKitchenBench.class,
                        new DefaultOverlayHandler(NEILayout.BENCH_OVERLAY_X, NEILayout.BENCH_OVERLAY_Y),
                        "minefantasy2.kitchen");
            }

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

            registerHandlerInfos(new NEIRegisterHandlerInfosEvent());
        }
    }

    private void registerRecipeCatalysts() {
        addCatalyst(BlockListMF.carpenter, "minefantasy2.carpenter", 100);
        if (ConfigKitchen.enableBench) {
            addCatalyst(BlockListMF.kitchenBench, "minefantasy2.kitchen", 100);
        }
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

    // Called explicitly instead of subscribed as an event: GTNH NEI may not fire NEIRegisterHandlerInfosEvent after
    // this plugin loads, and a live subscription would register everything a second time ("Replaced handler info"
    // log noise). registerHandlerInfo writes into a static map, so the event object is only used as a carrier.
    private void registerHandlerInfos(NEIRegisterHandlerInfosEvent event) {
        register(
                event,
                "minefantasy2.carpenter",
                builder -> builder.setDisplayStack(stack(BlockListMF.carpenter)).setHeight(145).setMaxRecipesPerPage(1))
                .setShowOverlayButton(true);
        if (ConfigKitchen.enableBench) {
            register(
                    event,
                    "minefantasy2.kitchen",
                    builder -> builder.setDisplayStack(stack(BlockListMF.kitchenBench)).setHeight(145)
                            .setMaxRecipesPerPage(1))
                    .setShowOverlayButton(true);
        }
        register(
                event,
                "minefantasy2.anvil",
                builder -> builder.setDisplayStack(stack(getAnvilBlock())).setHeight(136).setMaxRecipesPerPage(1));
        register(
                event,
                "minefantasy2.bloomery",
                builder -> builder.setDisplayStack(stack(BlockListMF.bloomery)).setHeight(88).setMaxRecipesPerPage(1));
        register(
                event,
                "minefantasy2.quern",
                builder -> builder.setDisplayStack(stack(BlockListMF.quern)).setHeight(80).setMaxRecipesPerPage(1));
        register(
                event,
                "minefantasy2.tanning",
                builder -> builder.setDisplayStack(stack(BlockListMF.tanner)).setHeight(95).setMaxRecipesPerPage(1));
        register(
                event,
                "minefantasy2.cooking",
                builder -> builder.setDisplayStack(stack(BlockListMF.firepit)).setWidth(RecipeHandlerCooking.WIDTH)
                        .setHeight(RecipeHandlerCooking.HEIGHT).setMaxRecipesPerPage(1));
        HandlerInfo salvage = register(
                event,
                "minefantasy2.salvage",
                builder -> builder.setDisplayStack(stack(BlockListMF.salvage_basic)).setHeight(40).setShiftY(5)
                        .setMaxRecipesPerPage(RecipeHandlerSalvage.RECIPES_PER_PAGE));
        salvage.setShowFavoritesButton(false);
        register(
                event,
                "minefantasy2.paint_oil",
                builder -> builder.setDisplayStack(stack(ComponentListMF.plant_oil)).setHeight(66)
                        .setMaxRecipesPerPage(1));
        register(
                event,
                "minefantasy2.crucible",
                builder -> builder.setDisplayStack(stack(BlockListMF.crucible)).setHeight(94).setMaxRecipesPerPage(1));
        register(
                event,
                "minefantasy2.big_furnace",
                builder -> builder.setDisplayStack(stack(BlockListMF.furnace_stone)).setHeight(63)
                        .setMaxRecipesPerPage(1));
        register(
                event,
                "minefantasy2.blast_furnace",
                builder -> builder.setDisplayStack(stack(BlockListMF.blast_chamber)).setHeight(112)
                        .setMaxRecipesPerPage(1));
        register(
                event,
                "minefantasy2.bomb_bench",
                builder -> builder.setDisplayStack(stack(BlockListMF.bombBench))
                        .setWidth(RecipeHandlerBombBench.getWidth()).setHeight(RecipeHandlerBombBench.getHeight())
                        .setMaxRecipesPerPage(1));
        register(
                event,
                "minefantasy2.crossbow_bench",
                builder -> builder.setDisplayStack(stack(BlockListMF.crossbowBench))
                        .setWidth(RecipeHandlerCrossbowBench.getWidth())
                        .setHeight(RecipeHandlerCrossbowBench.getHeight()).setMaxRecipesPerPage(1));
    }

    /**
     * Registers a handler's layout. The overlay button is hidden by default: it only works for a station with an
     * overlay handler registered for its GUI, which re-enables it.
     */
    private static HandlerInfo register(NEIRegisterHandlerInfosEvent event, String handlerId,
            Consumer<HandlerInfo.Builder> layout) {
        HandlerInfo.Builder builder = new HandlerInfo.Builder(handlerId, MineFantasyII.MODID, MineFantasyII.NAME);
        layout.accept(builder);
        HandlerInfo info = builder.build();
        info.setShowOverlayButton(false);
        event.registerHandlerInfo(info);
        return info;
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
