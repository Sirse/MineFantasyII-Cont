/**
 * The MineFantasy stations and how mods give them recipes.
 *
 * <h2>Stations</h2> {@link minefantasy.mf2.api.crafting.MFRecipes} holds one registry per station, each documented with
 * the metadata keys ({@link minefantasy.mf2.api.crafting.MFRecipeKeys}) it reads. The anvil, carpenter's bench and
 * kitchen bench take {@link minefantasy.mf2.api.crafting.GridRecipe}s. One with
 * {@link minefantasy.mf2.api.crafting.GridRecipe.Tiers#MATERIAL} gives its result the metal and wood of the parts put
 * in: a tier left at -1 is the material's, the time scales with it, and a result smithed from metal needs the research
 * of smelting it. {@link minefantasy.mf2.api.crafting.Requirements} is how each station treats a tool or station below
 * the tier a recipe needs: refused, or only harder.
 *
 * <h2>Registering from code</h2> Register in init or postInit through the builders on
 * {@link minefantasy.mf2.api.MineFantasyAPI}. Each names the recipe after what it makes or takes
 * ({@link minefantasy.mf2.api.crafting.NativeRecipes}), so the id stays the same whatever else is registered; wrap
 * alternative recipes for the same thing in {@link minefantasy.mf2.api.crafting.NativeRecipes#variant} to keep them
 * apart.
 *
 * <pre>
 * MineFantasyAPI.anvilRecipe(new ItemStack(plate)).skill(SkillList.artisanry).tool("hammer", 2).stationTier(2)
 *         .time(300).hot().shaped("II", "II", 'I', steelBar);
 * MineFantasyAPI.carpenterRecipe(new ItemStack(shelf)).tool("knife", 0).time(50).shapeless(plank, plank);
 * MineFantasyAPI.alloyRecipe(bronzeBar).level(1).ratio(3).of(copper, copper, copper, tin);
 * MineFantasyAPI.cookingRecipe(new ItemStack(dough), new ItemStack(bread)).temperature(150, 250).time(60).oven()
 *         .register();
 * </pre>
 *
 * A pattern is read as by {@link minefantasy.mf2.api.crafting.RecipePattern}: rows, then a character and its item,
 * block or stack; ore names go through {@link minefantasy.mf2.api.recipe.Input} instead.
 *
 * <h2>Reading</h2> Read what a station would use from the published snapshots, {@code MFRecipes.ANVIL.published()},
 * never from the working layer: a script reload may be building it.
 */
package minefantasy.mf2.api.crafting;
