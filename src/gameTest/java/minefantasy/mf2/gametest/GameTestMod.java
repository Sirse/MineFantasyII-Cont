package minefantasy.mf2.gametest;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/**
 * Loaded on the development server only, next to MineFantasy and Horizon-QA: registers the items and materials the game
 * tests craft with, so the tests do not depend on the mod's own content.
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
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Loader.isModLoaded("MineTweaker3")) {
            CraftTweakerDev.registerBrackets();
        }
    }
}
