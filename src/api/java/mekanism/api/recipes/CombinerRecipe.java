package mekanism.api.recipes;

import java.util.List;
import java.util.function.BiPredicate;
import mekanism.api.MekanismAPI;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Main Input: ItemStack
 * <br>
 * Secondary/Extra Input: ItemStack
 * <br>
 * Output: ItemStack
 *
 * @apiNote Combiners and Combining Factories can process this recipe type.
 */
@NothingNullByDefault
public abstract class CombinerRecipe extends MekanismRecipe implements BiPredicate<@NotNull ItemStack, @NotNull ItemStack> {

    private static final Holder<Item> COMBINER = DeferredHolder.create(Registries.ITEM, new ResourceLocation(MekanismAPI.MEKANISM_MODID, "combiner"));

    @Override
    public abstract boolean test(ItemStack input, ItemStack extra);

    /**
     * Gets the main input ingredient.
     */
    public abstract ItemStackIngredient getMainInput();

    /**
     * Gets the secondary input ingredient.
     */
    public abstract ItemStackIngredient getExtraInput();

    /**
     * Gets a new output based on the given inputs.
     *
     * @param input Specific input.
     * @param extra Specific secondary/extra input.
     *
     * @return New output.
     *
     * @apiNote While Mekanism does not currently make use of the inputs, it is important to support it and pass the proper value in case any addons define input based
     * outputs where things like NBT may be different.
     * @implNote The passed in inputs should <strong>NOT</strong> be modified.
     */
    @Contract(value = "_, _ -> new", pure = true)
    public abstract ItemStack getOutput(@NotNull ItemStack input, @NotNull ItemStack extra);

    @NotNull
    @Override
    public abstract ItemStack getResultItem(@NotNull RegistryAccess registryAccess);

    /**
     * For JEI, gets the output representations to display.
     *
     * @return Representation of the output, <strong>MUST NOT</strong> be modified.
     */
    public abstract List<ItemStack> getOutputDefinition();

    @Override
    public boolean isIncomplete() {
        return getMainInput().hasNoMatchingInstances() || getExtraInput().hasNoMatchingInstances();
    }

    @Override
    public final RecipeType<CombinerRecipe> getType() {
        return MekanismRecipeTypes.TYPE_COMBINING.value();
    }

    @Override
    public String getGroup() {
        return "combiner";
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(COMBINER);
    }
}
