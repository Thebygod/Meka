package mekanism.common.integration.crafttweaker.recipe.manager;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.fluid.CTFluidIngredient;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.FluidSlurryToSlurryRecipe;
import mekanism.api.recipes.basic.BasicFluidSlurryToSlurryRecipe;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient;
import mekanism.api.recipes.vanilla_input.SingleFluidChemicalRecipeInput;
import mekanism.common.integration.crafttweaker.CrTConstants;
import mekanism.common.integration.crafttweaker.CrTUtils;
import mekanism.common.integration.crafttweaker.chemical.ICrTChemicalStack;
import mekanism.common.recipe.IMekanismRecipeTypeProvider;
import mekanism.common.recipe.MekanismRecipeType;
import org.openzen.zencode.java.ZenCodeType;

@ZenRegister
@ZenCodeType.Name(CrTConstants.CLASS_RECIPE_MANAGER_FLUID_SLURRY_TO_SLURRY)
public abstract class FluidSlurryToSlurryRecipeManager extends MekanismRecipeManager<SingleFluidChemicalRecipeInput, FluidSlurryToSlurryRecipe> {

    protected FluidSlurryToSlurryRecipeManager(IMekanismRecipeTypeProvider<SingleFluidChemicalRecipeInput, FluidSlurryToSlurryRecipe, ?> recipeType) {
        super(recipeType);
    }

    /**
     * Adds a recipe that converts a fluid and slurry to another slurry.
     * <br>
     * If this is called from the washing recipe manager, this will be a washing recipe and able to be processed in a chemical washer.
     *
     * @param name        Name of the new recipe.
     * @param fluidInput  {@link CTFluidIngredient} representing the fluid input of the recipe.
     * @param slurryInput {@link ChemicalStackIngredient} representing the slurry input of the recipe.
     * @param output      {@link ICrTChemicalStack} representing the output of the recipe.
     */
    @ZenCodeType.Method
    public void addRecipe(String name, CTFluidIngredient fluidInput, ChemicalStackIngredient slurryInput, ICrTChemicalStack output) {
        addRecipe(name, makeRecipe(fluidInput, slurryInput, output));
    }

    /**
     * Creates a recipe that converts a fluid and slurry to another slurry.
     *
     * @param fluidInput  {@link CTFluidIngredient} representing the fluid input of the recipe.
     * @param slurryInput {@link ChemicalStackIngredient} representing the slurry input of the recipe.
     * @param output      {@link ICrTChemicalStack} representing the output of the recipe. Will be validated as not empty.
     */
    public final FluidSlurryToSlurryRecipe makeRecipe(CTFluidIngredient fluidInput, ChemicalStackIngredient slurryInput, ICrTChemicalStack output) {
        return makeRecipe(fluidInput, slurryInput, getAndValidateNotEmpty(output));
    }

    protected abstract FluidSlurryToSlurryRecipe makeRecipe(CTFluidIngredient fluidInput, ChemicalStackIngredient slurryInput, ChemicalStack output);

    @Override
    protected String describeOutputs(FluidSlurryToSlurryRecipe recipe) {
        return CrTUtils.describeOutputs(recipe.getOutputDefinition());
    }

    @ZenRegister
    @ZenCodeType.Name(CrTConstants.CLASS_RECIPE_MANAGER_WASHING)
    public static class ChemicalWasherRecipeManager extends FluidSlurryToSlurryRecipeManager {

        public static final ChemicalWasherRecipeManager INSTANCE = new ChemicalWasherRecipeManager();

        private ChemicalWasherRecipeManager() {
            super(MekanismRecipeType.WASHING);
        }

        @Override
        protected FluidSlurryToSlurryRecipe makeRecipe(CTFluidIngredient fluidInput, ChemicalStackIngredient slurryInput, ChemicalStack output) {
            return new BasicFluidSlurryToSlurryRecipe(CrTUtils.fromCrT(fluidInput), slurryInput, output);
        }
    }
}