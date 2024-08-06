package mekanism.generators.client.gui;

import java.util.List;
import mekanism.api.math.MathUtils;
import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.element.bar.GuiVerticalPowerBar;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiChemicalGauge;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.common.MekanismLang;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.util.text.EnergyDisplay;
import mekanism.generators.common.GeneratorsLang;
import mekanism.generators.common.tile.TileEntityGasGenerator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class GuiGasGenerator extends GuiMekanismTile<TileEntityGasGenerator, MekanismTileContainer<TileEntityGasGenerator>> {

    public GuiGasGenerator(MekanismTileContainer<TileEntityGasGenerator> container, Inventory inv, Component title) {
        super(container, inv, title);
        dynamicSlots = true;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        addRenderableWidget(new GuiEnergyTab(this, () -> {
            long productionAmount = MathUtils.clampToLong(tile.getGenerationRate() * tile.getUsed() * tile.getMaxBurnTicks());
            return List.of(
                  GeneratorsLang.PRODUCING_AMOUNT.translate(EnergyDisplay.of(productionAmount)),
                  MekanismLang.MAX_OUTPUT.translate(EnergyDisplay.of(tile.getMaxOutput())));
        }));
        addRenderableWidget(new GuiChemicalGauge(() -> tile.fuelTank, () -> tile.getChemicalTanks(null), GaugeType.WIDE, this, 55, 18));
        addRenderableWidget(new GuiVerticalPowerBar(this, tile.getEnergyContainer(), 164, 15));
    }

    @Override
    protected void drawForegroundText(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderTitleText(guiGraphics);
        drawString(guiGraphics, playerInventoryTitle, inventoryLabelX, inventoryLabelY, titleTextColor());
        Component component = GeneratorsLang.GAS_BURN_RATE.translate(tile.getUsed());
        int left = inventoryLabelX + getStringWidth(playerInventoryTitle) + 4;
        int end = imageWidth - 8;
        left = Math.max(left, end - getStringWidth(component));
        drawTextScaledBound(guiGraphics, component, left, inventoryLabelY, titleTextColor(), end - left);
        super.drawForegroundText(guiGraphics, mouseX, mouseY);
    }
}