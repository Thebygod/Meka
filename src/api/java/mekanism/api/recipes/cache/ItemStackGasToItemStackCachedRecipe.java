package mekanism.api.recipes.cache;

import java.util.function.LongSupplier;
import javax.annotation.ParametersAreNonnullByDefault;
import mekanism.api.annotations.FieldsAreNonnullByDefault;
import mekanism.api.annotations.NonNull;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.recipes.ItemStackGasToItemStackRecipe;
import mekanism.api.recipes.cache.chemical.ItemStackConstantChemicalToItemStackCachedRecipe;
import mekanism.api.recipes.inputs.IInputHandler;
import mekanism.api.recipes.inputs.ILongInputHandler;
import mekanism.api.recipes.inputs.chemical.GasStackIngredient;
import mekanism.api.recipes.outputs.IOutputHandler;
import net.minecraft.item.ItemStack;

/**
 * Base class to help implement handling of item gas to item recipes.
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
public class ItemStackGasToItemStackCachedRecipe<RECIPE extends ItemStackGasToItemStackRecipe> extends
      ItemStackConstantChemicalToItemStackCachedRecipe<Gas, GasStack, GasStackIngredient, RECIPE> {

    /**
     * @param recipe           Recipe.
     * @param itemInputHandler Item input handler.
     * @param gasInputHandler  Gas input handler.
     * @param gasUsage         Gas usage multiplier.
     * @param outputHandler    Output handler.
     */
    public ItemStackGasToItemStackCachedRecipe(RECIPE recipe, IInputHandler<@NonNull ItemStack> itemInputHandler,
          ILongInputHandler<@NonNull GasStack> gasInputHandler, LongSupplier gasUsage, IOutputHandler<@NonNull ItemStack> outputHandler) {
        super(recipe, itemInputHandler, gasInputHandler, gasUsage, outputHandler);
    }
}