package mekanism.client.jei.crafting;

import mekanism.common.recipe.ShapedMekanismRecipe;
import mezz.jei.api.recipe.IRecipeHandler;
import mezz.jei.api.recipe.IRecipeWrapper;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;

import javax.annotation.Nonnull;

public class ShapedMekanismRecipeHandler implements IRecipeHandler<ShapedMekanismRecipe>
{
	@Override
	public String getRecipeCategoryUid()
	{
		return VanillaRecipeCategoryUid.CRAFTING;
	}

	@Nonnull
	@Override
	public String getRecipeCategoryUid(@Nonnull ShapedMekanismRecipe recipe)
	{
		return VanillaRecipeCategoryUid.CRAFTING;
	}

	@Override
	public Class<ShapedMekanismRecipe> getRecipeClass() 
	{
		return ShapedMekanismRecipe.class;
	}

	@Override
	public IRecipeWrapper getRecipeWrapper(ShapedMekanismRecipe recipe)
	{
		return new ShapedMekanismRecipeWrapper(recipe);
	}

	@Override
	public boolean isRecipeValid(ShapedMekanismRecipe recipe)
	{
		return true;
	}
}
