package minefantasy.mf2.integration.nei;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.PositionedStack;
import minefantasy.mf2.api.crafting.anvil.CraftingManagerAnvil;
import minefantasy.mf2.api.crafting.anvil.CustomToolRecipe;
import minefantasy.mf2.api.crafting.anvil.IAnvilRecipe;
import minefantasy.mf2.api.crafting.anvil.ShapedAnvilRecipes;
import minefantasy.mf2.api.crafting.anvil.ShapelessAnvilRecipes;
import minefantasy.mf2.api.crafting.exotic.SpecialForging;
import minefantasy.mf2.api.heating.Heatable;
import minefantasy.mf2.api.heating.IHotItem;
import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.helpers.TextureHelperMF;
import minefantasy.mf2.api.material.CustomMaterial;
import minefantasy.mf2.item.list.ComponentListMF;
import minefantasy.mf2.knowledge.KnowledgeListMF;

public class RecipeHandlerAnvil extends MFNEIRecipeHandler {

    private static final int[][] SHAPELESS_STACK_ORDER = new int[][] { { 0, 0 }, { 1, 0 }, { 2, 0 }, { 0, 1 }, { 1, 1 },
            { 2, 1 }, { 0, 2 }, { 1, 2 }, { 2, 2 } };

    private static final int TOOL_ICON_X = 10;
    private static final int STATION_ICON_X = 32;
    private static final int ICON_Y = 18;

    public RecipeHandlerAnvil() {
        super("minefantasy2.anvil");
    }

    @Override
    public String getRecipeName() {
        return StatCollector.translateToLocal("method.anvil");
    }

    @Override
    public String getGuiTexture() {
        return "minefantasy2:textures/gui/knowledge/anvilGrid.png";
    }

    @Override
    public void drawBackground(int recipe) {
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GuiDraw.changeTexture(getGuiTexture());
        GuiDraw.drawTexturedModalRect(0, 0, 5, 22, 166, 147);
        ((CachedAnvilRecipe) arecipes.get(recipe)).drawSlotFrames();
    }

    @Override
    public int recipiesPerPage() {
        return 1;
    }

    @Override
    public void drawExtras(int recipe) {
        CachedAnvilRecipe cachedRecipe = (CachedAnvilRecipe) arecipes.get(recipe);
        cachedRecipe.drawHotOverlays(this);
    }

