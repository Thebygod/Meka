package mekanism.common.integration.projecte.mappers;

import java.util.List;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.ChemicalDissolutionRecipe;
import mekanism.common.integration.projecte.IngredientHelper;
import mekanism.common.integration.projecte.NSSChemical;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.tile.prefab.TileEntityAdvancedElectricMachine;
import moze_intel.projecte.api.mapper.collector.IMappingCollector;
import moze_intel.projecte.api.mapper.recipe.RecipeTypeMapper;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

@RecipeTypeMapper
public class ChemicalDissolutionRecipeMapper extends TypedMekanismRecipeMapper<ChemicalDissolutionRecipe> {

    public ChemicalDissolutionRecipeMapper() {
        super(ChemicalDissolutionRecipe.class, MekanismRecipeType.DISSOLUTION);
    }

    @Override
    public String getName() {
        return "MekDissolution";
    }

    @Override
    public String getDescription() {
        return "Maps Mekanism dissolution recipes.";
    }

    @Override
    protected boolean handleRecipe(IMappingCollector<NormalizedSimpleStack, Long> mapper, ChemicalDissolutionRecipe recipe) {
        boolean handled = false;
        List<@NotNull ItemStack> itemRepresentations = recipe.getItemInput().getRepresentations();
        List<@NotNull ChemicalStack> chemicalRepresentations = recipe.getChemicalInput().getRepresentations();
        for (ChemicalStack chemicalRepresentation : chemicalRepresentations) {
            NSSChemical nssChemical = NSSChemical.createChemical(chemicalRepresentation);
            long chemicalAmount = chemicalRepresentation.getAmount();
            if (recipe.perTickUsage()) {
                chemicalAmount *= TileEntityAdvancedElectricMachine.BASE_TICKS_REQUIRED;
            }
            for (ItemStack itemRepresentation : itemRepresentations) {
                ChemicalStack output = recipe.getOutput(itemRepresentation, chemicalRepresentation);
                if (!output.isEmpty()) {
                    IngredientHelper ingredientHelper = new IngredientHelper(mapper);
                    ingredientHelper.put(itemRepresentation);
                    ingredientHelper.put(nssChemical, chemicalAmount);
                    if (ingredientHelper.addAsConversion(output)) {
                        handled = true;
                    }
                }
            }
        }
        return handled;
    }
}