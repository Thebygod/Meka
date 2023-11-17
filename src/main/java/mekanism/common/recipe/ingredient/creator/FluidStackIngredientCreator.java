package mekanism.common.recipe.ingredient.creator;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import mekanism.api.JsonConstants;
import mekanism.api.SerializerHelper;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.recipes.ingredients.FluidStackIngredient;
import mekanism.api.recipes.ingredients.InputIngredient;
import mekanism.api.recipes.ingredients.creator.IFluidStackIngredientCreator;
import mekanism.common.network.BasePacketHandler;
import mekanism.common.recipe.ingredient.IMultiIngredient;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

@NothingNullByDefault
public class FluidStackIngredientCreator implements IFluidStackIngredientCreator {

    public static final FluidStackIngredientCreator INSTANCE = new FluidStackIngredientCreator();

    private static final Codec<FluidStackIngredient> SINGLE_CODEC = Codec.either(SingleFluidStackIngredient.CODEC, TaggedFluidStackIngredient.CODEC).xmap(
          either->either.map(Function.identity(), Function.identity()),
          input->{
              if (input instanceof SingleFluidStackIngredient stack) {
                  return Either.left(stack);
              }
              return Either.right((TaggedFluidStackIngredient) input);
          }
    );

    private static final Codec<FluidStackIngredient> CODEC = Codec.either(SINGLE_CODEC, MultiFluidStackIngredient.CODEC).xmap(
          either->either.map(Function.identity(), multi->{
              //unbox if we only got one
              if (multi.ingredients.length == 1) {
                  return multi.ingredients[0];
              }
              return multi;
          }),
          input->{
              if (input instanceof MultiFluidStackIngredient multi) {
                  return Either.right(multi);
              }
              return Either.left(input);
          }
    );

    private FluidStackIngredientCreator() {
    }

    @Override
    public Codec<FluidStackIngredient> codec() {
        return CODEC;
    }

    @Override
    public FluidStackIngredient from(FluidStack instance) {
        Objects.requireNonNull(instance, "FluidStackIngredients cannot be created from a null FluidStack.");
        if (instance.isEmpty()) {
            throw new IllegalArgumentException("FluidStackIngredients cannot be created using the empty stack.");
        }
        //Copy the stack to ensure it doesn't get modified afterwards
        return new SingleFluidStackIngredient(instance.copy());
    }

    @Override
    public FluidStackIngredient from(TagKey<Fluid> tag, int amount) {
        Objects.requireNonNull(tag, "FluidStackIngredients cannot be created from a null tag.");
        if (amount <= 0) {
            throw new IllegalArgumentException("FluidStackIngredients must have an amount of at least one. Received size was: " + amount);
        }
        return new TaggedFluidStackIngredient(tag, amount);
    }

    @Override
    public FluidStackIngredient read(FriendlyByteBuf buffer) {
        Objects.requireNonNull(buffer, "FluidStackIngredients cannot be read from a null packet buffer.");
        return switch (buffer.readEnum(IngredientType.class)) {
            case SINGLE -> from(FluidStack.readFromPacket(buffer));
            case TAGGED -> from(FluidTags.create(buffer.readResourceLocation()), buffer.readVarInt());
            case MULTI -> createMulti(BasePacketHandler.readArray(buffer, FluidStackIngredient[]::new, this::read));
        };
    }

    /**
     * {@inheritDoc}
     *
     * @implNote Converts a stream of ingredients into a single ingredient by converting the stream to an array and calling {@link #createMulti(FluidStackIngredient[])}.
     */
    @Override
    public FluidStackIngredient createMulti(FluidStackIngredient... ingredients) {
        Objects.requireNonNull(ingredients, "Cannot create a multi ingredient out of a null array.");
        if (ingredients.length == 0) {
            throw new IllegalArgumentException("Cannot create a multi ingredient out of no ingredients.");
        } else if (ingredients.length == 1) {
            return ingredients[0];
        }
        List<FluidStackIngredient> cleanedIngredients = new ArrayList<>();
        for (FluidStackIngredient ingredient : ingredients) {
            if (ingredient instanceof MultiFluidStackIngredient multi) {
                //Don't worry about if our inner ingredients are multi as well, as if this is the only external method for
                // creating a multi ingredient, then we are certified they won't be of a higher depth
                Collections.addAll(cleanedIngredients, multi.ingredients);
            } else {
                cleanedIngredients.add(ingredient);
            }
        }
        //There should be more than a single fluid, or we would have split out earlier
        return new MultiFluidStackIngredient(cleanedIngredients.toArray(new FluidStackIngredient[0]));
    }

