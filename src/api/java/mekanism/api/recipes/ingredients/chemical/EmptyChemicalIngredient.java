package mekanism.api.recipes.ingredients.chemical;

import com.mojang.serialization.MapCodec;
import java.util.stream.Stream;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.chemical.Chemical;

/**
 * Base Chemical ingredient implementation for a singleton that represents an empty chemical ingredient.
 * <p>
 * This is the only instance of an <b>explicitly</b> empty ingredient, and may be used as a fallback in ChemicalIngredient convenience methods (such as when trying to
 * create an ingredient from an empty list).
 *
 * @see mekanism.api.recipes.ingredients.creator.IChemicalIngredientCreator#empty()
 * @see ChemicalIngredient#isEmpty()
 * @since 10.6.0
 */
@NothingNullByDefault
public final class EmptyChemicalIngredient extends ChemicalIngredient {

    public static final EmptyChemicalIngredient INSTANCE = new EmptyChemicalIngredient();
    public static final MapCodec<EmptyChemicalIngredient> CODEC = MapCodec.unit(INSTANCE);

    private EmptyChemicalIngredient() {
    }

    @Override
    public boolean test(Chemical chemical) {
        return chemical.isEmptyType();
    }

    @Override
    public Stream<Chemical> generateChemicals() {
        return Stream.empty();
    }

    @Override
    public MapCodec<? extends ChemicalIngredient> codec() {
        return CODEC;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }
}
