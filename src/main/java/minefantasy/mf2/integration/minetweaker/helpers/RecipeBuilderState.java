package minefantasy.mf2.integration.minetweaker.helpers;

import java.util.function.Supplier;

import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.api.recipe.RecipeRegistry;
import minetweaker.api.item.IItemStack;

/** Internal state shared by station builders; public ZenScript methods remain on each builder. */
public final class RecipeBuilderState {

    private final String name;
    private int priority;
    private boolean registered;

    public RecipeBuilderState(String name) {
        this.name = name;
    }

    public int priority() {
        return priority;
    }

    public void priority(int priority) {
        this.priority = priority;
    }

    public String registrationProblem() {
        return registered ? "builder is already registered" : null;
    }

    public void claim() {
        String problem = registrationProblem();
        if (problem != null) throw new IllegalArgumentException(problem);
        registered = true;
    }

    public void register(RecipeRegistry<ProcessRecipe> registry, Supplier<ProcessRecipe> recipe) {
        ScriptProcess.add(registry, name, recipe, priority);
    }

    public void replace(RecipeRegistry<ProcessRecipe> registry, String id, Supplier<ProcessRecipe> recipe) {
        ScriptProcess.replace(registry, id, recipe, priority);
    }

    public void replaceAccepting(RecipeRegistry<ProcessRecipe> registry, IItemStack stack,
            Supplier<ProcessRecipe> recipe, int expected) {
        ScriptProcess.replaceAccepting(registry, stack, recipe, priority, expected);
    }
}
