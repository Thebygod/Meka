package mekanism.common.integration.crafttweaker.recipe.manager;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import mekanism.api.recipes.ChemicalOxidizerRecipe;
import mekanism.api.recipes.basic.BasicChemicalOxidizerRecipe;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import mekanism.common.integration.crafttweaker.CrTConstants;
import mekanism.common.integration.crafttweaker.CrTUtils;
import mekanism.common.integration.crafttweaker.chemical.ICrTChemicalStack;
import mekanism.common.recipe.MekanismRecipeType;
import org.openzen.zencode.java.ZenCodeType;

@ZenRegister
@ZenCodeType.Name(CrTConstants.CLASS_RECIPE_MANAGER_OXIDIZING)
public class ChemicalOxidizerRecipeManager extends MekanismRecipeManager<ChemicalOxidizerRecipe> {

    public static final ChemicalOxidizerRecipeManager INSTANCE = new ChemicalOxidizerRecipeManager();

    private ChemicalOxidizerRecipeManager() {
        super(MekanismRecipeType.OXIDIZING);
    }

    /**
     * Adds an oxidizing recipe that converts an item into a chemical. Chemical Oxidizers can process this recipe type.
     *
     * @param name      Name of the new recipe.
     * @param input {@link ItemStackIngredient} representing the item input of the recipe.
     * @param output    {@link ICrTChemicalStack} representing the output of the recipe.
     */
    @ZenCodeType.Method
    public void addRecipe(String name, ItemStackIngredient input, ICrTChemicalStack<?, ?, ?> output) {
        addRecipe(name, makeRecipe(input, output));
    }

    /**
     * Creates an oxidizing recipe that converts an item into a chemical.
     *
     * @param input {@link ItemStackIngredient} representing the item input of the recipe.
     * @param output    {@link ICrTChemicalStack} representing the output of the recipe. Will be validated as not empty.
     */
    public final BasicChemicalOxidizerRecipe makeRecipe(ItemStackIngredient input, ICrTChemicalStack<?, ?, ?> output) {
        return new BasicChemicalOxidizerRecipe(input, getAndValidateNotEmpty(output));
    }

    @Override
    protected String describeOutputs(ChemicalOxidizerRecipe recipe) {
        return CrTUtils.describeOutputs(recipe.getOutputDefinition(), stack -> {
            ICrTChemicalStack<?, ?, ?> output = CrTUtils.fromBoxedStack(stack);
            if (output == null) {
                return "unknown chemical output";
            }
            return output.toString();
        });
    }
}