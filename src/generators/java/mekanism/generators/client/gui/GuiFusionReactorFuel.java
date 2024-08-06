package mekanism.generators.client.gui;

import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiChemicalGauge;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.text.GuiTextField;
import mekanism.common.inventory.container.tile.EmptyTileContainer;
import mekanism.common.network.PacketUtils;
import mekanism.common.util.text.InputValidator;
import mekanism.generators.client.gui.element.GuiFusionReactorTab;
import mekanism.generators.client.gui.element.GuiFusionReactorTab.FusionReactorTab;
import mekanism.generators.common.GeneratorsLang;
import mekanism.generators.common.network.to_server.PacketGeneratorsGuiInteract;
import mekanism.generators.common.network.to_server.PacketGeneratorsGuiInteract.GeneratorsGuiInteraction;
import mekanism.generators.common.tile.fusion.TileEntityFusionReactorController;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class GuiFusionReactorFuel extends GuiFusionReactorInfo {

    private GuiTextField injectionRateField;

    public GuiFusionReactorFuel(EmptyTileContainer<TileEntityFusionReactorController> container, Inventory inv, Component title) {
        super(container, inv, title);
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        addRenderableWidget(new GuiChemicalGauge(() -> tile.getMultiblock().deuteriumTank, () -> tile.getMultiblock().getChemicalTanks(null), GaugeType.SMALL, this, 25, 64));
        addRenderableWidget(new GuiChemicalGauge(() -> tile.getMultiblock().fuelTank, () -> tile.getMultiblock().getChemicalTanks(null), GaugeType.STANDARD, this, 79, 50));
        addRenderableWidget(new GuiChemicalGauge(() -> tile.getMultiblock().tritiumTank, () -> tile.getMultiblock().getChemicalTanks(null), GaugeType.SMALL, this, 133, 64));
        addRenderableWidget(new GuiProgress(() -> tile.getMultiblock().isBurning(), ProgressType.SMALL_RIGHT, this, 47, 76));
        addRenderableWidget(new GuiProgress(() -> tile.getMultiblock().isBurning(), ProgressType.SMALL_LEFT, this, 101, 76));
        addRenderableWidget(new GuiFusionReactorTab(this, tile, FusionReactorTab.HEAT));
        addRenderableWidget(new GuiFusionReactorTab(this, tile, FusionReactorTab.STAT));
        injectionRateField = addRenderableWidget(new GuiTextField(this, 98, 115, 26, 11));
        injectionRateField.setInputValidator(InputValidator.DIGIT)
              .setEnterHandler(this::setInjection)
              .setMaxLength(2);
        setInitialFocus(injectionRateField);
    }

    @Override
    protected void drawForegroundText(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderTitleText(guiGraphics);
        drawCenteredText(guiGraphics, GeneratorsLang.REACTOR_INJECTION_RATE.translate(tile.getMultiblock().getInjectionRate()), 0, imageWidth, 35, titleTextColor());
        drawString(guiGraphics, GeneratorsLang.REACTOR_EDIT_RATE.translate(), 50, 117, titleTextColor());
        super.drawForegroundText(guiGraphics, mouseX, mouseY);
    }

    private void setInjection() {
        if (!injectionRateField.getText().isEmpty()) {
            PacketUtils.sendToServer(new PacketGeneratorsGuiInteract(GeneratorsGuiInteraction.INJECTION_RATE, tile, Integer.parseInt(injectionRateField.getText())));
            injectionRateField.setText("");
        }
    }
}