package minefantasy.mf2.api.crafting;

import minefantasy.mf2.api.recipe.RecipeMetadataKey;

/** Metadata keys of MineFantasy stations. */
public final class MFRecipeKeys {

    /** Station tier the recipe needs; -1 for any. */
    public static final RecipeMetadataKey<Integer> TIER = RecipeMetadataKey
            .create("minefantasy2:tier", Integer.class, v -> v >= -1, "minefantasy2.recipe.tier");
    /** Work time; the unit is the station's (ticks, or tanning's work units). */
    public static final RecipeMetadataKey<Float> TIME = RecipeMetadataKey
            .create("minefantasy2:time", Float.class, v -> v >= 0, "minefantasy2.recipe.time");
    /** Crafter tool type, such as "knife" or "hammer". */
    public static final RecipeMetadataKey<String> TOOL = RecipeMetadataKey
            .create("minefantasy2:tool", String.class, v -> !v.isEmpty(), "minefantasy2.recipe.tool");
    /** Research the player needs. */
    public static final RecipeMetadataKey<String> RESEARCH = RecipeMetadataKey
            .create("minefantasy2:research", String.class, v -> !v.isEmpty(), "minefantasy2.recipe.research");
    /** Crafter tool tier the recipe needs; -1 for any. */
    public static final RecipeMetadataKey<Integer> TOOL_TIER = RecipeMetadataKey
            .create("minefantasy2:tool_tier", Integer.class, v -> v >= -1, "minefantasy2.recipe.tool_tier");
    /** Whether the product leaves the station hot. */
    public static final RecipeMetadataKey<Boolean> HOT_OUTPUT = RecipeMetadataKey
            .create("minefantasy2:hot_output", Boolean.class, "minefantasy2.recipe.hot_output");
    /** Quern: whether grinding uses up the pot. */
    public static final RecipeMetadataKey<Boolean> CONSUME_POT = RecipeMetadataKey
            .create("minefantasy2:consume_pot", Boolean.class, "minefantasy2.recipe.consume_pot");
    /** Cooking: the heat the food starts cooking above. */
    public static final RecipeMetadataKey<Integer> MIN_TEMPERATURE = RecipeMetadataKey
            .create("minefantasy2:min_temperature", Integer.class, "minefantasy2.recipe.min_temperature");
    /** Cooking: the heat the food burns above, when it can burn. */
    public static final RecipeMetadataKey<Integer> MAX_TEMPERATURE = RecipeMetadataKey
            .create("minefantasy2:max_temperature", Integer.class, "minefantasy2.recipe.max_temperature");
    /** Cooking: whether too much heat burns the food. */
    public static final RecipeMetadataKey<Boolean> CAN_BURN = RecipeMetadataKey
            .create("minefantasy2:can_burn", Boolean.class, "minefantasy2.recipe.can_burn");

    private MFRecipeKeys() {}
}
