package mekanism.client.gui.element.scroll;

import com.mojang.blaze3d.vertex.PoseStack;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import mekanism.api.text.EnumColor;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiElement;
import mekanism.client.gui.element.slot.GuiSlot;
import mekanism.client.recipe_viewer.interfaces.IRecipeViewerIngredientHelper;
import mekanism.common.MekanismLang;
import mekanism.common.inventory.ISlotClickHandler;
import mekanism.common.inventory.ISlotClickHandler.IScrollableSlot;
import mekanism.common.lib.inventory.HashedItem;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import mekanism.common.util.text.TextUtils;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GuiSlotScroll extends GuiElement implements IRecipeViewerIngredientHelper {

    private static final ResourceLocation SLOTS = MekanismUtils.getResource(ResourceType.GUI_SLOT, "slots.png");
    private static final ResourceLocation SLOTS_DARK = MekanismUtils.getResource(ResourceType.GUI_SLOT, "slots_dark.png");
    private static final DecimalFormat COUNT_FORMAT = Util.make(new DecimalFormat("#.#"), format -> format.setRoundingMode(RoundingMode.FLOOR));

    private final GuiScrollBar scrollBar;

    private final int xSlots, ySlots;
    private final Supplier<@Nullable List<IScrollableSlot>> slotList;
    private final ISlotClickHandler clickHandler;

    public GuiSlotScroll(IGuiWrapper gui, int x, int y, int xSlots, int ySlots, Supplier<@Nullable List<IScrollableSlot>> slotList, ISlotClickHandler clickHandler) {
        super(gui, x, y, xSlots * 18 + 18, ySlots * 18);
        this.xSlots = xSlots;
        this.ySlots = ySlots;
        this.slotList = slotList;
        this.clickHandler = clickHandler;
        scrollBar = addChild(new GuiScrollBar(gui, relativeX + xSlots * 18 + 4, y, ySlots * 18,
              () -> getSlotList() == null ? 0 : Mth.ceil((double) getSlotList().size() / xSlots), () -> ySlots));
    }

    @Override
    public void drawBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.drawBackground(guiGraphics, mouseX, mouseY, partialTicks);
        List<IScrollableSlot> list = getSlotList();
        ResourceLocation resource = list == null ? SLOTS_DARK : SLOTS;
        guiGraphics.blit(resource, relativeX, relativeY, 0, 0, xSlots * 18, ySlots * 18, 288, 288);
        if (list != null) {
            int slotStart = scrollBar.getCurrentSelection() * xSlots, max = xSlots * ySlots;
            for (int i = 0; i < max; i++) {
                int slot = slotStart + i;
                // terminate if we've exceeded max slot pos
                if (slot >= list.size()) {
                    break;
                }
                int slotX = relativeX + (i % xSlots) * 18, slotY = relativeY + (i / xSlots) * 18;
                renderSlot(guiGraphics, list.get(slot), slotX, slotY);
            }
        }
    }

    @Override
    public void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderForeground(guiGraphics, mouseX, mouseY);
        int xAxis = mouseX - getGuiLeft(), yAxis = mouseY - getGuiTop();
        int slotX = (xAxis - relativeX) / 18, slotY = (yAxis - relativeY) / 18;
        if (slotX >= 0 && slotY >= 0 && slotX < xSlots && slotY < ySlots) {
            int slotStartX = relativeX + slotX * 18 + 1, slotStartY = relativeY + slotY * 18 + 1;
            if (xAxis >= slotStartX && xAxis < slotStartX + 16 && yAxis >= slotStartY && yAxis < slotStartY + 16 && checkWindows(mouseX, mouseY)) {
                guiGraphics.fill(RenderType.guiOverlay(), slotStartX, slotStartY, slotStartX + 16, slotStartY + 16, GuiSlot.DEFAULT_HOVER_COLOR);
            }
        }
    }

    @Override
    public void renderToolTip(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderToolTip(guiGraphics, mouseX, mouseY);
        IScrollableSlot slot = getSlot(mouseX, mouseY);
        if (slot != null) {
            renderSlotTooltip(guiGraphics, slot, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double xDelta, double yDelta) {
        return scrollBar.adjustScroll(yDelta) || super.mouseScrolled(mouseX, mouseY, xDelta, yDelta);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (gui().currentlyQuickCrafting()) {
            //If the player is currently quick crafting don't do any special handling for as if they clicked in the screen
            return super.mouseReleased(mouseX, mouseY, button);
        }
        super.mouseReleased(mouseX, mouseY, button);
        clickHandler.onClick(() -> getSlot(mouseX, mouseY), button, Screen.hasShiftDown(), gui().getCarriedItem());
        return true;
    }

    private IScrollableSlot getSlot(double mouseX, double mouseY) {
        List<IScrollableSlot> list = getSlotList();
        if (list == null) {
            return null;
        }
        int slotX = (int) ((mouseX - getX()) / 18), slotY = (int) ((mouseY - getY()) / 18);
        // terminate if we clicked the border of a slot
        int slotStartX = getX() + slotX * 18 + 1, slotStartY = getY() + slotY * 18 + 1;
        if (mouseX < slotStartX || mouseX >= slotStartX + 16 || mouseY < slotStartY || mouseY >= slotStartY + 16) {
            return null;
        }
        // terminate if we aren't looking at a slot on-screen
        if (slotX < 0 || slotY < 0 || slotX >= xSlots || slotY >= ySlots) {
            return null;
        }
        int slot = (slotY + scrollBar.getCurrentSelection()) * xSlots + slotX;
        // terminate if the slot doesn't exist
        if (slot >= list.size()) {
            return null;
        }
        return list.get(slot);
    }

    private void renderSlot(GuiGraphics guiGraphics, IScrollableSlot slot, int slotX, int slotY) {
        // sanity checks
        if (isSlotEmpty(slot)) {
            return;
        }
        gui().renderItemWithOverlay(guiGraphics, slot.item().getInternalStack(), slotX + 1, slotY + 1, 1, "");
        if (slot.count() > 1) {
            renderSlotText(guiGraphics, getCountText(slot.count()), slotX + 1, slotY + 1);
        }
    }

    private void renderSlotTooltip(GuiGraphics guiGraphics, IScrollableSlot slot, int slotX, int slotY) {
        // sanity checks
        if (isSlotEmpty(slot)) {
            return;
        }
        ItemStack stack = slot.item().getInternalStack();
        long count = slot.count();
        if (count < 10_000) {
            gui().renderItemTooltip(guiGraphics, stack, slotX, slotY);
        } else {
            //If the slot's displayed count is truncated, make sure we also add the actual amount to the tooltip
            gui().renderItemTooltipWithExtra(guiGraphics, stack, slotX, slotY, Collections.singletonList(MekanismLang.QIO_STORED_COUNT.translateColored(EnumColor.GRAY,
                  EnumColor.INDIGO, TextUtils.format(count))));
        }
    }

    private boolean isSlotEmpty(IScrollableSlot slot) {
        if (slot.count() == 0) {
            //Count is not expected to be zero, but validate it anyway
            return true;
        }
        //Slot's item is not null in default impl, but check in case we make it null at some point
        // and also validate if the internal stack is empty in case it is raw and there is some edge case
        HashedItem item = slot.item();
        return item == null || item.getInternalStack().isEmpty();
    }

    private void renderSlotText(GuiGraphics guiGraphics, String text, int x, int y) {
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        float scale = 0.6F;
        int width = getFont().width(text);
        //If we need a lower scale due to having a lot of text, calculate it
        scale = Math.min(1, 16F / (width * scale)) * scale;
        float yAdd = 4 - (scale * 8) / 2F;
        pose.translate(x + 16 - width * scale, y + 9 + yAdd, 200F);
        pose.scale(scale, scale, scale);

        guiGraphics.drawString(getFont(), text, 0, 0, 0xFFFFFF);
        pose.popPose();
    }

    private String getCountText(long count) {
        //Note: For cases like 9,999,999 we intentionally display as 9999.9K instead of 10M so that people
        // do not think they have more stored than they actually have just because it is rounding up
        if (count <= 1) {
            return null;
        } else if (count < 10_000) {
            return Long.toString(count);
        } else if (count < 10_000_000) {
            return COUNT_FORMAT.format(count / 1_000D) + "K";
        } else if (count < 10_000_000_000L) {
            return COUNT_FORMAT.format(count / 1_000_000D) + "M";
        } else if (count < 10_000_000_000_000L) {
            return COUNT_FORMAT.format(count / 1_000_000_000D) + "B";
        }
        return ">10T";
    }

    private List<IScrollableSlot> getSlotList() {
        return slotList.get();
    }

    @Override
    public Optional<?> getIngredient(double mouseX, double mouseY) {
        IScrollableSlot slot = getSlot(mouseX, mouseY);
        return slot == null ? Optional.empty() : Optional.of(slot.item().getInternalStack());
    }

    @Override
    public Rect2i getIngredientBounds(double mouseX, double mouseY) {
        List<IScrollableSlot> list = getSlotList();
        if (list != null) {
            int slotX = (int) ((mouseX - getX()) / 18), slotY = (int) ((mouseY - getY()) / 18);
            int slotStartX = getX() + slotX * 18 + 1, slotStartY = getY() + slotY * 18 + 1;
            if (mouseX >= slotStartX && mouseX < slotStartX + 16 && mouseY >= slotStartY && mouseY < slotStartY + 16) {
                return new Rect2i(slotStartX + 1, slotStartY + 1, 16, 16);
            }
        }
        //Note: This should never be the case as we validated we had an ingredient but if it is just return the entire gui portion
        return new Rect2i(getX(), getY(), width, height);
    }
}