    @Override
    public FluidStackIngredient from(Stream<FluidStackIngredient> ingredients) {
        return createMulti(ingredients.toArray(FluidStackIngredient[]::new));
    }

    @NothingNullByDefault
    public static class SingleFluidStackIngredient extends FluidStackIngredient {

        //Note: This must be a lazily initialized so that this class can be loaded in tests
        static final Codec<SingleFluidStackIngredient> CODEC = ExtraCodecs.lazyInitializedCodec(() -> SerializerHelper.FLUIDSTACK_CODEC.xmap(SingleFluidStackIngredient::new,
              SingleFluidStackIngredient::getInputRaw));

        private final List<@NotNull FluidStack> representations;
        private final FluidStack fluidInstance;

        private SingleFluidStackIngredient(FluidStack fluidInstance) {
            this.fluidInstance = Objects.requireNonNull(fluidInstance);
            //Note: While callers of getRepresentations aren't supposed to mutate it we copy it anyway so that in case they do
            // then nothing bad happens to the actual recipe
            this.representations = Collections.singletonList(this.fluidInstance.copy());
        }

        @Override
        public boolean test(FluidStack fluidStack) {
            return testType(fluidStack) && fluidStack.getAmount() >= fluidInstance.getAmount();
        }

        @Override
        public boolean testType(FluidStack fluidStack) {
            return Objects.requireNonNull(fluidStack).isFluidEqual(fluidInstance);
        }

        @Override
        public FluidStack getMatchingInstance(FluidStack fluidStack) {
            return test(fluidStack) ? fluidInstance.copy() : FluidStack.EMPTY;
        }

        @Override
        public long getNeededAmount(FluidStack stack) {
            return testType(stack) ? fluidInstance.getAmount() : 0;
        }

        @Override
        public boolean hasNoMatchingInstances() {
            return false;
        }

        @Override
        public List<@NotNull FluidStack> getRepresentations() {
            return representations;
        }

        /**
         * For use in recipe input caching. Do not use this to modify the backing stack.
         */
        public FluidStack getInputRaw() {
            return fluidInstance;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeEnum(IngredientType.SINGLE);
            fluidInstance.writeToPacket(buffer);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (o == null || getClass() != o.getClass()) {
                return false;
            }
            SingleFluidStackIngredient other = (SingleFluidStackIngredient) o;
            //Need to use this over equals to ensure we compare amounts
            return fluidInstance.isFluidStackIdentical(other.fluidInstance);
        }

        @Override
        public int hashCode() {
            return fluidInstance.hashCode();
        }
    }

    @NothingNullByDefault
    public static class TaggedFluidStackIngredient extends FluidStackIngredient {

        //Note: This must be a lazily initialized so that this class can be loaded in tests
        static final Codec<TaggedFluidStackIngredient> CODEC = ExtraCodecs.lazyInitializedCodec(() -> RecordCodecBuilder.create(instance->instance.group(
              TagKey.codec(Registries.FLUID).fieldOf(JsonConstants.TAG).forGetter(TaggedFluidStackIngredient::getTag),
              ExtraCodecs.POSITIVE_INT.fieldOf(JsonConstants.AMOUNT).forGetter(TaggedFluidStackIngredient::getRawAmount)
        ).apply(instance, TaggedFluidStackIngredient::new)));

        private final HolderSet.Named<Fluid> tag;
        private final int amount;

        private TaggedFluidStackIngredient(TagKey<Fluid> tag, int amount) {
            this(BuiltInRegistries.FLUID.getOrCreateTag(tag), amount);
        }

        private TaggedFluidStackIngredient(HolderSet.Named<Fluid> tag, int amount) {
            this.tag = tag;
            this.amount = amount;
        }

        @Override
        public boolean test(FluidStack fluidStack) {
            return testType(fluidStack) && fluidStack.getAmount() >= amount;
        }

        @Override
        public boolean testType(FluidStack fluidStack) {
            return Objects.requireNonNull(fluidStack).getFluid().builtInRegistryHolder().is(getTag());
        }

