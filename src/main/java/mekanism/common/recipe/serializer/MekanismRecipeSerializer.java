package mekanism.common.recipe.serializer;

import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Function7;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.BiFunction;
import java.util.function.Function;
import mekanism.api.SerializationConstants;
import mekanism.api.SerializerHelper;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.ChemicalDissolutionRecipe;
import mekanism.api.recipes.CombinerRecipe;
import mekanism.api.recipes.ElectrolysisRecipe;
import mekanism.api.recipes.FluidSlurryToSlurryRecipe;
import mekanism.api.recipes.FluidToFluidRecipe;
import mekanism.api.recipes.GasToGasRecipe;
import mekanism.api.recipes.ItemStackToEnergyRecipe;
import mekanism.api.recipes.NucleosynthesizingRecipe;
import mekanism.api.recipes.PressurizedReactionRecipe;
import mekanism.api.recipes.basic.BasicChemicalDissolutionRecipe;
import mekanism.api.recipes.basic.BasicCombinerRecipe;
import mekanism.api.recipes.basic.BasicElectrolysisRecipe;
import mekanism.api.recipes.basic.BasicFluidSlurryToSlurryRecipe;
import mekanism.api.recipes.basic.BasicFluidToFluidRecipe;
import mekanism.api.recipes.basic.BasicGasToGasRecipe;
import mekanism.api.recipes.basic.BasicItemStackToEnergyRecipe;
import mekanism.api.recipes.basic.BasicItemStackToItemStackRecipe;
import mekanism.api.recipes.basic.BasicNucleosynthesizingRecipe;
import mekanism.api.recipes.basic.BasicPressurizedReactionRecipe;
import mekanism.api.recipes.basic.IBasicChemicalOutput;
import mekanism.api.recipes.basic.IBasicItemStackOutput;
import mekanism.api.recipes.chemical.ChemicalChemicalToChemicalRecipe;
import mekanism.api.recipes.chemical.ItemStackChemicalToItemStackRecipe;
import mekanism.api.recipes.chemical.ItemStackToChemicalRecipe;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient;
import mekanism.api.recipes.ingredients.FluidStackIngredient;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import mekanism.api.recipes.ingredients.creator.IIngredientCreator;
import mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess;
import mekanism.common.recipe.WrappedShapedRecipe;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

