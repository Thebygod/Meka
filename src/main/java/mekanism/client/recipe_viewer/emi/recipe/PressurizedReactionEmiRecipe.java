package mekanism.client.recipe_viewer.emi.recipe;

import dev.emi.emi.api.widget.WidgetHolder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.functions.ConstantPredicates;
import mekanism.api.recipes.PressurizedReactionRecipe;
import mekanism.api.recipes.PressurizedReactionRecipe.PressurizedReactionRecipeOutput;
import mekanism.client.gui.element.bar.GuiVerticalPowerBar;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiChemicalGauge;
import mekanism.client.gui.element.gauge.GuiFluidGauge;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.slot.SlotType;
import mekanism.client.recipe_viewer.RecipeViewerUtils;
import mekanism.client.recipe_viewer.emi.MekanismEmiRecipeCategory;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.tile.component.config.DataType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

public class PressurizedReactionEmiRecipe extends MekanismEmiHolderRecipe<PressurizedReactionRecipe> {

    public PressurizedReactionEmiRecipe(MekanismEmiRecipeCategory category, RecipeHolder<PressurizedReactionRecipe> recipeHolder) {
        super(category, recipeHolder);
        addInputDefinition(recipe.getInputSolid());
        addInputDefinition(recipe.getInputFluid());
        addInputDefinition(recipe.getInputGas());
        List<ItemStack> itemOutputs = new ArrayList<>();
        List<ChemicalStack> gasOutputs = new ArrayList<>();
        for (PressurizedReactionRecipeOutput output : recipe.getOutputDefinition()) {
            itemOutputs.add(output.item());
            gasOutputs.add(output.gas());
        }
        if (itemOutputs.stream().allMatch(ConstantPredicates.ITEM_EMPTY)) {
            addOutputDefinition(Collections.emptyList());
        } else {
            addItemOutputDefinition(itemOutputs);
        }
        if (gasOutputs.stream().allMatch(ConstantPredicates.CHEMICAL_EMPTY)) {
            addOutputDefinition(Collections.emptyList());
        } else {
            addChemicalOutputDefinition(gasOutputs);
        }
    }

    @Override
    public void addWidgets(WidgetHolder widgetHolder) {
        addSlot(widgetHolder, SlotType.INPUT, 54, 35, input(0));
        addSlot(widgetHolder, SlotType.OUTPUT, 116, 35, output(0)).recipeContext(this);
        addSlot(widgetHolder, SlotType.POWER, 141, 17).with(SlotOverlay.POWER);
        initTank(widgetHolder, GuiFluidGauge.getDummy(GaugeType.STANDARD.with(DataType.INPUT), this, 5, 10), input(1));
        GaugeType type1 = GaugeType.STANDARD.with(DataType.INPUT);
        initTank(widgetHolder, GuiChemicalGauge.getDummy(type1, this, 28, 10), input(2));
        GaugeType type = GaugeType.SMALL.with(DataType.OUTPUT);
        initTank(widgetHolder, GuiChemicalGauge.getDummy(type, this, 140, 40), output(1)).recipeContext(this);
        addElement(widgetHolder, new GuiVerticalPowerBar(this, RecipeViewerUtils.FULL_BAR, 164, 15));
        addSimpleProgress(widgetHolder, ProgressType.RIGHT, 77, 38, recipe.getDuration());
    }
}