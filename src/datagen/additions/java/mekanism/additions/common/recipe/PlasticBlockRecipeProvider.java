package mekanism.additions.common.recipe;

import java.util.Map;
import mekanism.additions.common.AdditionsTags;
import mekanism.additions.common.MekanismAdditions;
import mekanism.additions.common.block.plastic.BlockPlastic;
import mekanism.additions.common.block.plastic.BlockPlasticRoad;
import mekanism.additions.common.block.plastic.BlockPlasticTransparent;
import mekanism.additions.common.registries.AdditionsBlocks;
import mekanism.api.datagen.recipe.builder.ItemStackChemicalToItemStackRecipeBuilder;
import mekanism.api.datagen.recipe.builder.ItemStackToItemStackRecipeBuilder;
import mekanism.api.providers.IItemProvider;
import mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess;
import mekanism.api.text.EnumColor;
import mekanism.common.block.interfaces.IColoredBlock;
import mekanism.common.recipe.BaseRecipeProvider;
import mekanism.common.recipe.ISubRecipeProvider;
import mekanism.common.recipe.builder.ExtendedShapedRecipeBuilder;
import mekanism.common.recipe.builder.ExtendedShapelessRecipeBuilder;
import mekanism.common.recipe.impl.PigmentExtractingRecipeProvider;
import mekanism.common.recipe.pattern.Pattern;
import mekanism.common.recipe.pattern.RecipePattern;
import mekanism.common.recipe.pattern.RecipePattern.TripleLine;
import mekanism.common.registration.impl.BlockRegistryObject;
import mekanism.common.registries.MekanismChemicals;
import mekanism.common.registries.MekanismItems;
import mekanism.common.resource.PrimaryResource;
import mekanism.common.resource.ResourceType;
import mekanism.common.tags.MekanismTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.Tags;

public class PlasticBlockRecipeProvider implements ISubRecipeProvider {

    private static final RecipePattern PLASTIC = RecipePattern.createPattern(
          TripleLine.of(Pattern.EMPTY, Pattern.CONSTANT, Pattern.EMPTY),
          TripleLine.of(Pattern.CONSTANT, Pattern.DYE, Pattern.CONSTANT),
          TripleLine.of(Pattern.EMPTY, Pattern.CONSTANT, Pattern.EMPTY));
    private static final RecipePattern PLASTIC_TRANSPARENT = RecipePattern.createPattern(
          TripleLine.of(Pattern.CONSTANT, Pattern.CONSTANT, Pattern.CONSTANT),
          TripleLine.of(Pattern.CONSTANT, Pattern.DYE, Pattern.CONSTANT),
          TripleLine.of(Pattern.CONSTANT, Pattern.CONSTANT, Pattern.CONSTANT));
    private static final RecipePattern REINFORCED_PLASTIC = RecipePattern.createPattern(
          TripleLine.of(Pattern.EMPTY, Pattern.CONSTANT, Pattern.EMPTY),
          TripleLine.of(Pattern.CONSTANT, Pattern.OSMIUM, Pattern.CONSTANT),
          TripleLine.of(Pattern.EMPTY, Pattern.CONSTANT, Pattern.EMPTY));
    private static final RecipePattern PLASTIC_ROAD = RecipePattern.createPattern(
          TripleLine.of(AdditionsRecipeProvider.SAND_CHAR, AdditionsRecipeProvider.SAND_CHAR, AdditionsRecipeProvider.SAND_CHAR),
          TripleLine.of(Pattern.CONSTANT, Pattern.CONSTANT, Pattern.CONSTANT),
          TripleLine.of(AdditionsRecipeProvider.SAND_CHAR, AdditionsRecipeProvider.SAND_CHAR, AdditionsRecipeProvider.SAND_CHAR));
    private static final RecipePattern SLICK_PLASTIC = RecipePattern.createPattern(
          TripleLine.of(Pattern.EMPTY, Pattern.CONSTANT, Pattern.EMPTY),
          TripleLine.of(Pattern.CONSTANT, AdditionsRecipeProvider.SLIME_CHAR, Pattern.CONSTANT),
          TripleLine.of(Pattern.EMPTY, Pattern.CONSTANT, Pattern.EMPTY));

    @Override
    public void addRecipes(RecipeOutput consumer, HolderLookup.Provider registries) {
        String basePath = "plastic/";
        registerPlasticBlocks(consumer, basePath);
        registerPlasticGlow(consumer, basePath);
        registerReinforcedPlastic(consumer, basePath);
        registerPlasticRoads(consumer, basePath);
        registerSlickPlastic(consumer, basePath);
        registerPlasticTransparent(consumer, basePath);
    }

    private void registerPlasticBlocks(RecipeOutput consumer, String basePath) {
        basePath += "block/";
        for (BlockRegistryObject<BlockPlastic, ?> blockRO : AdditionsBlocks.PLASTIC_BLOCKS.values()) {
            registerPlasticBlock(consumer, blockRO, basePath);
        }
    }

