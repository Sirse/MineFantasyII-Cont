package minefantasy.mf2.network;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.api.MineFantasyAPI;
import minefantasy.mf2.api.helpers.ClientTickHandler;
import minefantasy.mf2.api.knowledge.InformationList;
import minefantasy.mf2.block.tileentity.TileEntityAnvilMF;
import minefantasy.mf2.block.tileentity.TileEntityBellows;
import minefantasy.mf2.block.tileentity.TileEntityBigFurnace;
import minefantasy.mf2.block.tileentity.TileEntityBloomery;
import minefantasy.mf2.block.tileentity.TileEntityBombBench;
import minefantasy.mf2.block.tileentity.TileEntityBombPress;
import minefantasy.mf2.block.tileentity.TileEntityCarpenterMF;
import minefantasy.mf2.block.tileentity.TileEntityChimney;
import minefantasy.mf2.block.tileentity.TileEntityComponent;
import minefantasy.mf2.block.tileentity.TileEntityCrossbowBench;
import minefantasy.mf2.block.tileentity.TileEntityCrucible;
import minefantasy.mf2.block.tileentity.TileEntityFirepit;
import minefantasy.mf2.block.tileentity.TileEntityForge;
import minefantasy.mf2.block.tileentity.TileEntityQuern;
import minefantasy.mf2.block.tileentity.TileEntityResearch;
import minefantasy.mf2.block.tileentity.TileEntityRoast;
import minefantasy.mf2.block.tileentity.TileEntitySoakingTrough;
import minefantasy.mf2.block.tileentity.TileEntityTanningRack;
import minefantasy.mf2.block.tileentity.TileEntityTarKiln;
import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFC;
import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFH;
import minefantasy.mf2.block.tileentity.decor.TileEntityAmmoBox;
import minefantasy.mf2.block.tileentity.decor.TileEntityRack;
import minefantasy.mf2.block.tileentity.decor.TileEntityTrough;
import minefantasy.mf2.client.KnowledgePageRegistry;
import minefantasy.mf2.client.gui.GuiAnvilMF;
import minefantasy.mf2.client.gui.GuiBigFurnace;
import minefantasy.mf2.client.gui.GuiBlastChamber;
import minefantasy.mf2.client.gui.GuiBlastHeater;
import minefantasy.mf2.client.gui.GuiBloomery;
import minefantasy.mf2.client.gui.GuiBombBench;
import minefantasy.mf2.client.gui.GuiCarpenterMF;
import minefantasy.mf2.client.gui.GuiCrossbowBench;
import minefantasy.mf2.client.gui.GuiCrucible;
import minefantasy.mf2.client.gui.GuiForge;
import minefantasy.mf2.client.gui.GuiKnowledge;
import minefantasy.mf2.client.gui.GuiKnowledgeEntry;
import minefantasy.mf2.client.gui.GuiMF2Player;
import minefantasy.mf2.client.gui.GuiQuern;
import minefantasy.mf2.client.gui.GuiReload;
import minefantasy.mf2.client.gui.GuiResearchBlock;
import minefantasy.mf2.client.gui.GuiTarKiln;
import minefantasy.mf2.client.gui.tabs.InventoryTabMF2;
import minefantasy.mf2.client.gui.tabs.InventoryTabVanilla;
import minefantasy.mf2.client.gui.tabs.TabRegistry;
import minefantasy.mf2.client.render.AnimationHandlerMF;
import minefantasy.mf2.client.render.HudHandlerMF;
import minefantasy.mf2.client.render.RenderArrowMF;
import minefantasy.mf2.client.render.RenderBombIcon;
import minefantasy.mf2.client.render.RenderBow;
import minefantasy.mf2.client.render.RenderCrossbow;
import minefantasy.mf2.client.render.RenderDragonBreath;
import minefantasy.mf2.client.render.RenderFireBlast;
import minefantasy.mf2.client.render.RenderHeavyWeapon;
import minefantasy.mf2.client.render.RenderLance;
import minefantasy.mf2.client.render.RenderMine;
import minefantasy.mf2.client.render.RenderParachute;
import minefantasy.mf2.client.render.RenderPowerArmour;
import minefantasy.mf2.client.render.RenderSaw;
import minefantasy.mf2.client.render.RenderShrapnel;
import minefantasy.mf2.client.render.RenderSpear;
import minefantasy.mf2.client.render.RenderSword;
import minefantasy.mf2.client.render.block.RenderAmmoBox;
import minefantasy.mf2.client.render.block.RenderAnvilMF;
import minefantasy.mf2.client.render.block.RenderBellows;
import minefantasy.mf2.client.render.block.RenderBigFurnace;
import minefantasy.mf2.client.render.block.RenderBloomery;
import minefantasy.mf2.client.render.block.RenderBombBench;
import minefantasy.mf2.client.render.block.RenderBombPress;
import minefantasy.mf2.client.render.block.RenderCarpenter;
import minefantasy.mf2.client.render.block.RenderCrossbowBench;
import minefantasy.mf2.client.render.block.RenderFirepit;
import minefantasy.mf2.client.render.block.RenderForge;
import minefantasy.mf2.client.render.block.RenderQuern;
import minefantasy.mf2.client.render.block.RenderRack;
import minefantasy.mf2.client.render.block.RenderResearch;
import minefantasy.mf2.client.render.block.RenderRoast;
import minefantasy.mf2.client.render.block.RenderSmokePipe;
import minefantasy.mf2.client.render.block.RenderSoakingTrough;
import minefantasy.mf2.client.render.block.RenderTanningRack;
import minefantasy.mf2.client.render.block.RenderTrough;
import minefantasy.mf2.client.render.block.TileEntityAmmoBoxRenderer;
import minefantasy.mf2.client.render.block.TileEntityAnvilMFRenderer;
import minefantasy.mf2.client.render.block.TileEntityBellowsRenderer;
import minefantasy.mf2.client.render.block.TileEntityBigFurnaceRenderer;
import minefantasy.mf2.client.render.block.TileEntityBloomeryRenderer;
import minefantasy.mf2.client.render.block.TileEntityBombBenchRenderer;
import minefantasy.mf2.client.render.block.TileEntityBombPressRenderer;
import minefantasy.mf2.client.render.block.TileEntityCarpenterRenderer;
import minefantasy.mf2.client.render.block.TileEntityCrossbowBenchRenderer;
import minefantasy.mf2.client.render.block.TileEntityFirepitRenderer;
import minefantasy.mf2.client.render.block.TileEntityForgeRenderer;
import minefantasy.mf2.client.render.block.TileEntityQuernRenderer;
import minefantasy.mf2.client.render.block.TileEntityRackRenderer;
import minefantasy.mf2.client.render.block.TileEntityResearchRenderer;
import minefantasy.mf2.client.render.block.TileEntityRoastRenderer;
import minefantasy.mf2.client.render.block.TileEntitySmokePipeRenderer;
import minefantasy.mf2.client.render.block.TileEntityTanningRackRenderer;
import minefantasy.mf2.client.render.block.TileEntityTroughRenderer;
import minefantasy.mf2.client.render.block.component.TileEntityComponentRenderer;
import minefantasy.mf2.client.render.mob.ModelHound;
import minefantasy.mf2.client.render.mob.ModelMinotaur;
import minefantasy.mf2.client.render.mob.RenderDragon;
import minefantasy.mf2.client.render.mob.RenderHound;
import minefantasy.mf2.client.render.mob.RenderMinotaur;
import minefantasy.mf2.entity.EntityArrowMF;
import minefantasy.mf2.entity.EntityBomb;
import minefantasy.mf2.entity.EntityCogwork;
import minefantasy.mf2.entity.EntityDragonBreath;
import minefantasy.mf2.entity.EntityFireBlast;
import minefantasy.mf2.entity.EntityMine;
import minefantasy.mf2.entity.EntityParachute;
import minefantasy.mf2.entity.EntityShrapnel;
import minefantasy.mf2.entity.EntitySmoke;
import minefantasy.mf2.entity.mob.EntityDragon;
import minefantasy.mf2.entity.mob.EntityHound;
import minefantasy.mf2.entity.mob.EntityMinotaur;
import minefantasy.mf2.item.list.CustomToolListMF;
import minefantasy.mf2.item.list.ToolListMF;
import minefantasy.mf2.item.list.styles.DragonforgedStyle;
import minefantasy.mf2.item.list.styles.OrnateStyle;
import minefantasy.mf2.mechanics.ExtendedReachMF;
import minefantasy.mf2.mechanics.PlayerTickHandlerMF;
import minefantasy.mf2.player.IEEPMF2;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;

