package mekanism.api.recipes.ingredients.chemical;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import mekanism.api.MekanismAPI;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.chemical.Chemical;
import mekanism.api.recipes.ingredients.creator.IChemicalIngredientCreator;
import mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;

/**
 * This class serves as the chemical analogue of an item {@link Ingredient}, that is, a representation of both a {@linkplain #test predicate} to test {@link Chemical}s
 * against, and a {@linkplain #getChemicals list} of matching chemicals for e.g. display purposes.
 *
 * @see mekanism.api.recipes.ingredients.ChemicalStackIngredient
 * @since 10.6.0
 */
@NothingNullByDefault
public abstract sealed class ChemicalIngredient implements Predicate<Chemical> permits CompoundChemicalIngredient, DifferenceChemicalIngredient,
      EmptyChemicalIngredient, IntersectionChemicalIngredient, SingleChemicalIngredient, TagChemicalIngredient {

    @Nullable
    private List<Chemical> chemicals;

    /**
     * Checks if a given chemical matches this ingredient.
     *
     * @param chemical the chemical to test
     *
     * @return {@code true} if the chemical matches, {@code false} otherwise
     */
    @Override
    public abstract boolean test(Chemical chemical);

    /**
     * Generates a stream of all chemicals this ingredient matches against.
     * <p>
     * Unlike fluid and item ingredients, as chemicals have no data components, this should be exhaustive and perfectly accurate.
     * <ul>
     * <li>It is important that the returned chemicals correspond exactly to all the accepted {@link Chemical}s.</li>
     * <li>At least one chemical should always be returned, otherwise the ingredient may be considered {@linkplain #hasNoChemicals()} () accidentally empty}.</li>
     * </ul>
     *
     * @return a stream of all chemicals this ingredient accepts.
     * <p>
     *
     * @see ICustomIngredient#getItems()
     */
    public abstract Stream<Chemical> generateChemicals();

    /**
     * {@return a list of chemicals this ingredient accepts}
     *
     * @see #generateChemicals()
     */
    public final List<Chemical> getChemicals() {
        if (chemicals == null) {
            chemicals = generateChemicals().toList();
        }
        return chemicals;
    }

    /**
     * Checks if this ingredient is <b>explicitly empty</b>, i.e. equal to {@link IChemicalIngredientCreator#empty()}.
     * <p> Note: This does <i>not</i> return true for "accidentally empty" ingredients,
     * including compound ingredients that are explicitly constructed with no children or intersection / difference ingredients that resolve to an empty set.
     *
     * @return {@code true} if this ingredient is {@link IChemicalIngredientCreator#empty()}, {@code false} otherwise
     */
    public final boolean isEmpty() {
        return this == IngredientCreatorAccess.chemical().empty();
    }

    /**
     * Checks if this ingredient matches no chemicals, i.e. if its list of {@linkplain #getChemicals() matching chemicals} is empty.
     * <p>
     * Note that this method explicitly <b>resolves</b> the ingredient; if this is not desired, you will need to check for emptiness another way!
     *
     * @return {@code true} if this ingredient matches no chemicals, {@code false} otherwise
     *
     * @see #isEmpty()
     */
    public final boolean hasNoChemicals() {
        return getChemicals().isEmpty();
    }

    /**
     * {@return The type of this chemical ingredient.}
     *
     * <p>The type <b>must</b> be registered to the corresponding type register.
     *
     * @see MekanismAPI#CHEMICAL_INGREDIENT_TYPES
     */
    public abstract MapCodec<? extends ChemicalIngredient> codec();

    //Force overriding
    @Override
    public abstract int hashCode();

    //Force overriding
    @Override
    public abstract boolean equals(Object obj);
}