public record MekanismRecipeSerializer<RECIPE extends Recipe<?>>(MapCodec<RECIPE> codec, StreamCodec<RegistryFriendlyByteBuf, RECIPE> streamCodec)
      implements RecipeSerializer<RECIPE> {

    public static <RECIPE extends WrappedShapedRecipe> MekanismRecipeSerializer<RECIPE> wrapped(Function<ShapedRecipe, RECIPE> wrapper) {
        return new MekanismRecipeSerializer<>(
              RecipeSerializer.SHAPED_RECIPE.codec().xmap(wrapper, WrappedShapedRecipe::getInternal),
              RecipeSerializer.SHAPED_RECIPE.streamCodec().map(wrapper, WrappedShapedRecipe::getInternal)
        );
    }

    public static <RECIPE extends BasicItemStackToItemStackRecipe> MekanismRecipeSerializer<RECIPE> itemToItem(BiFunction<ItemStackIngredient, ItemStack, RECIPE> factory) {
        return new MekanismRecipeSerializer<>(RecordCodecBuilder.mapCodec(instance -> instance.group(
              ItemStackIngredient.CODEC.fieldOf(SerializationConstants.INPUT).forGetter(BasicItemStackToItemStackRecipe::getInput),
              ItemStack.CODEC.fieldOf(SerializationConstants.OUTPUT).forGetter(BasicItemStackToItemStackRecipe::getOutputRaw)
        ).apply(instance, factory)), StreamCodec.composite(
              ItemStackIngredient.STREAM_CODEC, BasicItemStackToItemStackRecipe::getInput,
              ItemStack.STREAM_CODEC, BasicItemStackToItemStackRecipe::getOutputRaw,
              factory
        ));
    }

    public static MekanismRecipeSerializer<BasicCombinerRecipe> combining(Function3<ItemStackIngredient, ItemStackIngredient, ItemStack, BasicCombinerRecipe> factory) {
        return new MekanismRecipeSerializer<>(RecordCodecBuilder.mapCodec(instance -> instance.group(
              ItemStackIngredient.CODEC.fieldOf(SerializationConstants.MAIN_INPUT).forGetter(CombinerRecipe::getMainInput),
              ItemStackIngredient.CODEC.fieldOf(SerializationConstants.EXTRA_INPUT).forGetter(CombinerRecipe::getExtraInput),
              ItemStack.CODEC.fieldOf(SerializationConstants.OUTPUT).forGetter(BasicCombinerRecipe::getOutputRaw)
        ).apply(instance, factory)), StreamCodec.composite(
              ItemStackIngredient.STREAM_CODEC, BasicCombinerRecipe::getMainInput,
              ItemStackIngredient.STREAM_CODEC, BasicCombinerRecipe::getExtraInput,
              ItemStack.STREAM_CODEC, BasicCombinerRecipe::getOutputRaw,
              factory
        ));
    }

    public static <RECIPE extends BasicItemStackToEnergyRecipe> MekanismRecipeSerializer<RECIPE> itemToEnergy(BiFunction<ItemStackIngredient, Long, RECIPE> factory) {
        return new MekanismRecipeSerializer<>(RecordCodecBuilder.mapCodec(instance -> instance.group(
              ItemStackIngredient.CODEC.fieldOf(SerializationConstants.INPUT).forGetter(ItemStackToEnergyRecipe::getInput),
              SerializerHelper.POSITIVE_NONZERO_LONG_CODEC_LEGACY.fieldOf(SerializationConstants.OUTPUT).forGetter(BasicItemStackToEnergyRecipe::getOutputRaw)
        ).apply(instance, factory)), StreamCodec.composite(
              ItemStackIngredient.STREAM_CODEC, ItemStackToEnergyRecipe::getInput,
              ByteBufCodecs.VAR_LONG, BasicItemStackToEnergyRecipe::getOutputRaw,
              factory
        ));
    }

    public static <RECIPE extends BasicFluidToFluidRecipe> MekanismRecipeSerializer<RECIPE> fluidToFluid(BiFunction<FluidStackIngredient, FluidStack, RECIPE> factory) {
        return new MekanismRecipeSerializer<>(RecordCodecBuilder.mapCodec(instance -> instance.group(
              FluidStackIngredient.CODEC.fieldOf(SerializationConstants.INPUT).forGetter(FluidToFluidRecipe::getInput),
              FluidStack.CODEC.fieldOf(SerializationConstants.OUTPUT).forGetter(BasicFluidToFluidRecipe::getOutputRaw)
        ).apply(instance, factory)), StreamCodec.composite(
              FluidStackIngredient.STREAM_CODEC, FluidToFluidRecipe::getInput,
              FluidStack.STREAM_CODEC, BasicFluidToFluidRecipe::getOutputRaw,
              factory
        ));
    }

    public static <RECIPE extends BasicGasToGasRecipe> MekanismRecipeSerializer<RECIPE> gasToGas(BiFunction<ChemicalStackIngredient, ChemicalStack, RECIPE> factory) {
        return new MekanismRecipeSerializer<>(RecordCodecBuilder.mapCodec(instance -> instance.group(
              IngredientCreatorAccess.chemicalStack().codec().fieldOf(SerializationConstants.INPUT).forGetter(GasToGasRecipe::getInput),
              ChemicalStack.MAP_CODEC.fieldOf(SerializationConstants.OUTPUT).forGetter(BasicGasToGasRecipe::getOutputRaw)
        ).apply(instance, factory)), StreamCodec.composite(
              IngredientCreatorAccess.chemicalStack().streamCodec(), GasToGasRecipe::getInput,
              ChemicalStack.STREAM_CODEC, BasicGasToGasRecipe::getOutputRaw,
              factory
        ));
    }

    public static MekanismRecipeSerializer<BasicFluidSlurryToSlurryRecipe> fluidSlurryToSlurry(Function3<FluidStackIngredient, ChemicalStackIngredient, ChemicalStack, BasicFluidSlurryToSlurryRecipe> factory) {
        return new MekanismRecipeSerializer<>(RecordCodecBuilder.mapCodec(instance -> instance.group(
              FluidStackIngredient.CODEC.fieldOf(SerializationConstants.FLUID_INPUT).forGetter(FluidSlurryToSlurryRecipe::getFluidInput),
              IngredientCreatorAccess.chemicalStack().codec().fieldOf(SerializationConstants.SLURRY_INPUT).forGetter(FluidSlurryToSlurryRecipe::getChemicalInput),
              ChemicalStack.CODEC.fieldOf(SerializationConstants.OUTPUT).forGetter(BasicFluidSlurryToSlurryRecipe::getOutputRaw)
        ).apply(instance, factory)), StreamCodec.composite(
              FluidStackIngredient.STREAM_CODEC, FluidSlurryToSlurryRecipe::getFluidInput,
              IngredientCreatorAccess.chemicalStack().streamCodec(), FluidSlurryToSlurryRecipe::getChemicalInput,
              ChemicalStack.STREAM_CODEC, BasicFluidSlurryToSlurryRecipe::getOutputRaw,
              factory
        ));
    }

    public static MekanismRecipeSerializer<BasicNucleosynthesizingRecipe> nucleosynthesizing(Function4<ItemStackIngredient, ChemicalStackIngredient, ItemStack, Integer, BasicNucleosynthesizingRecipe> factory) {
        return new MekanismRecipeSerializer<>(RecordCodecBuilder.mapCodec(instance -> instance.group(
              ItemStackIngredient.CODEC.fieldOf(SerializationConstants.ITEM_INPUT).forGetter(NucleosynthesizingRecipe::getItemInput),
              IngredientCreatorAccess.chemicalStack().codec().fieldOf(SerializationConstants.GAS_INPUT).forGetter(NucleosynthesizingRecipe::getChemicalInput),
              ItemStack.CODEC.fieldOf(SerializationConstants.OUTPUT).forGetter(BasicNucleosynthesizingRecipe::getOutputRaw),
              ExtraCodecs.POSITIVE_INT.fieldOf(SerializationConstants.DURATION).forGetter(NucleosynthesizingRecipe::getDuration)
        ).apply(instance, factory)), StreamCodec.composite(
              ItemStackIngredient.STREAM_CODEC, NucleosynthesizingRecipe::getItemInput,
              IngredientCreatorAccess.chemicalStack().streamCodec(), NucleosynthesizingRecipe::getChemicalInput,
              ItemStack.STREAM_CODEC, BasicNucleosynthesizingRecipe::getOutputRaw,
              ByteBufCodecs.VAR_INT, NucleosynthesizingRecipe::getDuration,
              factory
        ));
    }

    public static MekanismRecipeSerializer<BasicElectrolysisRecipe> separating(Function4<FluidStackIngredient, Long, ChemicalStack, ChemicalStack, BasicElectrolysisRecipe> factory) {
        return new MekanismRecipeSerializer<>(RecordCodecBuilder.mapCodec(instance -> instance.group(
              FluidStackIngredient.CODEC.fieldOf(SerializationConstants.INPUT).forGetter(ElectrolysisRecipe::getInput),
              SerializerHelper.POSITIVE_NONZERO_LONG_CODEC_LEGACY.optionalFieldOf(SerializationConstants.ENERGY_MULTIPLIER, 1L).forGetter(ElectrolysisRecipe::getEnergyMultiplier),
              ChemicalStack.MAP_CODEC.fieldOf(SerializationConstants.LEFT_GAS_OUTPUT).forGetter(BasicElectrolysisRecipe::getLeftGasOutput),
              ChemicalStack.MAP_CODEC.fieldOf(SerializationConstants.RIGHT_GAS_OUTPUT).forGetter(BasicElectrolysisRecipe::getRightGasOutput)
        ).apply(instance, factory)), StreamCodec.composite(
              FluidStackIngredient.STREAM_CODEC, ElectrolysisRecipe::getInput,
              ByteBufCodecs.VAR_LONG, ElectrolysisRecipe::getEnergyMultiplier,
              ChemicalStack.STREAM_CODEC, BasicElectrolysisRecipe::getLeftGasOutput,
              ChemicalStack.STREAM_CODEC, BasicElectrolysisRecipe::getRightGasOutput,
              factory
        ));
    }

    public static MekanismRecipeSerializer<BasicChemicalDissolutionRecipe> dissolution(Function3<ItemStackIngredient, ChemicalStackIngredient, ChemicalStack, BasicChemicalDissolutionRecipe> factory) {
        return new MekanismRecipeSerializer<>(RecordCodecBuilder.mapCodec(instance -> instance.group(
              ItemStackIngredient.CODEC.fieldOf(SerializationConstants.ITEM_INPUT).forGetter(ChemicalDissolutionRecipe::getItemInput),
              IngredientCreatorAccess.chemicalStack().codec().fieldOf(SerializationConstants.GAS_INPUT).forGetter(ChemicalDissolutionRecipe::getGasInput),
              ChemicalStack.CODEC.fieldOf(SerializationConstants.OUTPUT).forGetter(BasicChemicalDissolutionRecipe::getOutputRaw)
        ).apply(instance, factory)), StreamCodec.composite(
              ItemStackIngredient.STREAM_CODEC, BasicChemicalDissolutionRecipe::getItemInput,
              IngredientCreatorAccess.chemicalStack().streamCodec(), BasicChemicalDissolutionRecipe::getGasInput,
              ChemicalStack.STREAM_CODEC, BasicChemicalDissolutionRecipe::getOutputRaw,
              factory::apply
        ));
    }

    public static MekanismRecipeSerializer<BasicPressurizedReactionRecipe> reaction(
          Function7<ItemStackIngredient, FluidStackIngredient, ChemicalStackIngredient, Long, Integer, ItemStack, ChemicalStack, BasicPressurizedReactionRecipe> factory) {
        return new MekanismRecipeSerializer<>(RecordCodecBuilder.<BasicPressurizedReactionRecipe>mapCodec(instance -> instance.group(
              ItemStackIngredient.CODEC.fieldOf(SerializationConstants.ITEM_INPUT).forGetter(PressurizedReactionRecipe::getInputSolid),
              FluidStackIngredient.CODEC.fieldOf(SerializationConstants.FLUID_INPUT).forGetter(PressurizedReactionRecipe::getInputFluid),
              IngredientCreatorAccess.chemicalStack().codec().fieldOf(SerializationConstants.GAS_INPUT).forGetter(PressurizedReactionRecipe::getInputGas),
              SerializerHelper.POSITIVE_LONG_CODEC_LEGACY.optionalFieldOf(SerializationConstants.ENERGY_REQUIRED, 0L).forGetter(PressurizedReactionRecipe::getEnergyRequired),
              ExtraCodecs.POSITIVE_INT.fieldOf(SerializationConstants.DURATION).forGetter(PressurizedReactionRecipe::getDuration),
              ItemStack.CODEC.optionalFieldOf(SerializationConstants.ITEM_OUTPUT, ItemStack.EMPTY).forGetter(BasicPressurizedReactionRecipe::getOutputItem),
              ChemicalStack.CODEC.optionalFieldOf(SerializationConstants.GAS_OUTPUT, ChemicalStack.EMPTY).forGetter(BasicPressurizedReactionRecipe::getOutputGas)
        ).apply(instance, factory)).validate(result -> {
            if (result.getOutputItem().isEmpty() && result.getOutputGas().isEmpty()) {
                return DataResult.error(() -> "No output specified, must have at least an Item or Gas output");
            }
            return DataResult.success(result);
        }), NeoForgeStreamCodecs.composite(
              ItemStackIngredient.STREAM_CODEC, PressurizedReactionRecipe::getInputSolid,
              FluidStackIngredient.STREAM_CODEC, PressurizedReactionRecipe::getInputFluid,
              IngredientCreatorAccess.chemicalStack().streamCodec(), PressurizedReactionRecipe::getInputGas,
              ByteBufCodecs.VAR_LONG, PressurizedReactionRecipe::getEnergyRequired,
              ByteBufCodecs.VAR_INT, PressurizedReactionRecipe::getDuration,
              ItemStack.OPTIONAL_STREAM_CODEC, BasicPressurizedReactionRecipe::getOutputItem,
              ChemicalStack.OPTIONAL_STREAM_CODEC, BasicPressurizedReactionRecipe::getOutputGas,
              factory
        ));
    }

    public static <RECIPE extends ItemStackToChemicalRecipe & IBasicChemicalOutput>
    MekanismRecipeSerializer<RECIPE> itemToChemical(BiFunction<ItemStackIngredient, ChemicalStack, RECIPE> factory, MapCodec<ChemicalStack> stackCodec, StreamCodec<? super RegistryFriendlyByteBuf, ChemicalStack> stackStreamCodec) {
        return new MekanismRecipeSerializer<>(RecordCodecBuilder.mapCodec(instance -> instance.group(
              ItemStackIngredient.CODEC.fieldOf(SerializationConstants.INPUT).forGetter(ItemStackToChemicalRecipe::getInput),
              stackCodec.fieldOf(SerializationConstants.OUTPUT).forGetter(IBasicChemicalOutput::getOutputRaw)
        ).apply(instance, factory)), StreamCodec.composite(
              ItemStackIngredient.STREAM_CODEC, ItemStackToChemicalRecipe::getInput,
              stackStreamCodec, IBasicChemicalOutput::getOutputRaw,
              factory
        ));
    }

    public static <RECIPE extends ItemStackChemicalToItemStackRecipe & IBasicItemStackOutput> MekanismRecipeSerializer<RECIPE> itemChemicalToItem(
          Function3<ItemStackIngredient, ChemicalStackIngredient, ItemStack, RECIPE> factory, IIngredientCreator<Chemical, ChemicalStack, ChemicalStackIngredient> ingredientCreator) {
        return new MekanismRecipeSerializer<>(RecordCodecBuilder.mapCodec(instance -> instance.group(
              ItemStackIngredient.CODEC.fieldOf(SerializationConstants.ITEM_INPUT).forGetter(ItemStackChemicalToItemStackRecipe::getItemInput),
              ingredientCreator.codec().fieldOf(SerializationConstants.CHEMICAL_INPUT).forGetter(ItemStackChemicalToItemStackRecipe::getChemicalInput),
              ItemStack.CODEC.fieldOf(SerializationConstants.OUTPUT).forGetter(IBasicItemStackOutput::getOutputRaw)
        ).apply(instance, factory)), StreamCodec.composite(
              ItemStackIngredient.STREAM_CODEC, ItemStackChemicalToItemStackRecipe::getItemInput,
              ingredientCreator.streamCodec(), ItemStackChemicalToItemStackRecipe::getChemicalInput,
              ItemStack.STREAM_CODEC, IBasicItemStackOutput::getOutputRaw,
              factory
        ));
    }

    public static <RECIPE extends ChemicalChemicalToChemicalRecipe & IBasicChemicalOutput> MekanismRecipeSerializer<RECIPE>
    chemicalChemicalToChemical(Function3<ChemicalStackIngredient, ChemicalStackIngredient, ChemicalStack, RECIPE> factory, IIngredientCreator<Chemical, ChemicalStack, ChemicalStackIngredient> ingredientCreator,
          MapCodec<ChemicalStack> stackCodec, StreamCodec<? super RegistryFriendlyByteBuf, ChemicalStack> stackStreamCodec) {
        return new MekanismRecipeSerializer<>(RecordCodecBuilder.mapCodec(instance -> instance.group(
              ingredientCreator.codec().fieldOf(SerializationConstants.LEFT_INPUT).forGetter(ChemicalChemicalToChemicalRecipe::getLeftInput),
              ingredientCreator.codec().fieldOf(SerializationConstants.RIGHT_INPUT).forGetter(ChemicalChemicalToChemicalRecipe::getRightInput),
              stackCodec.fieldOf(SerializationConstants.OUTPUT).forGetter(IBasicChemicalOutput::getOutputRaw)
        ).apply(instance, factory)), StreamCodec.composite(
              ingredientCreator.streamCodec(), ChemicalChemicalToChemicalRecipe::getLeftInput,
              ingredientCreator.streamCodec(), ChemicalChemicalToChemicalRecipe::getRightInput,
              stackStreamCodec, IBasicChemicalOutput::getOutputRaw,
              factory
        ));
    }
}