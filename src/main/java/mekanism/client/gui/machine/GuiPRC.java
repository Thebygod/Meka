package mekanism.client.gui.machine;

import mekanism.api.recipes.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.GuiElement;
import mekanism.client.gui.element.bar.GuiVerticalPowerBar;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiChemicalGauge;
import mekanism.client.gui.element.gauge.GuiFluidGauge;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.inventory.warning.WarningTracker.WarningType;
import mekanism.common.tile.machine.TileEntityPressurizedReactionChamber;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class GuiPRC extends GuiConfigurableTile<TileEntityPressurizedReactionChamber, MekanismTileContainer<TileEntityPressurizedReactionChamber>> {

    private GuiElement secondGauge;

    public GuiPRC(MekanismTileContainer<TileEntityPressurizedReactionChamber> container, Inventory inv, Component title) {
        super(container, inv, title);
        dynamicSlots = true;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        addRenderableWidget(new GuiEnergyTab(this, tile.getEnergyContainer(), tile::getActive));
        addRenderableWidget(new GuiFluidGauge(() -> tile.inputFluidTank, () -> tile.getFluidTanks(null), GaugeType.STANDARD, this, 5, 10)
              .warning(WarningType.NO_MATCHING_RECIPE, tile.getWarningCheck(TileEntityPressurizedReactionChamber.NOT_ENOUGH_FLUID_INPUT_ERROR)));
        secondGauge = addRenderableWidget(new GuiChemicalGauge(() -> tile.inputGasTank, () -> tile.getChemicalTanks(null), GaugeType.STANDARD, this, 28, 10)
              .warning(WarningType.NO_MATCHING_RECIPE, tile.getWarningCheck(TileEntityPressurizedReactionChamber.NOT_ENOUGH_CHEMICAL_INPUT_ERROR)));
        addRenderableWidget(new GuiChemicalGauge(() -> tile.outputGasTank, () -> tile.getChemicalTanks(null), GaugeType.SMALL, this, 140, 40)
              .warning(WarningType.NO_SPACE_IN_OUTPUT, tile.getWarningCheck(TileEntityPressurizedReactionChamber.NOT_ENOUGH_SPACE_GAS_OUTPUT_ERROR)));
        addRenderableWidget(new GuiVerticalPowerBar(this, tile.getEnergyContainer(), 163, 16)
              .warning(WarningType.NOT_ENOUGH_ENERGY, tile.getWarningCheck(RecipeError.NOT_ENOUGH_ENERGY)));
        addRenderableWidget(new GuiProgress(tile::getScaledProgress, ProgressType.RIGHT, this, 77, 38).recipeViewerCategory(tile))
              .warning(WarningType.INPUT_DOESNT_PRODUCE_OUTPUT, tile.getWarningCheck(RecipeError.INPUT_DOESNT_PRODUCE_OUTPUT));
    }

    @Override
    protected void drawForegroundText(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderTitleTextWithOffset(guiGraphics, secondGauge.getRelativeX() + secondGauge.getWidth(), getXSize(), 4, TextAlignment.LEFT);
        renderInventoryText(guiGraphics);
        super.drawForegroundText(guiGraphics, mouseX, mouseY);
    }
}