    @Override
    public List<String> handleItemTooltip(codechicken.nei.recipe.GuiRecipe<?> gui, ItemStack stack,
            List<String> currenttip, int recipe) {
        if (recipe >= 0 && recipe < arecipes.size()) {
            CachedAnvilRecipe cachedRecipe = (CachedAnvilRecipe) arecipes.get(recipe);
            if (cachedRecipe.hasSpecialCatalyst() && cachedRecipe.isOutputStack(stack)) {
                currenttip.add(
                        StatCollector.translateToLocalFormatted(
                                "nei.minefantasy2.special_output",
                                cachedRecipe.getSpecialCatalystName()));
            }
        }
        return currenttip;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void loadCraftingRecipes(ItemStack inputStack) {
        if (!NEIHelper.isValidStack(inputStack)) {
            return;
        }
        ItemStack hiddenStack = null;

        if (inputStack.getItem() instanceof IHotItem) {
            inputStack = Heatable.getItem(inputStack);
            if (!NEIHelper.isValidStack(inputStack)) {
                return;
            }
        }

        if (NEIHelper.canViewResearch(Minecraft.getMinecraft().thePlayer, KnowledgeListMF.smeltDragonforge)) {
            for (Map.Entry<Item, Item> entry : SpecialForging.dragonforgeCrafts.entrySet()) {
                if (CustomToolHelper.areEqual(new ItemStack(entry.getValue()), inputStack)) {
                    hiddenStack = CustomToolHelper.tryDeconstruct(new ItemStack(entry.getKey()), inputStack);
                }
            }
        }

        for (IAnvilRecipe irecipe : (List<IAnvilRecipe>) CraftingManagerAnvil.getInstance().getRecipeList()) {
            if (irecipe == null || !NEIHelper.isValidStack(irecipe.getRecipeOutput())) {
                continue;
            }
            ItemStack specialStack = getSpecialResultFor(irecipe.getRecipeOutput(), inputStack);
            if ((hiddenStack != null && CustomToolHelper.areEqual(irecipe.getRecipeOutput(), hiddenStack))
                    || CustomToolHelper.areEqual(irecipe.getRecipeOutput(), inputStack)
                    || CustomToolHelper.areEqual(specialStack, inputStack)) {
                if (NEIHelper.canViewResearch(Minecraft.getMinecraft().thePlayer, irecipe.getResearch())) {
                    CachedAnvilRecipe recipe = handleRecipe(
                            irecipe,
                            inputStack,
                            getSpecialCatalyst(irecipe, inputStack));

                    if (recipe == null) {
                        continue;
                    }

                    arecipes.add(recipe);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void loadAllRecipes() {
        for (IAnvilRecipe irecipe : (List<IAnvilRecipe>) CraftingManagerAnvil.getInstance().getRecipeList()) {
            if (irecipe == null || !NEIHelper.isValidStack(irecipe.getRecipeOutput())
                    || !NEIHelper.canViewResearch(Minecraft.getMinecraft().thePlayer, irecipe.getResearch())) {
                continue;
            }
            CachedAnvilRecipe recipe = handleRecipe(irecipe, null, null);
            if (recipe != null) {
                arecipes.add(recipe);
            }
        }
    }

    private ItemStack getSpecialResultFor(ItemStack baseOutput, ItemStack requestedOutput) {
        String design = CustomToolHelper.getCustomStyle(requestedOutput);
        if (design == null) {
            return null;
        }
        ItemStack specialOutput = createSpecialResult(design, baseOutput);
        if (specialOutput != null && !CustomToolHelper.hasAnyMaterial(specialOutput)
                && CustomToolHelper.hasAnyMaterial(requestedOutput)) {
            specialOutput = CustomToolHelper.tryDeconstruct(specialOutput, requestedOutput);
        }
        return specialOutput;
    }

    private ItemStack createSpecialResult(String design, ItemStack baseOutput) {
        if (!NEIHelper.isValidStack(baseOutput)) {
            return null;
        }
        Item special = SpecialForging.getSpecialCraft(design, baseOutput);
        if (special == null) {
            return null;
        }

        ItemStack specialOutput = new ItemStack(special, baseOutput.stackSize, baseOutput.getItemDamage());
        NBTBase nbt = !baseOutput.hasTagCompound() ? null : baseOutput.getTagCompound().copy();
        if (nbt != null) {
            specialOutput.setTagCompound((NBTTagCompound) nbt);
        }
        return specialOutput;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        if (!NEIHelper.isValidStack(ingredient)) {
            return;
        }
        if (ingredient.getItem() instanceof IHotItem) {
            ingredient = Heatable.getItem(ingredient);
            if (!NEIHelper.isValidStack(ingredient)) {
                return;
            }
        }

        if (NEIHelper.matchesCrafting(new ItemStack(ComponentListMF.ornate_items), ingredient)) {
            for (IAnvilRecipe irecipe : (List<IAnvilRecipe>) CraftingManagerAnvil.getInstance().getRecipeList()) {
                if (irecipe == null || !NEIHelper.isValidStack(irecipe.getRecipeOutput())) {
                    continue;
                }
                if (!NEIHelper.canViewResearch(Minecraft.getMinecraft().thePlayer, irecipe.getResearch())) {
                    continue;
                }
                if (SpecialForging.getSpecialCraft("ornate", irecipe.getRecipeOutput()) == null) {
                    continue;
                }
                ItemStack ornateOutput = createSpecialResult("ornate", irecipe.getRecipeOutput());
                CachedAnvilRecipe recipe = handleRecipe(
                        irecipe,
                        ornateOutput,
                        new ItemStack(ComponentListMF.ornate_items));
                if (recipe != null) {
                    arecipes.add(recipe);
                }
            }
            return;
        }

        for (IAnvilRecipe irecipe : (List<IAnvilRecipe>) CraftingManagerAnvil.getInstance().getRecipeList()) {
            if (irecipe == null || !NEIHelper.isValidStack(irecipe.getRecipeOutput())) {
                continue;
            }
            if (!NEIHelper.canViewResearch(Minecraft.getMinecraft().thePlayer, irecipe.getResearch())) {
                continue;
            }

            CachedAnvilRecipe recipe = handleRecipe(irecipe, null, null);
            if (recipe == null || !recipe.contains(recipe.ingredients, ingredient)) {
                continue;
            }

            recipe.setIngredientPermutation(recipe.ingredients, ingredient);
            arecipes.add(recipe);
        }
    }

    private ItemStack getSpecialCatalyst(IAnvilRecipe recipe, ItemStack requestedOutput) {
        String design = CustomToolHelper.getCustomStyle(requestedOutput);
        if (!"ornate".equals(design)) {
            return null;
        }
        if (recipe == null || !NEIHelper.isValidStack(recipe.getRecipeOutput())) {
            return null;
        }
        if (SpecialForging.getSpecialCraft(design, recipe.getRecipeOutput()) == null) {
            return null;
        }
        return new ItemStack(ComponentListMF.ornate_items);
    }

    private CachedAnvilRecipe handleRecipe(IAnvilRecipe irecipe, ItemStack inputStack, ItemStack specialCatalyst) {
        if (irecipe instanceof ShapedAnvilRecipes) {
            return new CachedAnvilRecipe((ShapedAnvilRecipes) irecipe, inputStack, specialCatalyst);
        }
        if (irecipe instanceof ShapelessAnvilRecipes) {
            return new CachedAnvilRecipe((ShapelessAnvilRecipes) irecipe, specialCatalyst);
        }

        return null;
    }

    // TODO: Implement wood permutations, add additional helper method for custom material support in crafting and usage
    // handlers
    private class CachedAnvilRecipe extends CachedRecipe {

        private final ArrayList<PositionedStack> ingredients = new ArrayList<PositionedStack>();
        private final ArrayList<PositionedStack> otherStacks = new ArrayList<PositionedStack>();
        private final ArrayList<int[]> hotSlots = new ArrayList<int[]>();
        private ItemStack inputStack;
        private ItemStack specialCatalyst;
        private IAnvilRecipe iAnvilRecipe;
        private String toolType;
        private int toolTier;
        private int anvilTier;

        private CachedAnvilRecipe(ShapedAnvilRecipes recipe, ItemStack inputStack, ItemStack specialCatalyst) {
            iAnvilRecipe = recipe;
            toolType = recipe.getToolType();
            toolTier = recipe.getRecipeHammer();
            anvilTier = recipe.getAnvil();
            if (inputStack != null) {
                this.inputStack = inputStack.copy();
                this.inputStack.stackSize = recipe.getRecipeOutput().stackSize;
            } else {
                this.inputStack = NEIHelper.validCopy(recipe.getRecipeOutput());
            }
            if (recipe instanceof CustomToolRecipe) {
                applyMaterialTiers();
            }
            addSpecialCatalyst(specialCatalyst);
            setShapedRecipeIngredients(recipe.recipeWidth, recipe.recipeHeight, recipe.recipeItems);
        }

        private CachedAnvilRecipe(ShapelessAnvilRecipes recipe, ItemStack specialCatalyst) {
            iAnvilRecipe = recipe;
            toolType = recipe.getToolType();
            toolTier = recipe.getRecipeHammer();
            anvilTier = recipe.getAnvil();
            inputStack = NEIHelper.validCopy(recipe.getRecipeOutput());
            addSpecialCatalyst(specialCatalyst);
            setShapelessRecipeIngredients(recipe.recipeItems);
        }

        /**
         * Custom tool recipes leave their tiers at -1 and the anvil takes them from the material at craft time
         * (CustomToolRecipe.modifyTiers), so read them off the result shown here the same way.
         */
        private void applyMaterialTiers() {
            CustomMaterial material = CustomToolHelper.getCustomPrimaryMaterial(inputStack);
            if (material == null) {
                material = CustomToolHelper.getCustomSecondaryMaterial(inputStack);
            }
            if (material == null) {
                return;
            }
            if (toolTier < 0) {
                toolTier = material.crafterTier;
            }
            if (anvilTier < 0) {
                anvilTier = material.crafterAnvilTier;
            }
        }

        private void addSpecialCatalyst(ItemStack specialCatalyst) {
            if (!NEIHelper.isValidStack(specialCatalyst)) {
                return;
            }
            // Shown cycling with the result in the output slot (see getResult), so no separate slot is needed.
            this.specialCatalyst = specialCatalyst.copy();
        }

        private void setShapedRecipeIngredients(int width, int height, Object[] items) {
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    if (items[y * width + x] == null) {
                        continue;
                    }

                    if (!(items[y * width + x] instanceof ItemStack)) {
                        continue;
                    }

                    ItemStack cachedStack = NEIHelper.displayCopy((ItemStack) items[y * width + x]);
                    if (cachedStack == null) {
                        continue;
                    }
                    NEIHelper.fillMaterials(iAnvilRecipe, cachedStack, inputStack);
                    PositionedStack stack = NEIHelper.positionedStack(cachedStack, 31 + x * 18, 54 + y * 18, false);
                    if (stack == null) {
                        continue;
                    }
                    stack.setMaxSize(1);
                    ingredients.add(stack);
                    if (Heatable.canHeatItem(cachedStack)) {
                        hotSlots.add(new int[] { 31 + x * 18, 54 + y * 18 });
                    }
                }
            }
        }

        private void setShapelessRecipeIngredients(List<?> items) {
            for (int ingred = 0; ingred < items.size() && ingred < SHAPELESS_STACK_ORDER.length; ingred++) {
                Object item = items.get(ingred);
                if (!(item instanceof ItemStack)) {
                    continue;
                }

                ItemStack cachedStack = NEIHelper.displayCopy((ItemStack) item);
                if (cachedStack == null) {
                    continue;
                }
                NEIHelper.fillMaterials(iAnvilRecipe, cachedStack, inputStack);
                PositionedStack stack = NEIHelper.positionedStack(
                        cachedStack,
                        31 + SHAPELESS_STACK_ORDER[ingred][0] * 18,
                        54 + SHAPELESS_STACK_ORDER[ingred][1] * 18,
                        false);
                if (stack == null) {
                    continue;
                }
                stack.setMaxSize(1);
                ingredients.add(stack);
                if (Heatable.canHeatItem(cachedStack)) {
                    hotSlots.add(
                            new int[] { 31 + SHAPELESS_STACK_ORDER[ingred][0] * 18,
                                    54 + SHAPELESS_STACK_ORDER[ingred][1] * 18 });
                }
            }
        }

        @Override
        public List<PositionedStack> getIngredients() {
            return getCycledIngredients(cycleticks / 20, ingredients);
        }

        private PositionedStack toolSlot;
        private PositionedStack stationSlot;
        private boolean slotsBuilt;

        /** Built on first use: the tool list walks the item registry once per tool type and tier */
        private void buildStationSlots() {
            if (slotsBuilt) {
                return;
            }
            slotsBuilt = true;
            toolSlot = NEIStationSlots.slot(NEIStationSlots.tools(toolType, toolTier), TOOL_ICON_X, ICON_Y);
            stationSlot = NEIStationSlots.slot(NEIStationSlots.anvils(anvilTier), STATION_ICON_X, ICON_Y);
        }

        @Override
        public List<PositionedStack> getOtherStacks() {
            buildStationSlots();
            ArrayList<PositionedStack> stacks = new ArrayList<PositionedStack>(otherStacks);
            for (PositionedStack slot : new PositionedStack[] { toolSlot, stationSlot }) {
                if (slot != null) {
                    NEIStationSlots.cycle(slot, cycleticks);
                    stacks.add(slot);
                }
            }
            return stacks;
        }

        private void drawSlotFrames() {
            buildStationSlots();
            NEIStationSlots.drawFrame(toolSlot);
            NEIStationSlots.drawFrame(stationSlot);
        }

        private boolean hasSpecialCatalyst() {
            return specialCatalyst != null;
        }

        private String getSpecialCatalystName() {
            return specialCatalyst == null ? "" : specialCatalyst.getDisplayName();
        }

        private boolean isOutputStack(ItemStack stack) {
            return CustomToolHelper.areEqual(stack, inputStack) || CustomToolHelper.areEqual(stack, specialCatalyst);
        }

        @Override
        public PositionedStack getResult() {
            // For ornate/special crafts, cycle the output slot between the result and the required catalyst.
            if (specialCatalyst != null && (cycleticks / 40) % 2 == 1) {
                return NEIHelper.positionedStack(specialCatalyst, 75, 20);
            }
            return NEIHelper.positionedStack(inputStack, 75, 20);
        }

        private void drawHotOverlays(RecipeHandlerAnvil handler) {
            boolean showingCatalyst = specialCatalyst != null && (cycleticks / 40) % 2 == 1;
            handler.drawHeatOverlay(75, 20, iAnvilRecipe.outputHot() && !showingCatalyst);
            for (int[] hotSlot : hotSlots) {
                handler.drawHeatOverlay(hotSlot[0], hotSlot[1], true);
            }
        }

    }

    private void drawHeatOverlay(int x, int y, boolean heatable) {
        if (!heatable) {
            return;
        }
        GL11.glPushMatrix();
        GL11.glColor3f(1F, 1F, 1F);
        mc().getTextureManager().bindTexture(TextureHelperMF.getResource("textures/gui/knowledge/anvilGrid.png"));
        GuiDraw.drawTexturedModalRect(x, y, 248, 0, 8, 8);
        GL11.glPopMatrix();
    }

    private Minecraft mc() {
        return Minecraft.getMinecraft();
    }
}
