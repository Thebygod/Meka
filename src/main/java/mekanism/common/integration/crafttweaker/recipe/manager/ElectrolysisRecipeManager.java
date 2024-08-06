package mekanism.common.integration.crafttweaker.recipe.manager;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.fluid.CTFluidIngredient;
import mekanism.api.recipes.ElectrolysisRecipe;
import mekanism.api.recipes.basic.BasicElectrolysisRecipe;
import mekanism.api.recipes.vanilla_input.SingleFluidRecipeInput;
import mekanism.common.integration.crafttweaker.CrTConstants;
import mekanism.common.integration.crafttweaker.CrTUtils;
import mekanism.common.integration.crafttweaker.chemical.CrTChemicalStack;
import mekanism.common.integration.crafttweaker.chemical.ICrTChemicalStack;
import mekanism.common.recipe.MekanismRecipeType;
import org.openzen.zencode.java.ZenCodeType;

@ZenRegister
@ZenCodeType.Name(CrTConstants.CLASS_RECIPE_MANAGER_SEPARATING)
public class ElectrolysisRecipeManager extends MekanismRecipeManager<SingleFluidRecipeInput, ElectrolysisRecipe> {

    public static final ElectrolysisRecipeManager INSTANCE = new ElectrolysisRecipeManager();

    private ElectrolysisRecipeManager() {
        super(MekanismRecipeType.SEPARATING);
    }

    /**
     * Adds a separating recipe that separates a fluid into two gases. Electrolytic Separators can process this recipe type.
     *
     * @param name             Name of the new recipe.
     * @param input            {@link CTFluidIngredient} representing the input of the recipe.
     * @param leftGasOutput    {@link ICrTChemicalStack} representing the left output of the recipe.
     * @param rightGasOutput   {@link ICrTChemicalStack} representing the right output of the recipe.
     * @param energyMultiplier Value representing the multiplier to the energy cost in relation to the configured hydrogen separating energy cost. This value must be
     *                         greater than or equal to one.
     */
    @ZenCodeType.Method
    public void addRecipe(String name, CTFluidIngredient input, ICrTChemicalStack leftGasOutput, ICrTChemicalStack rightGasOutput, long energyMultiplier) {
        addRecipe(name, makeRecipe(input, leftGasOutput, rightGasOutput, energyMultiplier));
    }

    /**
     * Adds a separating recipe that separates a fluid into two gases. Electrolytic Separators can process this recipe type.
     *
     * @param name           Name of the new recipe.
     * @param input          {@link CTFluidIngredient} representing the input of the recipe.
     * @param leftGasOutput  {@link ICrTChemicalStack} representing the left output of the recipe.
     * @param rightGasOutput {@link ICrTChemicalStack} representing the right output of the recipe.
     *
     * @apiNote {@code energyMultiplier} will default to one. If this value is specified it must be greater than or equal to one.
     */
    @ZenCodeType.Method
    public void addRecipe(String name, CTFluidIngredient input, ICrTChemicalStack leftGasOutput, ICrTChemicalStack rightGasOutput) {
        //TODO: If https://github.com/ZenCodeLang/ZenCode/issues/31 gets fixed, merge this back with the other addRecipe method using a ZC Optional
        addRecipe(name, makeRecipe(input, leftGasOutput, rightGasOutput, 1L));
    }

    /**
     * Creates a separating recipe that separates a fluid into two gases.
     *
     * @param input            {@link CTFluidIngredient} representing the input of the recipe.
     * @param leftGasOutput    {@link ICrTChemicalStack} representing the left output of the recipe. Will be validated as not empty.
     * @param rightGasOutput   {@link ICrTChemicalStack} representing the right output of the recipe. Will be validated as not empty.
     * @param energyMultiplier Value representing the multiplier to the energy cost in relation to the configured hydrogen separating energy cost. Will be validated to be
     *                         greater than or equal to one.
     */
    public final ElectrolysisRecipe makeRecipe(CTFluidIngredient input, ICrTChemicalStack leftGasOutput, ICrTChemicalStack rightGasOutput, long energyMultiplier) {
        if (energyMultiplier < 1L) {
            throw new IllegalArgumentException("Energy multiplier must be at least one! Multiplier: " + energyMultiplier);
        }
        return new BasicElectrolysisRecipe(CrTUtils.fromCrT(input), energyMultiplier, getAndValidateNotEmpty(leftGasOutput),
              getAndValidateNotEmpty(rightGasOutput));
    }

    @Override
    protected String describeOutputs(ElectrolysisRecipe recipe) {
        return CrTUtils.describeOutputs(recipe.getOutputDefinition(), output -> new CrTChemicalStack(output.left()) + " and " + new CrTChemicalStack(output.right()));
    }
}