        @Override
        public FluidStack getMatchingInstance(FluidStack fluidStack) {
            if (test(fluidStack)) {
                //Our fluid is in the tag, so we make a new stack with the given amount
                return new FluidStack(fluidStack, amount);
            }
            return FluidStack.EMPTY;
        }

        @Override
        public long getNeededAmount(FluidStack stack) {
            return testType(stack) ? amount : 0;
        }

        @Override
        public boolean hasNoMatchingInstances() {
            return tag.size() == 0;
        }

        @Override
        public List<@NotNull FluidStack> getRepresentations() {
            //TODO: Can this be cached somehow
            List<@NotNull FluidStack> representations = new ArrayList<>();
            for (Holder<Fluid> fluid : tag) {
                representations.add(new FluidStack(fluid.value(), amount));
            }
            return representations;
        }

        /**
         * For use in recipe input caching.
         */
        public Iterable<Holder<Fluid>> getRawInput() {
            return tag;
        }

        public int getRawAmount() {
            return amount;
        }

        public TagKey<Fluid> getTag() {
            return tag.key();
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeEnum(IngredientType.TAGGED);
            buffer.writeResourceLocation(getTag().location());
            buffer.writeVarInt(amount);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TaggedFluidStackIngredient other = (TaggedFluidStackIngredient) o;
            return amount == other.amount && tag.equals(other.tag);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tag, amount);
        }
    }

    @NothingNullByDefault
    public static class MultiFluidStackIngredient extends FluidStackIngredient implements IMultiIngredient<FluidStack, FluidStackIngredient> {

        static final Codec<MultiFluidStackIngredient> CODEC = ExtraCodecs.nonEmptyList(SINGLE_CODEC.listOf()).xmap(
              lst->new MultiFluidStackIngredient(lst.toArray(new FluidStackIngredient[0])),
              MultiFluidStackIngredient::getIngredients
        );

        private final FluidStackIngredient[] ingredients;

        private MultiFluidStackIngredient(FluidStackIngredient... ingredients) {
            this.ingredients = ingredients;
        }

        @Override
        public boolean test(FluidStack stack) {
            return Arrays.stream(ingredients).anyMatch(ingredient -> ingredient.test(stack));
        }

        @Override
        public boolean testType(FluidStack stack) {
            return Arrays.stream(ingredients).anyMatch(ingredient -> ingredient.testType(stack));
        }

        @Override
        public FluidStack getMatchingInstance(FluidStack stack) {
            for (FluidStackIngredient ingredient : ingredients) {
                FluidStack matchingInstance = ingredient.getMatchingInstance(stack);
                if (!matchingInstance.isEmpty()) {
                    return matchingInstance;
                }
            }
            return FluidStack.EMPTY;
        }

        @Override
        public long getNeededAmount(FluidStack stack) {
            for (FluidStackIngredient ingredient : ingredients) {
                long amount = ingredient.getNeededAmount(stack);
                if (amount > 0) {
                    return amount;
                }
            }
            return 0;
        }

        @Override
        public boolean hasNoMatchingInstances() {
            return Arrays.stream(ingredients).allMatch(InputIngredient::hasNoMatchingInstances);
        }

        @Override
        public List<@NotNull FluidStack> getRepresentations() {
            List<@NotNull FluidStack> representations = new ArrayList<>();
            for (FluidStackIngredient ingredient : ingredients) {
                representations.addAll(ingredient.getRepresentations());
            }
            return representations;
        }

        @Override
        public boolean forEachIngredient(Predicate<FluidStackIngredient> checker) {
            boolean result = false;
            for (FluidStackIngredient ingredient : ingredients) {
                result |= checker.test(ingredient);
            }
            return result;
        }

        @Override
        public final List<FluidStackIngredient> getIngredients() {
            return List.of(ingredients);
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeEnum(IngredientType.MULTI);
            BasePacketHandler.writeArray(buffer, ingredients, InputIngredient::write);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (o == null || getClass() != o.getClass()) {
                return false;
            }
            return Arrays.equals(ingredients, ((MultiFluidStackIngredient) o).ingredients);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(ingredients);
        }
    }

    private enum IngredientType {
        SINGLE,
        TAGGED,
        MULTI
    }
}