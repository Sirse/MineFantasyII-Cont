package minefantasy.mf2.gametest;

import minetweaker.MineTweakerAPI;
import minetweaker.mc1710.brackets.ItemBracketHandler;
import minetweaker.mc1710.brackets.LiquidBracketHandler;
import minetweaker.mc1710.brackets.OreBracketHandler;

/**
 * CraftTweaker's development jar ships its class registry as an empty stub (the release build generates it), so on the
 * development server no bracket handler exists and a script cannot name a single item. This registers the handlers the
 * release jar would.
 */
final class CraftTweakerDev {

    private CraftTweakerDev() {}

    static void registerBrackets() {
        MineTweakerAPI.registerBracketHandler(new ItemBracketHandler());
        MineTweakerAPI.registerBracketHandler(new OreBracketHandler());
        MineTweakerAPI.registerBracketHandler(new LiquidBracketHandler());
    }
}
