package mekanism.api.recipes.ingredients.chemical;

import com.mojang.serialization.MapCodec;
import java.util.stream.Stream;
import mekanism.api.MekanismAPI;
import mekanism.api.SerializationConstants;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.chemical.Chemical;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.Nullable;

/**
 * Base Chemical ingredient implementation that matches all chemicals within the given tag.
 * <p>
 * Unlike with ingredients, this is an explicit "type" of chemical ingredient, though it may still be written without a type field, see
 * {@link mekanism.api.recipes.ingredients.creator.IChemicalIngredientCreator#mapCodecNonEmpty}
 *
 * @since 10.6.0
 */
@NothingNullByDefault
public non-sealed class TagChemicalIngredient extends ChemicalIngredient {

    public static final MapCodec<TagChemicalIngredient> CODEC = TagKey.codec(MekanismAPI.CHEMICAL_REGISTRY_NAME).xmap(
          TagChemicalIngredient::new,
          TagChemicalIngredient::tag
    ).fieldOf(SerializationConstants.TAG);

    private final TagKey<Chemical> tag;

    /**
     * @param tag Tag key to match.
     */
    public TagChemicalIngredient(TagKey<Chemical> tag) {
        this.tag = tag;
    }

    @Override
    public final boolean test(Chemical chemical) {
        return chemical.is(tag());
    }

    @Override
    public final Stream<Chemical> generateChemicals() {
        return MekanismAPI.CHEMICAL_REGISTRY.getTag(tag())
              .stream()
              .flatMap(HolderSet::stream)
              .map(Holder::value);
    }

    @Override
    public MapCodec<? extends ChemicalIngredient> codec() {
        return CODEC;
    }

    /**
     * {@return tag key to match}
     */
    public final TagKey<Chemical> tag() {
        return tag;
    }

    @Override
    public int hashCode() {
        return tag.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return tag.equals(((TagChemicalIngredient) obj).tag);
    }
}
