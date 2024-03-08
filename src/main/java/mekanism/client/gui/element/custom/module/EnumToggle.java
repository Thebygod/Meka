package mekanism.client.gui.element.custom.module;

import java.util.List;
import mekanism.api.gear.config.IHasModeIcon;
import mekanism.api.gear.config.ModuleEnumData;
import mekanism.api.text.IHasTextComponent;
import mekanism.client.gui.GuiUtils;
import mekanism.common.MekanismLang;
import mekanism.common.content.gear.ModuleConfigItem;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

class EnumToggle<TYPE extends Enum<TYPE> & IHasTextComponent> extends MiniElement {

    private static final ResourceLocation SLIDER = MekanismUtils.getResource(ResourceType.GUI, "slider.png");
    private static final float TEXT_SCALE = 0.7F;
    private static final int BAR_START = 10;

    private final int BAR_LENGTH;
    private final ModuleConfigItem<TYPE> data;
    private final int optionDistance;
    private final boolean usesIcons;
    boolean dragging = false;

    EnumToggle(GuiModuleScreen parent, ModuleConfigItem<TYPE> data, int xPos, int yPos) {
        super(parent, xPos, yPos);
        this.data = data;
        BAR_LENGTH = this.parent.getScreenWidth() - 24;
        List<TYPE> options = getData().getEnums();
        this.optionDistance = (BAR_LENGTH / (options.size() - 1));
        this.usesIcons = options.stream().findFirst().filter(option -> option instanceof IHasModeIcon).isPresent();
    }

    @Override
    protected int getNeededHeight() {
        return usesIcons ? 31 : 28;
    }

    private ModuleEnumData<TYPE> getData() {
        return (ModuleEnumData<TYPE>) data.getData();
    }

    @Override
    protected void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int center = optionDistance * data.get().ordinal();
        guiGraphics.blit(SLIDER, getRelativeX() + BAR_START + center - 2, getRelativeY() + 11, 0, 0, 5, 6, 8, 8);
        guiGraphics.blit(SLIDER, getRelativeX() + BAR_START, getRelativeY() + 17, 0, 6, BAR_LENGTH, 2, 8, 8);
    }

    @Override
    protected void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int textColor = parent.screenTextColor();
        Component description = data.getDescription();
        if (usesIcons) {
            description = MekanismLang.GENERIC_STORED.translate(description, getData().get());
        }
        parent.drawTextWithScale(guiGraphics, description, getRelativeX() + 3, getRelativeY(), textColor, usesIcons ? 0.75F : 0.8F);
        List<TYPE> options = getData().getEnums();
        for (int i = 0, count = options.size(); i < count; i++) {
            int center = optionDistance * i;
            TYPE option = options.get(i);
            Component text = option.getTextComponent();
            //Similar to logic for drawScaledCenteredText except shifts values slightly if they go past the max length
            int textWidth = parent.getStringWidth(text);
            float widthScaling = usesIcons ? 2.5F : (textWidth / 2F) * TEXT_SCALE;
            int optionCenter = BAR_START + center;
            float left = optionCenter - widthScaling;
            if (left < 0) {
                left = 0;
            } else {
                int max = parent.getScreenWidth() - 1;
                float objectWidth = usesIcons ? 5 : textWidth * TEXT_SCALE;
                int end = xPos + Mth.ceil(left + objectWidth);
                if (end > max) {
                    left -= end - max;
                }
            }
            int color = textColor;
            if (text.getStyle().getColor() != null) {
                color = 0xFF000000 | text.getStyle().getColor().getValue();
            }
            GuiUtils.fill(guiGraphics, getRelativeX() + optionCenter, getRelativeY() + 17, 1, 3, color);
            if (usesIcons) {
                IHasModeIcon hasModeIcon = (IHasModeIcon) option;
                guiGraphics.blit(hasModeIcon.getModeIcon(), (int) (getRelativeX() + optionCenter - 8), getRelativeY() + 19, 0, 0, 16, 16, 16, 16);
            } else {
                parent.drawTextWithScale(guiGraphics, text, getRelativeX() + left, getRelativeY() + 20, textColor, TEXT_SCALE);
            }
        }
    }

    @Override
    protected void click(double mouseX, double mouseY) {
        if (!dragging) {
            int center = optionDistance * data.get().ordinal();
            if (mouseOver(mouseX, mouseY, BAR_START + center - 2, 11, 5, 6)) {
                dragging = true;
            } else if (mouseOver(mouseX, mouseY, BAR_START, 10, BAR_LENGTH, 12)) {
                setData(getData().getEnums(), mouseX);
            }
        }
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (dragging) {
            setData(getData().getEnums(), mouseX);
        }
    }

    private void setData(List<TYPE> options, double mouseX) {
        int size = options.size() - 1;
        int cur = (int) Math.round(((mouseX - getX() - BAR_START) / BAR_LENGTH) * size);
        cur = Mth.clamp(cur, 0, size);
        if (cur != data.get().ordinal()) {
            setData(data, options.get(cur));
        }
    }

    @Override
    protected void release(double mouseX, double mouseY) {
        dragging = false;
    }
}