package minefantasy.mf2.api.helpers;

import static minefantasy.mf2.gametest.Assert.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

/**
 * Checks the shipped lang files as data: every key of en_US is translated, en_GB only overrides, placeholders match
 * what the code passes, and the material forms of CustomToolHelper.withMaterial are complete.
 */
@GameTestHolder("minefantasy2")
public class LocalisationTest {

    private static final String DIR = "/assets/minefantasy2/lang/";
    private static final List<String> TRANSLATIONS = Arrays.asList("ru_RU", "zh_CN");
    /** Keys only a declining language needs: material forms and the "of" pattern of withMaterial. */
    private static final Pattern MATERIAL_FORM = Pattern
            .compile("material\\.\\w+\\.(gen|adj)|languagecfg\\.materialof");
    /** Names formatted by withMaterial, which passes seven arguments. */
    private static final Pattern MATERIAL_NAME = Pattern.compile(
            "item\\.(standard|dragonforged|ornate|commodity|mod)_\\w+\\.name"
                    + "|tile\\.(rack_wood|food_box_basic|ammo_box_basic|crate_basic|trough_wood)\\.name");
    /**
     * Format placeholders; a lone % is plain text in pages that are never formatted, a %1$ without conversion a typo.
     */
    private static final Pattern PERCENT = Pattern.compile("%(\\d+\\$)?[sdf]|%%|%\\d+\\$");
    private static final int SHOWN = 15;

    @GameTest
    public static void everyKeyIsTranslated(GameTestHelper helper) throws Exception {
        Map<String, String> en = load("en_US");
        List<String> problems = new ArrayList<>();
        for (String lang : TRANSLATIONS) {
            Map<String, String> tr = load(lang);
            for (String key : en.keySet()) {
                if (!tr.containsKey(key)) problems.add(lang + " misses " + key);
            }
            for (String key : tr.keySet()) {
                if (!en.containsKey(key) && !MATERIAL_FORM.matcher(key).matches()) {
                    problems.add(lang + " has a key en_US does not: " + key);
                }
            }
        }
        for (String key : load("en_GB").keySet()) {
            if (!en.containsKey(key)) problems.add("en_GB overrides a key en_US does not have: " + key);
        }
        report(problems);
        helper.succeed();
    }

    @GameTest
    public static void placeholdersMatchEnglish(GameTestHelper helper) throws Exception {
        Map<String, String> en = load("en_US");
        List<String> problems = new ArrayList<>();
        for (String lang : Arrays.asList("en_US", "en_GB", "ru_RU", "zh_CN")) {
            for (Map.Entry<String, String> e : load(lang).entrySet()) {
                String key = e.getKey();
                List<String> found = placeholders(e.getValue());
                if (found.stream().anyMatch(p -> p.endsWith("$"))) {
                    problems.add(lang + " " + key + ": a broken placeholder: " + e.getValue());
                    continue;
                }
                String source = en.get(key);
                if (source == null || lang.equals("en_US")) continue;
                if (MATERIAL_NAME.matcher(key).matches()) {
                    for (String p : found) {
                        if (!p.matches("%[1-7]\\$s")) problems.add(lang + " " + key + ": withMaterial has no " + p);
                    }
                } else if (!sorted(found).equals(sorted(placeholders(source)))) {
                    problems.add(lang + " " + key + ": " + found + " where en_US has " + placeholders(source));
                }
            }
        }
        report(problems);
        helper.succeed();
    }

    @GameTest
    public static void materialFormsAreComplete(GameTestHelper helper) throws Exception {
        List<String> problems = new ArrayList<>();
        for (String lang : TRANSLATIONS) {
            Map<String, String> tr = load(lang);
            boolean declines = false;
            for (String key : tr.keySet()) {
                declines |= key.endsWith(".gen") && key.startsWith("material.");
            }
            for (Map.Entry<String, String> e : tr.entrySet()) {
                String key = e.getKey();
                if (declines && key.matches("material\\.\\w+\\.name")) {
                    String gen = key.replaceFirst("\\.name$", ".gen");
                    if (!tr.containsKey(gen)) problems.add(lang + " has no " + gen);
                }
                if (key.endsWith(".adj") && key.startsWith("material.")) {
                    String[] parts = e.getValue().split("\\|", -1);
                    if (parts.length != 4 || Arrays.asList(parts).stream().anyMatch(p -> p.trim().isEmpty())) {
                        problems.add(lang + " " + key + " needs four forms m|f|n|pl: " + e.getValue());
                    }
                }
            }
        }
        report(problems);
        helper.succeed();
    }

    @GameTest
    public static void adjectiveFormsSurviveBrokenValues(GameTestHelper helper) {
        assertNull(CustomToolHelper.adjectiveForms(null));
        assertNull(CustomToolHelper.adjectiveForms(""));
        assertNull(CustomToolHelper.adjectiveForms("|||"));
        assertNull(CustomToolHelper.adjectiveForms(" | "));
        assertEquals(Arrays.asList("a", "b", "c", "d"), Arrays.asList(CustomToolHelper.adjectiveForms(" a |b|c|d|e")));
        assertEquals(Arrays.asList("a", "a", "a", "a"), Arrays.asList(CustomToolHelper.adjectiveForms("a")));
        assertEquals(Arrays.asList("a", "a", "c", "c"), Arrays.asList(CustomToolHelper.adjectiveForms("a||c")));
        assertEquals(Arrays.asList("b", "b", "b", "b"), Arrays.asList(CustomToolHelper.adjectiveForms("|b")));
        helper.succeed();
    }

    /** Reads a lang file the way the game does: key=value lines, everything else ignored; duplicates are errors. */
    private static Map<String, String> load(String lang) throws Exception {
        Map<String, String> map = new LinkedHashMap<>();
        List<String> duplicates = new ArrayList<>();
        try (InputStream in = LocalisationTest.class.getResourceAsStream(DIR + lang + ".lang")) {
            assertNotNull(lang + ".lang is not on the classpath", in);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("﻿")) line = line.substring(1);
                if (line.isEmpty() || line.charAt(0) == '#') continue;
                int eq = line.indexOf('=');
                if (eq < 0) continue;
                String key = line.substring(0, eq);
                if (map.put(key, line.substring(eq + 1)) != null) duplicates.add(key);
            }
        }
        report(
                duplicates.isEmpty() ? Collections.emptyList()
                        : Collections.singletonList(lang + " declares twice: " + duplicates));
        return map;
    }

    private static List<String> placeholders(String value) {
        List<String> out = new ArrayList<>();
        Matcher m = PERCENT.matcher(value);
        while (m.find()) {
            if (!m.group().equals("%%")) out.add(m.group());
        }
        return out;
    }

    private static List<String> sorted(List<String> list) {
        List<String> copy = new ArrayList<>(list);
        Collections.sort(copy);
        return copy;
    }

    private static void report(List<String> problems) {
        if (problems.isEmpty()) return;
        StringBuilder message = new StringBuilder(problems.size() + " lang problem(s):");
        for (String p : problems.subList(0, Math.min(SHOWN, problems.size()))) {
            message.append("\n  ").append(p);
        }
        if (problems.size() > SHOWN) message.append("\n  ...");
        fail(message.toString());
    }
}