/**
 * @author Anonymous Productions
 */
@SideOnly(Side.CLIENT)
public class ClientProxyMF extends CommonProxyMF {

    /**
     * Is the player trying to jump (assuming no screens are open)
     */
    public static boolean isUserJumpCommand(Entity user) {
        return Minecraft.getMinecraft().currentScreen == null && user == Minecraft.getMinecraft().thePlayer
                && Keyboard.isKeyDown(Minecraft.getMinecraft().gameSettings.keyBindJump.getKeyCode());
    }

    @Override
    public World getClientWorld() {
        return FMLClientHandler.instance().getClient().theWorld;
    }

    @Override
    public void preInit() {
    }

    @Override
    public void registerMain() {
        super.registerMain();
        registerRenders();
    }

    @Override
    public void postInit() {
        super.postInit();
        MineFantasyAPI.init();
        KnowledgePageRegistry.registerPages();
    }

    @Override
    public void registerTickHandlers() {
        super.registerTickHandlers();
        FMLCommonHandler.instance().bus().register(new PlayerTickHandlerMF());
        FMLCommonHandler.instance().bus().register(new AnimationHandlerMF());
        FMLCommonHandler.instance().bus().register(new ExtendedReachMF());
        MinecraftForge.EVENT_BUS.register(new HudHandlerMF());
        FMLCommonHandler.instance().bus().register(new ClientTickHandler());

        RenderingRegistry.registerBlockHandler(new RenderAnvilMF());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityAnvilMF.class, new TileEntityAnvilMFRenderer());
        RenderingRegistry.registerBlockHandler(new RenderCarpenter());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityCarpenterMF.class, new TileEntityCarpenterRenderer());
        RenderingRegistry.registerBlockHandler(new RenderBombBench());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityBombBench.class, new TileEntityBombBenchRenderer());
        RenderingRegistry.registerBlockHandler(new RenderTanningRack());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityTanningRack.class, new TileEntityTanningRackRenderer());
        RenderingRegistry.registerBlockHandler(new RenderForge());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityForge.class, new TileEntityForgeRenderer());
        RenderingRegistry.registerBlockHandler(new RenderBellows());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityBellows.class, new TileEntityBellowsRenderer());
        RenderingRegistry.registerBlockHandler(new RenderResearch());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityResearch.class, new TileEntityResearchRenderer());
        RenderingRegistry.registerBlockHandler(new RenderTrough());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityTrough.class, new TileEntityTroughRenderer());

        RenderingRegistry.registerBlockHandler(new RenderBombPress());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityBombPress.class, new TileEntityBombPressRenderer());
        RenderingRegistry.registerBlockHandler(new RenderBloomery());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityBloomery.class, new TileEntityBloomeryRenderer());
        RenderingRegistry.registerBlockHandler(new RenderCrossbowBench());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityCrossbowBench.class,
                new TileEntityCrossbowBenchRenderer());
        RenderingRegistry.registerBlockHandler(new RenderQuern());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityQuern.class, new TileEntityQuernRenderer());
        RenderingRegistry.registerBlockHandler(new RenderFirepit());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityFirepit.class, new TileEntityFirepitRenderer());
        RenderingRegistry.registerBlockHandler(new RenderRoast());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityRoast.class, new TileEntityRoastRenderer());
        RenderingRegistry.registerBlockHandler(new RenderBigFurnace());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityBigFurnace.class, new TileEntityBigFurnaceRenderer());
        RenderingRegistry.registerBlockHandler(new RenderRack());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityRack.class, new TileEntityRackRenderer());
        RenderingRegistry.registerBlockHandler(new RenderAmmoBox());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityAmmoBox.class, new TileEntityAmmoBoxRenderer());
        RenderingRegistry.registerBlockHandler(new RenderSmokePipe());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityChimney.class, new TileEntitySmokePipeRenderer());
        RenderingRegistry.registerBlockHandler(new RenderSoakingTrough());
        // ClientRegistry.bindTileEntitySpecialRenderer(TileEntitySoakingTrough.class, new TileEntitySoakingTroughRenderer());

        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityComponent.class, new TileEntityComponentRenderer());
        
        MinecraftForge.EVENT_BUS.register(new TabRegistry());
        MinecraftForge.EVENT_BUS.register(new IEEPMF2.Handler());
        TabRegistry.registerTab(new InventoryTabVanilla());
        TabRegistry.registerTab(new InventoryTabMF2());
        
    }

    public void registerEntityRenderer() {
        RenderingRegistry.registerEntityRenderingHandler(EntityArrowMF.class, new RenderArrowMF());
        RenderingRegistry.registerEntityRenderingHandler(EntityBomb.class, new RenderBombIcon());// Switch to RenderBomb
        // when syncing is
        // fixed
        RenderingRegistry.registerEntityRenderingHandler(EntityMine.class, new RenderMine());
        RenderingRegistry.registerEntityRenderingHandler(EntityShrapnel.class, new RenderShrapnel("shrapnel"));
        RenderingRegistry.registerEntityRenderingHandler(EntityFireBlast.class, new RenderFireBlast());
        RenderingRegistry.registerEntityRenderingHandler(EntitySmoke.class, new RenderFireBlast());
        RenderingRegistry.registerEntityRenderingHandler(EntityDragonBreath.class, new RenderDragonBreath());
        RenderingRegistry.registerEntityRenderingHandler(EntityParachute.class, new RenderParachute());
        RenderingRegistry.registerEntityRenderingHandler(EntityCogwork.class, new RenderPowerArmour());

        RenderingRegistry.registerEntityRenderingHandler(EntityDragon.class, new RenderDragon(2F));
        RenderingRegistry.registerEntityRenderingHandler(EntityMinotaur.class,
                new RenderMinotaur(new ModelMinotaur(), 1.5F));
        RenderingRegistry.registerEntityRenderingHandler(EntityHound.class, new RenderHound(new ModelHound()));
    }

    @Override
    public EntityPlayer getClientPlayer() {
        return Minecraft.getMinecraft().thePlayer;
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        Minecraft mc = Minecraft.getMinecraft();
        if (ID == 0) {
            TileEntity tile = world.getTileEntity(x, y, z);
            // int meta = world.getBlockMetadata(x, y, z);

            if (tile == null) {
                return null;
            }
            if (tile instanceof TileEntityTarKiln) {
                return new GuiTarKiln(player.inventory, (TileEntityTarKiln) tile);
            }

            if (tile instanceof TileEntityAnvilMF) {
                return new GuiAnvilMF(player.inventory, (TileEntityAnvilMF) tile);
            }
            if (tile instanceof TileEntityCarpenterMF) {
                return new GuiCarpenterMF(player.inventory, (TileEntityCarpenterMF) tile);
            }
            if (tile instanceof TileEntityBombBench) {
                return new GuiBombBench(player.inventory, (TileEntityBombBench) tile);
            }
            if (tile instanceof TileEntityBlastFH) {
                return new GuiBlastHeater(player.inventory, (TileEntityBlastFH) tile);
            }
            if (tile instanceof TileEntityBlastFC) {
                return new GuiBlastChamber(player.inventory, (TileEntityBlastFC) tile);
            }
            if (tile instanceof TileEntityCrucible) {
                return new GuiCrucible(player.inventory, (TileEntityCrucible) tile);
            }
            if (tile instanceof TileEntityForge) {
                return new GuiForge(player.inventory, (TileEntityForge) tile);
            }
            if (tile instanceof TileEntityResearch) {
                return new GuiResearchBlock(player.inventory, (TileEntityResearch) tile);
            }
            if (tile instanceof TileEntityBloomery) {
                return new GuiBloomery(player.inventory, (TileEntityBloomery) tile);
            }
            if (tile instanceof TileEntityCrossbowBench) {
                return new GuiCrossbowBench(player.inventory, (TileEntityCrossbowBench) tile);
            }
            if (tile instanceof TileEntityQuern) {
                return new GuiQuern(player.inventory, (TileEntityQuern) tile);
            }
            if (tile instanceof TileEntityBigFurnace) {
                return new GuiBigFurnace(player, (TileEntityBigFurnace) tile);
            }
            return null;
        }
        if (ID == 1) {
            if (x == 0) {// GuiAchievements
                if (y >= 0) {
                    return new GuiKnowledgeEntry(mc.currentScreen, InformationList.knowledgeList.get(y));
                }
                return new GuiKnowledge(player);
            }
            if (x == 1 && player.getHeldItem() != null) {
                return new GuiReload(player.inventory, player.getHeldItem());
            }
            if (x == 10) {
            	return new GuiMF2Player();
            }
        }
        return null;
    }

    private void registerRenders() {
        registerEntityRenderer();

        MinecraftForgeClient.registerItemRenderer(ToolListMF.swordStone, new RenderSword());
        MinecraftForgeClient.registerItemRenderer(ToolListMF.maceStone, new RenderSword());
        MinecraftForgeClient.registerItemRenderer(ToolListMF.waraxeStone, new RenderSword().setAxe());
        MinecraftForgeClient.registerItemRenderer(ToolListMF.spearStone, new RenderSpear());
        MinecraftForgeClient.registerItemRenderer(ToolListMF.swordTraining, new RenderSword());
        MinecraftForgeClient.registerItemRenderer(ToolListMF.crossbow_custom, new RenderCrossbow(2.0F));

        // STANDARD
        MinecraftForgeClient.registerItemRenderer(CustomToolListMF.standard_dagger, new RenderSword());
        MinecraftForgeClient.registerItemRenderer(CustomToolListMF.standard_sword, new RenderSword());
        MinecraftForgeClient.registerItemRenderer(CustomToolListMF.standard_waraxe, new RenderSword().setAxe());
        MinecraftForgeClient.registerItemRenderer(CustomToolListMF.standard_mace, new RenderSword());
        MinecraftForgeClient.registerItemRenderer(CustomToolListMF.standard_scythe, new RenderHeavyWeapon().setBlunt());
        MinecraftForgeClient.registerItemRenderer(CustomToolListMF.standard_warhammer,
                new RenderHeavyWeapon().setBlunt());
        MinecraftForgeClient.registerItemRenderer(CustomToolListMF.standard_battleaxe,
                new RenderHeavyWeapon().setBlunt().setParryable());
        MinecraftForgeClient.registerItemRenderer(CustomToolListMF.standard_greatsword,
                new RenderHeavyWeapon().setGreatsword().setParryable());
        MinecraftForgeClient.registerItemRenderer(CustomToolListMF.standard_katana,
                new RenderHeavyWeapon().setKatana().setParryable());
        MinecraftForgeClient.registerItemRenderer(CustomToolListMF.standard_spear, new RenderSpear());
        MinecraftForgeClient.registerItemRenderer(CustomToolListMF.standard_halbeard, new RenderSpear(true));
        MinecraftForgeClient.registerItemRenderer(CustomToolListMF.standard_lance, new RenderLance());
        MinecraftForgeClient.registerItemRenderer(CustomToolListMF.standard_lumber, new RenderHeavyWeapon().setBlunt());
        MinecraftForgeClient.registerItemRenderer(CustomToolListMF.standard_saw, new RenderSaw());
        MinecraftForgeClient.registerItemRenderer(CustomToolListMF.standard_bow, new RenderBow(false));
        // DRAGONFORGED
        MinecraftForgeClient.registerItemRenderer(DragonforgedStyle.dragonforged_dagger, new RenderSword());
        MinecraftForgeClient.registerItemRenderer(DragonforgedStyle.dragonforged_sword, new RenderSword());
        MinecraftForgeClient.registerItemRenderer(DragonforgedStyle.dragonforged_waraxe, new RenderSword().setAxe());
        MinecraftForgeClient.registerItemRenderer(DragonforgedStyle.dragonforged_mace, new RenderSword());
        MinecraftForgeClient.registerItemRenderer(DragonforgedStyle.dragonforged_warhammer,
                new RenderHeavyWeapon().setBlunt());
        MinecraftForgeClient.registerItemRenderer(DragonforgedStyle.dragonforged_battleaxe,
                new RenderHeavyWeapon().setBlunt().setParryable());
        MinecraftForgeClient.registerItemRenderer(DragonforgedStyle.dragonforged_greatsword,
                new RenderHeavyWeapon().setGreatsword().setParryable());
        MinecraftForgeClient.registerItemRenderer(DragonforgedStyle.dragonforged_katana,
                new RenderHeavyWeapon().setKatana().setParryable());
        MinecraftForgeClient.registerItemRenderer(DragonforgedStyle.dragonforged_spear, new RenderSpear());
        MinecraftForgeClient.registerItemRenderer(DragonforgedStyle.dragonforged_halbeard, new RenderSpear(true));
        MinecraftForgeClient.registerItemRenderer(DragonforgedStyle.dragonforged_lance, new RenderLance());
        MinecraftForgeClient.registerItemRenderer(DragonforgedStyle.dragonforged_scythe,
                new RenderHeavyWeapon().setBlunt());
        MinecraftForgeClient.registerItemRenderer(DragonforgedStyle.dragonforged_lumber,
                new RenderHeavyWeapon().setBlunt());
        MinecraftForgeClient.registerItemRenderer(DragonforgedStyle.dragonforged_saw, new RenderSaw());
        MinecraftForgeClient.registerItemRenderer(DragonforgedStyle.dragonforged_bow, new RenderBow(false));

        // ORNATE
        MinecraftForgeClient.registerItemRenderer(OrnateStyle.ornate_dagger, new RenderSword());
        MinecraftForgeClient.registerItemRenderer(OrnateStyle.ornate_sword, new RenderSword());
        MinecraftForgeClient.registerItemRenderer(OrnateStyle.ornate_waraxe, new RenderSword().setAxe());
        MinecraftForgeClient.registerItemRenderer(OrnateStyle.ornate_mace, new RenderSword());
        MinecraftForgeClient.registerItemRenderer(OrnateStyle.ornate_warhammer, new RenderHeavyWeapon().setBlunt());
        MinecraftForgeClient.registerItemRenderer(OrnateStyle.ornate_battleaxe,
                new RenderHeavyWeapon().setBlunt().setParryable());
        MinecraftForgeClient.registerItemRenderer(OrnateStyle.ornate_greatsword,
                new RenderHeavyWeapon().setGreatsword().setParryable());
        MinecraftForgeClient.registerItemRenderer(OrnateStyle.ornate_katana,
                new RenderHeavyWeapon().setKatana().setParryable());
        MinecraftForgeClient.registerItemRenderer(OrnateStyle.ornate_spear, new RenderSpear());
        MinecraftForgeClient.registerItemRenderer(OrnateStyle.ornate_halbeard, new RenderSpear(true));
        MinecraftForgeClient.registerItemRenderer(OrnateStyle.ornate_lance, new RenderLance());
        MinecraftForgeClient.registerItemRenderer(OrnateStyle.ornate_scythe, new RenderHeavyWeapon().setBlunt());
        MinecraftForgeClient.registerItemRenderer(OrnateStyle.ornate_lumber, new RenderHeavyWeapon().setBlunt());
        MinecraftForgeClient.registerItemRenderer(OrnateStyle.ornate_saw, new RenderSaw());
        MinecraftForgeClient.registerItemRenderer(OrnateStyle.ornate_bow, new RenderBow(false));

    }
}
