/**
 * The recipe core every MineFantasy station shares.
 *
 * <h2>Registries and ids</h2> Each station keeps its recipes in a {@link minefantasy.mf2.api.recipe.RecipeRegistry},
 * all of them owned by one {@link minefantasy.mf2.api.recipe.RecipeRegistries} ({@code MFRecipes.REGISTRIES}). A recipe
 * is a {@link minefantasy.mf2.api.recipe.RecipeEntry}: the recipe itself, a stable
 * {@link minefantasy.mf2.api.recipe.RecipeId} ({@code namespace:station/path}), its
 * {@link minefantasy.mf2.api.recipe.RecipeSource} and a priority. A station tries recipes by priority, highest first,
 * then in the order they were registered; ids never depend on that order, so saves and scripts can name a recipe.
 *
 * <h2>Layers and reloads</h2> While mods load, registrations go to the base layer. On load complete the registries
 * publish an immutable {@link minefantasy.mf2.api.recipe.RecipeSnapshot}, and stations only ever read snapshots. A
 * script reload starts a draft from the base layer, applies the scripts and publishes the draft, so a reload never
 * builds on the last one. Each script action is its own transaction: one that is refused changes nothing and the rest
 * are published; a reload that fails as a whole is dropped, leaving the published recipes as they were.
 *
 * <h2>Writing</h2> Every change goes through a {@link minefantasy.mf2.api.recipe.RecipeTransaction}: add, replace or
 * remove across any registries, applied together on commit or not at all. A registry refuses, with a
 * {@link minefantasy.mf2.api.recipe.RecipeRegistrationException}, a recipe that fails its own checks
 * ({@link minefantasy.mf2.api.recipe.RecipeChecks}) or asks for what the station cannot do: an amount or
 * {@link minefantasy.mf2.api.recipe.Usage} outside its {@link minefantasy.mf2.api.recipe.InputSupport}, a tier it does
 * not have, a research nobody can learn. Nothing is dropped silently.
 *
 * <pre>
 * try (RecipeTransaction tx = MFRecipes.REGISTRIES.begin(RecipeSource.NATIVE)) {
 *     tx.add(MFRecipes.QUERN, RecipeId.of("mymod", "quern/flour"), ProcessRecipe.of(Input.of(seeds), flour), 0);
 *     tx.commit();
 * }
 * </pre>
 *
 * <h2>Inputs</h2> An {@link minefantasy.mf2.api.recipe.Input} says what fits a slot (items, ore names, alternatives),
 * what it must be made of, how many it takes and what crafting does to it. NBT counts only where the input asks for it.
 *
 * <h2>Crafting</h2> A station finds its recipe ({@link minefantasy.mf2.api.recipe.RecipeLookup}) and works it out in
 * full as a {@link minefantasy.mf2.api.recipe.CraftPlan}: the requirements in force, what each slot pays and what comes
 * out. The plan is applied whole or not at all, and the station's progress belongs to that plan
 * ({@link minefantasy.mf2.api.recipe.RunningCraft}): replacing the recipe or swapping the inputs starts the work over.
 * {@link minefantasy.mf2.api.recipe.CheckResult} and {@link minefantasy.mf2.api.recipe.Diagnosis} explain why a station
 * does not craft, for its HUD and {@code /mf recipes}.
 */
package minefantasy.mf2.api.recipe;
