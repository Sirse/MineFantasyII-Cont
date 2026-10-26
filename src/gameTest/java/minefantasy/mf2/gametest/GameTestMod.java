package minefantasy.mf2.gametest;

import net.minecraft.server.MinecraftServer;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

/**
 * Loaded in development only, next to MineFantasy and Horizon-QA: registers the items and materials the game tests
 * craft with, so the tests do not depend on the mod's own content, and the MF2 Test World.
 */
@Mod(
        modid = GameTestMod.MODID,
        name = "MineFantasy II game tests",
        version = "test",
        dependencies = "required-after:minefantasy2;required-after:horizonqa",
        acceptedMinecraftVersions = "[1.7.10]")
public class GameTestMod {

    public static final String MODID = "minefantasy2tests";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        TestItems.register();
        StubbornBlock.register();
        ShowcaseWorld.register();
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new Showcase.Command());
    }

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        Showcase.buildIfNew(MinecraftServer.getServer().worldServerForDimension(0));
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance().bus().register(new Menagerie.Keeper());
        if (Loader.isModLoaded("MineTweaker3")) {
            CraftTweakerDev.registerBrackets();
        }
    }
}