    private void registerPlasticBlock(RecipeOutput consumer, BlockRegistryObject<? extends IColoredBlock, ?> result, String basePath) {
        EnumColor color = result.getBlock().getColor();
        DyeColor dye = color.getDyeColor();
        if (dye != null) {
            ExtendedShapedRecipeBuilder.shapedRecipe(result, 4)
                  .pattern(PLASTIC)
                  .key(Pattern.CONSTANT, MekanismItems.HDPE_SHEET)
                  .key(Pattern.DYE, dye.getTag())
                  .category(RecipeCategory.BUILDING_BLOCKS)
                  .build(consumer, MekanismAdditions.rl(basePath + color.getRegistryPrefix()));
        }
        registerRecolor(consumer, result, AdditionsTags.Items.PLASTIC_BLOCKS_PLASTIC, color, basePath);
    }

    private void registerPlasticTransparent(RecipeOutput consumer, String basePath) {
        basePath += "transparent/";
        for (BlockRegistryObject<BlockPlasticTransparent, ?> blockRO : AdditionsBlocks.TRANSPARENT_PLASTIC_BLOCKS.values()) {
            registerPlasticTransparent(consumer, blockRO, basePath);
        }
    }

    private void registerPlasticTransparent(RecipeOutput consumer, BlockRegistryObject<? extends IColoredBlock, ?> result, String basePath) {
        EnumColor color = result.getBlock().getColor();
        DyeColor dye = color.getDyeColor();
        if (dye != null) {
            ExtendedShapedRecipeBuilder.shapedRecipe(result, 8)
                  .pattern(PLASTIC_TRANSPARENT)
                  .key(Pattern.CONSTANT, MekanismItems.HDPE_SHEET)
                  .key(Pattern.DYE, dye.getTag())
                  .category(RecipeCategory.BUILDING_BLOCKS)
                  .build(consumer, MekanismAdditions.rl(basePath + color.getRegistryPrefix()));
        }
        registerTransparentRecolor(consumer, result, AdditionsTags.Items.PLASTIC_BLOCKS_TRANSPARENT, color, basePath);
    }

    private void registerPlasticGlow(RecipeOutput consumer, String basePath) {
        basePath += "glow/";
        for (Map.Entry<EnumColor, ? extends BlockRegistryObject<BlockPlastic, ?>> entry : AdditionsBlocks.PLASTIC_GLOW_BLOCKS.entrySet()) {
            registerPlasticGlow(consumer, entry.getValue(), AdditionsBlocks.PLASTIC_BLOCKS.get(entry.getKey()), basePath);
        }
    }

    private void registerPlasticGlow(RecipeOutput consumer, BlockRegistryObject<? extends IColoredBlock, ?> result, IItemProvider plastic, String basePath) {
        EnumColor color = result.getBlock().getColor();
        ExtendedShapelessRecipeBuilder.shapelessRecipe(result, 3)
              .addIngredient(plastic, 3)
              .addIngredient(Tags.Items.DUSTS_GLOWSTONE)
              .category(RecipeCategory.BUILDING_BLOCKS)
              .build(consumer, MekanismAdditions.rl(basePath + color.getRegistryPrefix()));
        registerRecolor(consumer, result, AdditionsTags.Items.PLASTIC_BLOCKS_GLOW, color, basePath);
    }

    private void registerReinforcedPlastic(RecipeOutput consumer, String basePath) {
        basePath += "reinforced/";
        for (Map.Entry<EnumColor, ? extends BlockRegistryObject<BlockPlastic, ?>> entry : AdditionsBlocks.REINFORCED_PLASTIC_BLOCKS.entrySet()) {
            registerReinforcedPlastic(consumer, entry.getValue(), AdditionsBlocks.PLASTIC_BLOCKS.get(entry.getKey()), basePath);
        }
    }

    private void registerReinforcedPlastic(RecipeOutput consumer, BlockRegistryObject<? extends IColoredBlock, ?> result, IItemProvider plastic, String basePath) {
        EnumColor color = result.getBlock().getColor();
        ExtendedShapedRecipeBuilder.shapedRecipe(result, 4)
              .pattern(REINFORCED_PLASTIC)
              .key(Pattern.OSMIUM, MekanismTags.Items.PROCESSED_RESOURCES.get(ResourceType.DUST, PrimaryResource.OSMIUM))
              .key(Pattern.CONSTANT, plastic)
              .category(RecipeCategory.BUILDING_BLOCKS)
              .build(consumer, MekanismAdditions.rl(basePath + color.getRegistryPrefix()));
        registerRecolor(consumer, result, AdditionsTags.Items.PLASTIC_BLOCKS_REINFORCED, color, basePath);
    }

    private void registerPlasticRoads(RecipeOutput consumer, String basePath) {
        basePath += "road/";
        for (Map.Entry<EnumColor, ? extends BlockRegistryObject<BlockPlasticRoad, ?>> entry : AdditionsBlocks.PLASTIC_ROADS.entrySet()) {
            registerPlasticRoad(consumer, entry.getValue(), AdditionsBlocks.SLICK_PLASTIC_BLOCKS.get(entry.getKey()), basePath);
        }
    }

