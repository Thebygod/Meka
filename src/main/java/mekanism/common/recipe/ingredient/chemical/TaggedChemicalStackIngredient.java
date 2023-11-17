package mekanism.common.recipe.ingredient.chemical;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import mekanism.api.JsonConstants;
import mekanism.api.SerializerHelper;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient;
import mekanism.common.recipe.ingredient.chemical.ChemicalIngredientDeserializer.IngredientType;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;

public abstract class TaggedChemicalStackIngredient<CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>>
      implements ChemicalStackIngredient<CHEMICAL, STACK> {

    public static <CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>, CLAZZ extends TaggedChemicalStackIngredient<CHEMICAL, STACK>> Codec<CLAZZ>
    makeCodec(ResourceKey<? extends Registry<CHEMICAL>> registry, BiFunction<TagKey<CHEMICAL>, Long, CLAZZ> constructor) {
        return RecordCodecBuilder.create(instance -> instance.group(
              TagKey.codec(registry).fieldOf(JsonConstants.TAG).forGetter(TaggedChemicalStackIngredient::getTag),
              SerializerHelper.POSITIVE_NONZERO_LONG_CODEC.fieldOf(JsonConstants.AMOUNT).forGetter(TaggedChemicalStackIngredient::getRawAmount)
        ).apply(instance, constructor));
    }

    @NotNull
    private final HolderSet.Named<CHEMICAL> tag;
    private final long amount;

    protected TaggedChemicalStackIngredient(@NotNull HolderSet.Named<CHEMICAL> tag, long amount) {
        this.tag = tag;
        this.amount = amount;
    }

    protected abstract ChemicalIngredientInfo<CHEMICAL, STACK> getIngredientInfo();

    @Override
    public boolean test(@NotNull STACK chemicalStack) {
        return testType(chemicalStack) && chemicalStack.getAmount() >= amount;
    }

    @Override
    public boolean testType(@NotNull STACK chemicalStack) {
        return testType(Objects.requireNonNull(chemicalStack).getType());
    }

    @Override
    public boolean testType(@NotNull CHEMICAL chemical) {
        return Objects.requireNonNull(chemical).is(getTag());
    }

    @NotNull
    @Override
    public STACK getMatchingInstance(@NotNull STACK chemicalStack) {
        if (test(chemicalStack)) {
            //Our chemical is in the tag, so we make a new stack with the given amount
            return getIngredientInfo().createStack(chemicalStack, amount);
        }
        return getIngredientInfo().getEmptyStack();
    }

    @Override
    public long getNeededAmount(@NotNull STACK stack) {
        return testType(stack) ? amount : 0;
    }

    public long getRawAmount() {
        return this.amount;
    }

    @Override
    public boolean hasNoMatchingInstances() {
        return tag.size() == 0;
    }

    @NotNull
    @Override
    public List<@NotNull STACK> getRepresentations() {
        ChemicalIngredientInfo<CHEMICAL, STACK> ingredientInfo = getIngredientInfo();
        //TODO: Can this be cached somehow
        List<@NotNull STACK> representations = new ArrayList<>();
        for (Holder<CHEMICAL> chemical : tag) {
            representations.add(ingredientInfo.createStack(chemical.value(), amount));
        }
        return representations;
    }

    /**
     * For use in recipe input caching.
     */
    public Iterable<Holder<CHEMICAL>> getRawInput() {
        return tag;
    }

    public TagKey<CHEMICAL> getTag() {
        return tag.key();
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeEnum(IngredientType.TAGGED);
        buffer.writeResourceLocation(getTag().location());
        buffer.writeVarLong(amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TaggedChemicalStackIngredient<?, ?> other = (TaggedChemicalStackIngredient<?, ?>) o;
        return amount == other.amount && tag.equals(other.tag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tag, amount);
    }
}