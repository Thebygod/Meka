package mekanism.api.recipes.basic;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.recipes.ItemStackGasToItemStackRecipe;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient.GasStackIngredient;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@NothingNullByDefault
public abstract class BasicItemStackGasToItemStackRecipe extends ItemStackGasToItemStackRecipe implements IBasicItemStackOutput {

    protected final ItemStackIngredient itemInput;
    protected final GasStackIngredient chemicalInput;
    protected final ItemStack output;

    /**
     * @param itemInput Item input.
     * @param gasInput  Gas input.
     * @param output    Output.
     */
    public BasicItemStackGasToItemStackRecipe(ItemStackIngredient itemInput, GasStackIngredient gasInput, ItemStack output, RecipeType<ItemStackGasToItemStackRecipe> recipeType) {
        super(recipeType);
        this.itemInput = Objects.requireNonNull(itemInput, "Item input cannot be null.");
        this.chemicalInput = Objects.requireNonNull(gasInput, "Chemical input cannot be null.");
        Objects.requireNonNull(output, "Output cannot be null.");
        if (output.isEmpty()) {
            throw new IllegalArgumentException("Output cannot be empty.");
        }
        this.output = output.copy();
    }

    @Override
    public ItemStackIngredient getItemInput() {
        return itemInput;
    }

    @Override
    public GasStackIngredient getChemicalInput() {
        return chemicalInput;
    }

    @Override
    @Contract(value = "_, _ -> new", pure = true)
    public ItemStack getOutput(ItemStack inputItem, GasStack inputChemical) {
        return output.copy();
    }

    @NotNull
    @Override
    public ItemStack getResultItem(@NotNull RegistryAccess registryAccess) {
        return output.copy();
    }

    @Override
    public boolean test(ItemStack itemStack, GasStack gasStack) {
        return itemInput.test(itemStack) && chemicalInput.test(gasStack);
    }

    @Override
    public List<@NotNull ItemStack> getOutputDefinition() {
        return Collections.singletonList(output);
    }

    @Override
    public ItemStack getOutputRaw() {
        return output;
    }
}