    private void registerPlasticRoad(RecipeOutput consumer, BlockRegistryObject<? extends IColoredBlock, ?> result, IItemProvider slickPlastic, String basePath) {
        EnumColor color = result.getBlock().getColor();
        ExtendedShapedRecipeBuilder.shapedRecipe(result, 3)
              .pattern(PLASTIC_ROAD)
              .key(AdditionsRecipeProvider.SAND_CHAR, Tags.Items.SANDS)
              .key(Pattern.CONSTANT, slickPlastic)
              .category(RecipeCategory.BUILDING_BLOCKS)
              .build(consumer, MekanismAdditions.rl(basePath + color.getRegistryPrefix()));
        registerRecolor(consumer, result, AdditionsTags.Items.PLASTIC_BLOCKS_ROAD, color, basePath);
    }

    private void registerSlickPlastic(RecipeOutput consumer, String basePath) {
        basePath += "slick/";
        for (Map.Entry<EnumColor, ? extends BlockRegistryObject<BlockPlastic, ?>> entry : AdditionsBlocks.SLICK_PLASTIC_BLOCKS.entrySet()) {
            registerSlickPlastic(consumer, entry.getValue(), AdditionsBlocks.PLASTIC_BLOCKS.get(entry.getKey()), basePath);
        }
    }

    private void registerSlickPlastic(RecipeOutput consumer, BlockRegistryObject<? extends IColoredBlock, ?> result, IItemProvider plastic, String basePath) {
        EnumColor color = result.getBlock().getColor();
        String colorString = color.getRegistryPrefix();
        ExtendedShapedRecipeBuilder.shapedRecipe(result, 4)
              .pattern(SLICK_PLASTIC)
              .key(Pattern.CONSTANT, plastic)
              .key(AdditionsRecipeProvider.SLIME_CHAR, Tags.Items.SLIMEBALLS)
              .category(RecipeCategory.BUILDING_BLOCKS)
              .build(consumer, MekanismAdditions.rl(basePath + colorString));
        //Enriching recipes
        ItemStackToItemStackRecipeBuilder.enriching(
              IngredientCreatorAccess.item().from(plastic),
              result.getItemStack()
        ).build(consumer, MekanismAdditions.rl(basePath + "enriching/" + colorString));
        //Recolor recipes
        registerRecolor(consumer, result, AdditionsTags.Items.PLASTIC_BLOCKS_SLICK, color, basePath);
    }

    public static void registerRecolor(RecipeOutput consumer, IItemProvider result, TagKey<Item> blockType, EnumColor color, String basePath) {
        Ingredient recolorInput = BaseRecipeProvider.difference(blockType, result);
        String colorString = color.getRegistryPrefix();
        DyeColor dye = color.getDyeColor();
        if (dye != null) {
            ExtendedShapedRecipeBuilder.shapedRecipe(result, 4)
                  .pattern(PLASTIC)
                  .key(Pattern.CONSTANT, recolorInput)
                  .key(Pattern.DYE, dye.getTag())
                  .category(RecipeCategory.BUILDING_BLOCKS)
                  .build(consumer, MekanismAdditions.rl(basePath + "recolor/" + colorString));
        }
        ItemStackChemicalToItemStackRecipeBuilder.painting(
              IngredientCreatorAccess.item().from(recolorInput),
              IngredientCreatorAccess.chemicalStack().from(MekanismChemicals.PIGMENT_COLOR_LOOKUP.get(color), PigmentExtractingRecipeProvider.DYE_RATE / 4),
              new ItemStack(result),
              false
        ).build(consumer, MekanismAdditions.rl(basePath + "recolor/painting/" + colorString));
    }

    public static void registerTransparentRecolor(RecipeOutput consumer, IItemProvider result, TagKey<Item> blockType, EnumColor color, String basePath) {
        Ingredient recolorInput = BaseRecipeProvider.difference(blockType, result);
        String colorString = color.getRegistryPrefix();
        DyeColor dye = color.getDyeColor();
        if (dye != null) {
            ExtendedShapedRecipeBuilder.shapedRecipe(result, 8)
                  .pattern(PLASTIC_TRANSPARENT)
                  .key(Pattern.CONSTANT, recolorInput)
                  .key(Pattern.DYE, dye.getTag())
                  .category(RecipeCategory.BUILDING_BLOCKS)
                  .build(consumer, MekanismAdditions.rl(basePath + "recolor/" + colorString));
        }
        ItemStackChemicalToItemStackRecipeBuilder.painting(
              IngredientCreatorAccess.item().from(recolorInput),
              IngredientCreatorAccess.chemicalStack().from(MekanismChemicals.PIGMENT_COLOR_LOOKUP.get(color), PigmentExtractingRecipeProvider.DYE_RATE / 8),
              new ItemStack(result),
              false
        ).build(consumer, MekanismAdditions.rl(basePath + "recolor/painting/" + colorString));
    }
}