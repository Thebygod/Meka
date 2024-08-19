package mekanism.api.recipes.ingredients.chemical;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import mekanism.api.SerializationConstants;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.chemical.Chemical;
import mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

/**
 * Base Chemical ingredient implementation that matches if all child ingredients match
 *
 * @since 10.6.0
 */
@NothingNullByDefault
public non-sealed class IntersectionChemicalIngredient extends ChemicalIngredient {

    public static final MapCodec<IntersectionChemicalIngredient> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
          IngredientCreatorAccess.chemical().listCodecMultipleElements().fieldOf(SerializationConstants.CHILDREN).forGetter(IntersectionChemicalIngredient::children)
    ).apply(builder, IntersectionChemicalIngredient::new));

    private final List<ChemicalIngredient> children;

    /**
     * @param children Ingredients to form an intersection from.
     */
    @Internal
    public IntersectionChemicalIngredient(List<ChemicalIngredient> children) {
        if (children.size() < 2) {
            throw new IllegalArgumentException("Intersection chemical ingredients require at least two ingredients");
        }
        this.children = children;
    }

    @Override
    public final boolean test(Chemical chemical) {
        for (ChemicalIngredient child : children) {
            if (!child.test(chemical)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public final Stream<Chemical> generateChemicals() {
        return children.stream()
              .flatMap(ChemicalIngredient::generateChemicals)
              .distinct()//Ensure we don't include the same chemical multiple times
              .filter(this);
    }

    /**
     * {@return all the child ingredients that this ingredient is an intersection of}
     */
    public final List<ChemicalIngredient> children() {
        return children;
    }

    @Override
    public int hashCode() {
        return Objects.hash(children);
    }

    @Override
    public MapCodec<? extends ChemicalIngredient> codec() {
        return CODEC;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return children.equals(((IntersectionChemicalIngredient) obj).children);
    }
}
