package minefantasy.mf2.api.recipe;

import static minefantasy.mf2.gametest.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.crafting.MFRecipes;

/**
 * The ids of the recipes the mod and its integrations register must not change by accident: a station saves its running
 * work under the recipe id, and scripts replace and remove recipes by it. The ids are checked against a snapshot kept
 * in the repository, so a change shows up in review as a diff of that file.
 * <p>
 * To accept a deliberate change, rewrite the snapshot: {@code ./gradlew runServer -PgameTests -PupdateRecipeIds}.
 */
@GameTestHolder("minefantasy2")
public class NativeRecipeIdTest {

    private static final String SNAPSHOT = "/minefantasy2tests/native_recipe_ids.txt";
    private static final String UPDATE = "minefantasy2tests.updateRecipeIds";
    private static final int SHOWN = 15;

    private NativeRecipeIdTest() {}

    /** One line per recipe registered in code, sorted: {@code kind id}. Script recipes are not part of it. */
    private static List<String> registeredIds() {
        List<String> lines = new ArrayList<>();
        for (RecipeRegistry<?> registry : MFRecipes.REGISTRIES.all()) {
            for (RecipeEntry<?> entry : registry.published().all()) {
                RecipeSource.Kind kind = entry.getSource().getKind();
                if (kind != RecipeSource.Kind.SCRIPT) {
                    lines.add(kind.name().toLowerCase() + " " + entry.getId());
                }
            }
        }
        Collections.sort(lines);
        return lines;
    }

    private static List<String> snapshot() throws IOException {
        List<String> lines = new ArrayList<>();
        InputStream in = NativeRecipeIdTest.class.getResourceAsStream(SNAPSHOT);
        if (in == null) {
            return null;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                if (!line.isEmpty() && !line.startsWith("#")) {
                    lines.add(line);
                }
            }
        }
        return lines;
    }

    private static void write(File file, List<String> ids) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("# Ids of the recipes registered in code, checked by NativeRecipeIdTest.");
        lines.add("# Rewrite on a deliberate change: ./gradlew runServer -PgameTests -PupdateRecipeIds");
        lines.addAll(ids);
        file.getParentFile().mkdirs();
        Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
    }

    private static String shown(String label, Set<String> ids) {
        StringBuilder text = new StringBuilder("\n  ").append(ids.size()).append(' ').append(label).append(':');
        int count = 0;
        for (String id : ids) {
            if (count++ == SHOWN) {
                text.append("\n    ...");
                break;
            }
            text.append("\n    ").append(id);
        }
        return text.toString();
    }

    @GameTest
    public static void nativeRecipeIdsMatchTheSnapshot(GameTestHelper helper) throws Exception {
        List<String> actual = registeredIds();
        assertFalse("no recipe is registered in code", actual.isEmpty());
        assertEquals("a recipe id is registered twice", actual.size(), new LinkedHashSet<>(actual).size());

        String update = System.getProperty(UPDATE);
        if (update != null) {
            write(new File(update), actual);
            helper.succeed();
            return;
        }

        List<String> expected = snapshot();
        assertNotNull("no snapshot " + SNAPSHOT + "; create it with -PupdateRecipeIds", expected);
        Set<String> added = new LinkedHashSet<>(actual);
        added.removeAll(expected);
        Set<String> removed = new LinkedHashSet<>(expected);
        removed.removeAll(actual);
        if (!added.isEmpty() || !removed.isEmpty()) {
            // The full list goes next to the reports, to compare or copy over the snapshot
            File written = new File(System.getProperty("horizonqa.reportDir", "."), "native_recipe_ids.txt");
            write(written, actual);
            fail(
                    "recipe ids changed; saved work and scripts that name them break" + shown("new", added)
                            + shown("gone", removed)
                            + "\n  all ids: "
                            + written.getAbsolutePath()
                            + "\n  accept a deliberate change with -PupdateRecipeIds");
        }
        helper.succeed();
    }
}
