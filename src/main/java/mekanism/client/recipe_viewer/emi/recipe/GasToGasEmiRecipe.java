package mekanism.client.recipe_viewer.emi.recipe;

import dev.emi.emi.api.widget.WidgetHolder;
import mekanism.api.recipes.GasToGasRecipe;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiGasGauge;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.slot.SlotType;
import mekanism.client.recipe_viewer.emi.MekanismEmiRecipeCategory;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.tile.component.config.DataType;
import net.minecraft.world.item.crafting.RecipeHolder;

public class GasToGasEmiRecipe extends MekanismEmiHolderRecipe<GasToGasRecipe> {

    public GasToGasEmiRecipe(MekanismEmiRecipeCategory category, RecipeHolder<GasToGasRecipe> recipeHolder) {
        super(category, recipeHolder);
        addInputDefinition(recipe.getInput());
        addChemicalOutputDefinition(recipe.getOutputDefinition());
    }

    @Override
    public void addWidgets(WidgetHolder widgetHolder) {
        addSlot(widgetHolder, SlotType.INPUT, 5, 56).with(SlotOverlay.MINUS);
        addSlot(widgetHolder, SlotType.OUTPUT, 155, 56).with(SlotOverlay.PLUS);
        initTank(widgetHolder, GuiGasGauge.getDummy(GaugeType.STANDARD.with(DataType.INPUT), this, 25, 13), input(0));
        initTank(widgetHolder, GuiGasGauge.getDummy(GaugeType.STANDARD.with(DataType.OUTPUT), this, 133, 13), output(0)).recipeContext(this);
        addConstantProgress(widgetHolder, ProgressType.LARGE_RIGHT, 64, 39);
    }
}