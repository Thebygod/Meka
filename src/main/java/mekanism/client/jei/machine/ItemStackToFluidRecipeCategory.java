package mekanism.client.jei.machine;

import java.util.Collections;
import mekanism.api.providers.IItemProvider;
import mekanism.api.recipes.ItemStackToFluidRecipe;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiFluidGauge;
import mekanism.client.gui.element.gauge.GuiGauge;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.slot.GuiSlot;
import mekanism.client.gui.element.slot.SlotType;
import mekanism.client.jei.BaseRecipeCategory;
import mekanism.common.tile.component.config.DataType;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ItemStackToFluidRecipeCategory extends BaseRecipeCategory<ItemStackToFluidRecipe> {

    protected final GuiProgress progressBar;
    private final GuiGauge<?> output;
    private final GuiSlot input;

    public ItemStackToFluidRecipeCategory(IGuiHelper helper, IItemProvider provider, boolean isConversion) {
        this(helper, provider.getRegistryName(), provider.getTextComponent(), createIcon(helper, provider), isConversion);
    }

    public ItemStackToFluidRecipeCategory(IGuiHelper helper, ResourceLocation id, Component component, IDrawable icon, boolean isConversion) {
        super(helper, id, component, icon, 20, 12, 132, 62);
        output = addElement(GuiFluidGauge.getDummy(GaugeType.STANDARD.with(DataType.OUTPUT), this, 131, 13));
        input = addSlot(SlotType.INPUT, 26, 36);
        progressBar = addElement(new GuiProgress(isConversion ? () -> 1 : getSimpleProgressTimer(), ProgressType.LARGE_RIGHT, this, 64, 40));
    }

    @Override
    public Class<? extends ItemStackToFluidRecipe> getRecipeClass() {
        return ItemStackToFluidRecipe.class;
    }

    @Override
    public void setIngredients(ItemStackToFluidRecipe recipe, IIngredients ingredients) {
        ingredients.setInputLists(VanillaTypes.ITEM, Collections.singletonList(recipe.getInput().getRepresentations()));
        ingredients.setOutputLists(VanillaTypes.FLUID, Collections.singletonList(recipe.getOutputDefinition()));
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, ItemStackToFluidRecipe recipe, IIngredients ingredients) {
        initItem(recipeLayout.getItemStacks(), 0, true, input, recipe.getInput().getRepresentations());
        initFluid(recipeLayout.getFluidStacks(), 0, false, output, recipe.getOutputDefinition());
    }
}