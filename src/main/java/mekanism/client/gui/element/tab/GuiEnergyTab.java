package mekanism.client.gui.element.tab;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.UnaryOperator;
import mekanism.api.IIncrementalEnum;
import mekanism.api.math.FloatingLong;
import mekanism.api.math.FloatingLongSupplier;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiTexturedElement;
import mekanism.common.MekanismLang;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.config.MekanismConfig;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import mekanism.common.util.UnitDisplayUtils.EnergyUnit;
import mekanism.common.util.text.EnergyDisplay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public class GuiEnergyTab extends GuiTexturedElement {

    private static final Map<EnergyUnit, ResourceLocation> ICONS = new EnumMap<>(EnergyUnit.class);
    private final IInfoHandler infoHandler;

    public GuiEnergyTab(IGuiWrapper gui, IInfoHandler handler) {
        super(MekanismUtils.getResource(ResourceType.GUI_TAB, "energy_info.png"), gui, -26, 137, 26, 26);
        infoHandler = handler;
    }

    public GuiEnergyTab(IGuiWrapper gui, MachineEnergyContainer<?> energyContainer, FloatingLongSupplier lastEnergyUsed) {
        this(gui, () -> List.of(MekanismLang.USING.translate(EnergyDisplay.of(lastEnergyUsed.get())),
              MekanismLang.NEEDED.translate(EnergyDisplay.of(energyContainer.getNeeded()))));
    }

    //TODO: Re-evaluate uses of this constructor at some point
    public GuiEnergyTab(IGuiWrapper gui, MachineEnergyContainer<?> energyContainer, BooleanSupplier isActive) {
        this(gui, () -> {
            //Note: This isn't the most accurate using calculation as deactivation doesn't sync instantly
            // to the client, but it is close enough given a lot more things would have to be kept track of otherwise
            // which would lead to higher memory usage
            FloatingLong using = isActive.getAsBoolean() ? energyContainer.getEnergyPerTick() : FloatingLong.ZERO;
            return List.of(MekanismLang.USING.translate(EnergyDisplay.of(using)),
                  MekanismLang.NEEDED.translate(EnergyDisplay.of(energyContainer.getNeeded())));
        });
    }

    @Override
    public void drawBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.drawBackground(guiGraphics, mouseX, mouseY, partialTicks);
        guiGraphics.blit(getResource(), relativeX, relativeY, 0, 0, width, height, width, height);
    }

    @Override
    public void renderToolTip(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderToolTip(guiGraphics, mouseX, mouseY);
        List<Component> info = new ArrayList<>(infoHandler.getInfo());
        info.add(MekanismLang.UNIT.translate(EnergyUnit.getConfigured()));
        displayTooltips(guiGraphics, mouseX, mouseY, info);
    }

    @Override
    protected ResourceLocation getResource() {
        return ICONS.computeIfAbsent(EnergyUnit.getConfigured(), type -> MekanismUtils.getResource(ResourceType.GUI_TAB,
              "energy_info_" + type.getTabName() + ".png"));
    }

    @Override
    public void onClick(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_1) {
            updateEnergyUnit(IIncrementalEnum::getNext);
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_2) {
            updateEnergyUnit(IIncrementalEnum::getPrevious);
        }
    }

    @Override
    public boolean isValidClickButton(int button) {
        return button == GLFW.GLFW_MOUSE_BUTTON_1 || button == GLFW.GLFW_MOUSE_BUTTON_2;
    }

    private void updateEnergyUnit(UnaryOperator<EnergyUnit> converter) {
        EnergyUnit current = EnergyUnit.getConfigured();
        EnergyUnit updated = converter.apply(current);
        if (current != updated) {//May be equal if all other energy types are disabled
            MekanismConfig.common.energyUnit.set(updated);
            MekanismConfig.common.save();
        }
    }
}