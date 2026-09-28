package minefantasy.mf2.gametest;

import java.io.File;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import minetweaker.MineTweakerAPI;
import minetweaker.mc1710.brackets.ItemBracketHandler;
import minetweaker.mc1710.brackets.LiquidBracketHandler;
import minetweaker.mc1710.brackets.OreBracketHandler;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenExpansion;

/**
 * CraftTweaker's development jar ships its class registry as an empty stub (the release build generates it), so on the
 * development server scripts know none of its types: no bracket handler names an item, and no NBT literal becomes data.
 * This registers what the release jar would.
 */
final class CraftTweakerDev {

    private CraftTweakerDev() {}

    static void registerBrackets() {
        MineTweakerAPI.registerBracketHandler(new ItemBracketHandler());
        MineTweakerAPI.registerBracketHandler(new OreBracketHandler());
        MineTweakerAPI.registerBracketHandler(new LiquidBracketHandler());
        registerClasses();
    }

    /** Every class of the CraftTweaker jar a script can use: those it marks as a ZenScript class or expansion. */
    private static void registerClasses() {
        ClassLoader loader = MineTweakerAPI.class.getClassLoader();
        // Under the launch wrapper the class comes as jar:file:<jar>!/<class>
        String url = String.valueOf(MineTweakerAPI.class.getResource("MineTweakerAPI.class"));
        if (!url.startsWith("jar:") || !url.contains("!")) {
            throw new IllegalStateException("CraftTweaker is not loaded from a jar: " + url);
        }
        try (JarFile jar = new JarFile(new File(new URL(url.substring(4, url.indexOf('!'))).toURI()))) {
            for (Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements();) {
                String name = entries.nextElement().getName();
                if (!name.startsWith("minetweaker/") || !name.endsWith(".class") || name.contains("$")) {
                    continue;
                }
                try {
                    Class<?> type = Class.forName(name.replace('/', '.').replace(".class", ""), false, loader);
                    if (type.isAnnotationPresent(ZenClass.class) || type.isAnnotationPresent(ZenExpansion.class)) {
                        MineTweakerAPI.registerClass(type);
                    }
                } catch (Throwable clientOnly) {
                    // Classes of the client side cannot load on a dedicated server; scripts here do not need them
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Cannot read the CraftTweaker jar", e);
        }
    }
}
