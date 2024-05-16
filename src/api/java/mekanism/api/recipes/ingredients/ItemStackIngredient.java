package mekanism.api.recipes.ingredients;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Objects;
import mekanism.api.annotations.NothingNullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base implementation for how Mekanism handle's ItemStack Ingredients.
 * <p>
 * Create instances of this using {@link mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess#item()}.
 *
 * @implNote This is a wrapper around {@link SizedIngredient}
 */
@NothingNullByDefault
public final class ItemStackIngredient implements InputIngredient<@NotNull ItemStack> {

    /**
     * A codec which can (de)encode item stack ingredients.
     *
     * @implNote This must be a lazily initialized so that this class can be loaded in tests
     * @since 10.6.0
     */
    public static final Codec<ItemStackIngredient> CODEC = Codec.lazyInitialized(() -> SizedIngredient.FLAT_CODEC.xmap(
          ItemStackIngredient::new, ItemStackIngredient::ingredient
    ));
    /**
     * A stream codec which can be used to encode and decode item stack ingredients over the network.
     *
     * @implNote This must be a lazily initialized so that this class can be loaded in tests
     * @since 10.6.0
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemStackIngredient> STREAM_CODEC = NeoForgeStreamCodecs.lazy(() ->
          SizedIngredient.STREAM_CODEC.map(ItemStackIngredient::new, ItemStackIngredient::ingredient)
    );

    /**
     * Creates an Item Stack Ingredient that matches a given ingredient and amount. Prefer calling via
     * {@link mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess#item()} and
     * {@link mekanism.api.recipes.ingredients.creator.IItemStackIngredientCreator#from(SizedIngredient)}.
     *
     * @param ingredient Sized ingredient to match.
     *
     * @throws NullPointerException     if the given instance is null.
     * @throws IllegalArgumentException if the given instance is empty.
     * @since 10.6.0
     */
    public static ItemStackIngredient of(SizedIngredient ingredient) {
        Objects.requireNonNull(ingredient, "ItemStackIngredients cannot be created from a null ingredient.");
        if (ingredient.ingredient().isEmpty()) {
            throw new IllegalArgumentException("ItemStackIngredients cannot be created using the empty ingredient.");
        }
        return new ItemStackIngredient(ingredient);
    }

    private final SizedIngredient ingredient;
    @Nullable
    private List<ItemStack> representations;

    private ItemStackIngredient(SizedIngredient ingredient) {
        this.ingredient = ingredient;
    }

    @Override
    public boolean test(ItemStack stack) {
        Objects.requireNonNull(stack);
        return ingredient.test(stack);
    }

    @Override
    public boolean testType(ItemStack stack) {
        Objects.requireNonNull(stack);
        return ingredient.ingredient().test(stack);
    }

    @Override
    public ItemStack getMatchingInstance(ItemStack stack) {
        return test(stack) ? stack.copyWithCount(ingredient.count()) : ItemStack.EMPTY;
    }

    @Override
    public long getNeededAmount(ItemStack stack) {
        return testType(stack) ? ingredient.count() : 0;
    }

    @Override
    public boolean hasNoMatchingInstances() {
        return ingredient.ingredient().hasNoItems();
    }

    @Override
    public List<@NotNull ItemStack> getRepresentations() {
        if (this.representations == null) {
            //TODO: See if quark or whatever mods used to occasionally have empty stacks in their ingredients still do
            // if so we probably should filter them out of this
            this.representations = List.of(ingredient.getItems());
        }
        return representations;
    }

    /**
     * For use in recipe input caching. Gets the internal Neo Sized Ingredient.
     *
     * @since 10.6.0
     */
    @Internal
    public SizedIngredient ingredient() {
        return ingredient;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ItemStackIngredient other = (ItemStackIngredient) o;
        //TODO - 1.20.5: Replace this once sized ingredient implements equals and hashcode
        return ingredient.count() == other.ingredient.count() && ingredient.ingredient().equals(other.ingredient.ingredient());
    }

    @Override
    public int hashCode() {
        //return ingredient.hashCode();
        return Objects.hash(ingredient.count(), ingredient.ingredient());
    }
}