package mekanism.common.integration.projecte.mappers;

import java.util.List;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.PressurizedReactionRecipe;
import mekanism.api.recipes.PressurizedReactionRecipe.PressurizedReactionRecipeOutput;
import mekanism.common.integration.projecte.IngredientHelper;
import mekanism.common.integration.projecte.NSSChemical;
import mekanism.common.recipe.MekanismRecipeType;
import moze_intel.projecte.api.mapper.collector.IMappingCollector;
import moze_intel.projecte.api.mapper.recipe.RecipeTypeMapper;
import moze_intel.projecte.api.nss.NSSFluid;
import moze_intel.projecte.api.nss.NSSItem;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

@RecipeTypeMapper
public class PressurizedReactionRecipeMapper extends TypedMekanismRecipeMapper<PressurizedReactionRecipe> {

    public PressurizedReactionRecipeMapper() {
        super(PressurizedReactionRecipe.class, MekanismRecipeType.REACTION);
    }

    @Override
    public String getName() {
        return "MekPressurizedReaction";
    }

    @Override
    public String getDescription() {
        return "Maps Mekanism pressurized reaction recipes.";
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    protected boolean handleRecipe(IMappingCollector<NormalizedSimpleStack, Long> mapper, PressurizedReactionRecipe recipe) {
        boolean handled = false;
        List<@NotNull ItemStack> itemRepresentations = recipe.getInputSolid().getRepresentations();
        List<@NotNull FluidStack> fluidRepresentations = recipe.getInputFluid().getRepresentations();
        List<@NotNull ChemicalStack> gasRepresentations = recipe.getInputGas().getRepresentations();
        for (ItemStack itemRepresentation : itemRepresentations) {
            NormalizedSimpleStack nssItem = NSSItem.createItem(itemRepresentation);
            for (FluidStack fluidRepresentation : fluidRepresentations) {
                NormalizedSimpleStack nssFluid = NSSFluid.createFluid(fluidRepresentation);
                for (ChemicalStack gasRepresentation : gasRepresentations) {
                    NormalizedSimpleStack nssGas = NSSChemical.createChemical(gasRepresentation);
                    PressurizedReactionRecipeOutput output = recipe.getOutput(itemRepresentation, fluidRepresentation, gasRepresentation);
                    ItemStack itemOutput = output.item();
                    ChemicalStack gasOutput = output.gas();
                    IngredientHelper ingredientHelper = new IngredientHelper(mapper);
                    ingredientHelper.put(nssItem, itemRepresentation.getCount());
                    ingredientHelper.put(nssFluid, fluidRepresentation.getAmount());
                    ingredientHelper.put(nssGas, gasRepresentation.getAmount());
                    if (itemOutput.isEmpty()) {
                        //We only have a gas output
                        if (!gasOutput.isEmpty() && ingredientHelper.addAsConversion(gasOutput)) {
                            handled = true;
                        }
                    } else if (gasOutput.isEmpty()) {
                        //We only have an item output
                        if (ingredientHelper.addAsConversion(itemOutput)) {
                            handled = true;
                        }
                    } else {
                        NormalizedSimpleStack nssItemOutput = NSSItem.createItem(itemOutput);
                        NormalizedSimpleStack nssGasOutput = NSSChemical.createChemical(gasOutput);
                        //We have both so do our best guess
                        //Add trying to calculate the item output (using it as if we needed negative of gas output)
                        ingredientHelper.put(nssGasOutput, -gasOutput.getAmount());
                        if (ingredientHelper.addAsConversion(nssItemOutput, itemOutput.getCount())) {
                            handled = true;
                        }
                        //Add trying to calculate gas output (using it as if we needed negative of item output)
                        ingredientHelper.resetHelper();
                        ingredientHelper.put(nssItem, itemRepresentation.getCount());
                        ingredientHelper.put(nssFluid, fluidRepresentation.getAmount());
                        ingredientHelper.put(nssGas, gasRepresentation.getAmount());
                        ingredientHelper.put(nssItemOutput, -itemOutput.getCount());
                        if (ingredientHelper.addAsConversion(nssGasOutput, gasOutput.getAmount())) {
                            handled = true;
                        }
                    }
                }
            }
        }
        return handled;
    }
}