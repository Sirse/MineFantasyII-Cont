package minefantasy.mf2.coremod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;

import cpw.mods.fml.relauncher.FMLLaunchHandler;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

/**
 * Registers MineFantasy's mixins with the Mixin runtime (UniMixins): they change vanilla classes, so they load early.
 * Nothing else is transformed here.
 */
@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.Name("MineFantasyCore")
public class MineFantasyCore implements IFMLLoadingPlugin, IEarlyMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.minefantasy2.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        List<String> mixins = new ArrayList<>(Arrays.asList("EntityPlayerAccessor", "ItemInWorldManagerMixin"));
        // The player controller and renderer are client only: their mixins would not resolve on a dedicated server
        if (FMLLaunchHandler.side().isClient()) {
            mixins.add("PlayerControllerAccessor");
            mixins.add("EntityRendererMixin");
        }
        return mixins;
    }

    @Override
    public String[] getASMTransformerClass() {
        return null;
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {}

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
