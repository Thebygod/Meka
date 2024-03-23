package mekanism.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import mekanism.api.text.ILangEntry;
import mekanism.client.gui.element.GuiElement;
import mekanism.client.gui.element.GuiElement.IHoverable;
import mekanism.client.gui.element.slot.GuiSlot;
import mekanism.client.gui.element.slot.GuiVirtualSlot;
import mekanism.client.gui.element.slot.SlotType;
import mekanism.client.gui.element.tab.GuiWarningTab;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.client.render.IFancyFontRenderer;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.Mekanism;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.SelectedWindowData;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.inventory.container.slot.IVirtualSlot;
import mekanism.common.inventory.container.slot.InventoryContainerSlot;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.inventory.warning.IWarningTracker;
import mekanism.common.inventory.warning.WarningTracker;
import mekanism.common.inventory.warning.WarningTracker.WarningType;
import mekanism.common.lib.collection.LRU;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.interfaces.ISideConfiguration;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public abstract class GuiMekanism<CONTAINER extends AbstractContainerMenu> extends VirtualSlotContainerScreen<CONTAINER> implements IGuiWrapper, IFancyFontRenderer {

    public static final ResourceLocation BASE_BACKGROUND = MekanismUtils.getResource(ResourceType.GUI, "base.png");
    public static final ResourceLocation SHADOW = MekanismUtils.getResource(ResourceType.GUI, "shadow.png");
    public static final ResourceLocation BLUR = MekanismUtils.getResource(ResourceType.GUI, "blur.png");
    //TODO: Look into defaulting this to true
    protected boolean dynamicSlots;
    protected final LRU<GuiWindow> windows = new LRU<>();
    public boolean switchingToRecipeViewer;
    @Nullable
    private IWarningTracker warningTracker;

    private boolean hasClicked = false;

    public static int maxZOffset;

    protected GuiMekanism(CONTAINER container, Inventory inv, Component title) {
        super(container, inv, title);
    }

    @NotNull
    @Override
    public BooleanSupplier trackWarning(@NotNull WarningType type, @NotNull BooleanSupplier warningSupplier) {
        if (warningTracker == null) {
            warningTracker = new WarningTracker();
        }
        return warningTracker.trackWarning(type, warningSupplier);
    }

    @Override
    public void removed() {
        if (!switchingToRecipeViewer) {
            //If we are not switching to JEI then run the super close method
            // which will exit the container. We don't want to mark the
            // container as exited if it will be revived when leaving JEI
            // Note: We start by closing all open windows so that any cleanup
            // they need to have done such as saving positions can be done
            windows.forEach(GuiWindow::close);
            super.removed();
        }
    }

    @Override
    protected void init() {
        super.init();
        if (warningTracker != null) {
            //If our warning tracker isn't null (so this isn't the first time we are initializing, such as after resizing)
            // clear out any tracked warnings, so we don't have duplicates being tracked when we add our elements again
            warningTracker.clearTrackedWarnings();
        }
        addGuiElements();
        if (warningTracker != null) {
            //If we have a warning tracker add it as a button, we do so via a method in case any of the sub GUIs need to reposition where it ends up
            addWarningTab(warningTracker);
        }
        initPinnedWindows();
    }

    protected void initPinnedWindows() {
        if (this.windows.isEmpty()) {
            //TODO: Improve support for this if we have windows that are opened from other windows
            for (GuiEventListener child : children()) {
                if (child instanceof GuiElement element) {
                    element.openPinnedWindows();
                }
            }
        }
    }

    protected void addWarningTab(IWarningTracker warningTracker) {
        addRenderableWidget(new GuiWarningTab(this, warningTracker, 109));
    }

    /**
     * Called to add gui elements to the GUI. Add elements before calling super if they should be before the slots, and after if they should be after the slots. Most
     * elements can and should be added after the slots.
     */
    protected void addGuiElements() {
        if (dynamicSlots) {
            addSlots();
        }
    }

    /**
     * Like {@link #addRenderableWidget(GuiEventListener)}, except doesn't add the element as narratable.
     */
    protected <T extends GuiElement> T addElement(T element) {
        renderables.add(element);
        ((List<GuiEventListener>) children()).add(element);
        return element;
    }

    protected <T extends GuiElement> T addRenderableWidget(T element) {
        //TODO: At some point we want to replace calls of this to directly call addElement, and then add in better support
        // for the narrator and what is currently focused and implement in some gui elements updateNarration rather than
        // just have it NO-OP. We currently redirect this to our version that doesn't add it as narratable so that we don't
        // have hitting tab with the narrator on just list indices
        return addElement(element);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        children().stream().filter(child -> child instanceof GuiElement).map(child -> (GuiElement) child).forEach(GuiElement::tick);
        windows.forEach(GuiWindow::tick);
    }

    protected void renderTitleText(GuiGraphics guiGraphics) {
        drawTitleText(guiGraphics, title, titleLabelY);
    }

    protected IHoverable getOnHover(ILangEntry translationHelper) {
        return getOnHover((Supplier<Component>) translationHelper::translate);
    }

    protected IHoverable getOnHover(Supplier<Component> componentSupplier) {
        return (onHover, guiGraphics, mouseX, mouseY) -> displayTooltips(guiGraphics, mouseX, mouseY, componentSupplier.get());
    }

    protected ResourceLocation getButtonLocation(String name) {
        return MekanismUtils.getResource(ResourceType.GUI_BUTTON, name + ".png");
    }

    @NotNull
    @Override
    public ItemStack getCarriedItem() {
        return getMenu().getCarried();
    }

    @Nullable
    private ComponentPath handleNavigationWithWindows(Function<ContainerEventHandler, @Nullable ComponentPath> handleNavigation) {
        GuiWindow topWindow = windows.iterator().next();
        List<GuiEventListener> combinedChildren;
        //Note: As allowContainer wise only allows interacting with slots, which we don't have navigation for, we check against allowAll.
        // If we are allowed to interact with everything, we want to include all the windows and children in the navigation check,
        // otherwise we only include the top window, as while it should already be focused, maybe it isn't, so we need to make sure
        // to handle checking navigation on it
        if (topWindow.getInteractionStrategy().allowAll()) {
            combinedChildren = new ArrayList<>(windows);
            combinedChildren.addAll(children());
        } else {
            combinedChildren = List.of(topWindow);
        }
        ContainerEventHandler handlerWithWindows = new ContainerEventHandler() {
            @NotNull
            @Override
            public List<? extends GuiEventListener> children() {
                return combinedChildren;
            }

            @Override
            public boolean isDragging() {
                return GuiMekanism.this.isDragging();
            }

            @Override
            public void setDragging(boolean dragging) {
            }

            @Nullable
            @Override
            public GuiEventListener getFocused() {
                return GuiMekanism.this.getFocused();
            }

            @Override
            public void setFocused(@Nullable GuiEventListener focused) {
            }

            @NotNull
            @Override
            public ScreenRectangle getRectangle() {
                return GuiMekanism.this.getRectangle();
            }
        };
        ComponentPath componentPath = handleNavigation.apply(handlerWithWindows);
        if (componentPath == null) {
            return null;
        } else if (componentPath instanceof ComponentPath.Path path) {
            if (path.component() == handlerWithWindows) {
                //Replace the root of the path with ourselves rather than our fake wrapper
                return ComponentPath.path(this, path.childPath());
            }
        }
        return componentPath;
    }

    @Nullable
    @Override
    public ComponentPath handleTabNavigation(@NotNull FocusNavigationEvent.TabNavigation navigation) {
        //Note: We have to AT this method and the arrow navigation one, as Screen explicitly calls super.nextFocusPath,
        // so we can't get away with just overriding getFocusPath
        if (windows.isEmpty()) {
            return super.handleTabNavigation(navigation);
        }
        return handleNavigationWithWindows(handlerWithWindows -> handlerWithWindows.handleTabNavigation(navigation));
    }

    @Nullable
    @Override
    public ComponentPath handleArrowNavigation(@NotNull FocusNavigationEvent.ArrowNavigation navigation) {
        if (windows.isEmpty()) {
            return super.handleArrowNavigation(navigation);
        }
        return handleNavigationWithWindows(handlerWithWindows -> handlerWithWindows.handleArrowNavigation(navigation));
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeftIn, int guiTopIn, int mouseButton) {
        return getWindowHovering(mouseX, mouseY) == null && super.hasClickedOutside(mouseX, mouseY, guiLeftIn, guiTopIn, mouseButton);
    }

    @Override
    protected void repositionElements() {
        //Mark that we are not switching to JEI if we start being initialized again
        // Note: We can do this here as the screen will always have initialized as true, so we don't need to definalize init(mc, width, height)
        // as it will never potentially have init() with no params be the call path
        // Additionally, as the screen is not actively being used we shouldn't have cases this is called from resize while we are not present
        // and setting this to false when it is already false does nothing
        switchingToRecipeViewer = false;
        super.repositionElements();
    }

    @Override
    protected void rebuildWidgets() {
        //Gather any persistent data from existing elements
        record PreviousElement(int index, GuiElement element, boolean wasFocus) {
        }
        List<PreviousElement> prevElements = new ArrayList<>();
        GuiEventListener previousFocused = getFocused();
        for (int i = 0; i < children().size(); i++) {
            GuiEventListener widget = children().get(i);
            if (widget instanceof GuiElement element) {
                boolean wasPreviousFocus = element == previousFocused;
                if (wasPreviousFocus || element.hasPersistentData()) {
                    prevElements.add(new PreviousElement(i, element, wasPreviousFocus));
                }
            }
        }
        int prevLeft = leftPos, prevTop = topPos;
        //Allow the elements to be cleared and reinitialized
        super.rebuildWidgets();

        //Resize any windows as we can't easily just rebuild them
        windows.forEach(window -> window.resize(prevLeft, prevTop, leftPos, topPos));

        //And set any persistent data that we stored
        prevElements.forEach(e -> {
            if (e.index() < children().size()) {
                GuiEventListener widget = children().get(e.index());
                // we're forced to assume that the children list is the same before and after the resize.
                // for verification, we run a lightweight class equality check
                // Note: We do not perform an instance check on widget to ensure it is a GuiElement, as that is
                // ensured by the class comparison, and the restrictions of what can go in prevElements
                if (widget.getClass() == e.element().getClass()) {
                    ((GuiElement) widget).syncFrom(e.element());
                    if (e.wasFocus()) {
                        setFocused(widget);
                    }
                }
            }
        });
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        //PoseStack modelViewStack = RenderSystem.getModelViewStack();
        //Note: We intentionally don't push the modelViewStack, see notes further down in this method for more details
        //TODO - 1.20: Figure this out as some transforms and hacks may be unnecessary now
        PoseStack pose = guiGraphics.pose();
        //TODO - 1.20: Re-evaluate?? this always was against the pose but what is it for
        pose.translate(0, 0, 300);
        children().stream().filter(c -> c instanceof GuiElement).forEach(c -> ((GuiElement) c).onDrawBackground(guiGraphics, mouseX, mouseY, MekanismRenderer.getPartialTick()));
        drawForegroundText(guiGraphics, mouseX, mouseY);
        // first render general foregrounds
        int zOffset = 200;
        maxZOffset = zOffset;
        for (GuiEventListener widget : children()) {
            if (widget instanceof GuiElement element) {
                pose.pushPose();
                element.onRenderForeground(guiGraphics, mouseX, mouseY, zOffset, zOffset);
                pose.popPose();
            }
        }

        // now render overlays in reverse-order (i.e. back to front)
        for (LRU<GuiWindow>.LRUIterator iter = getWindowsDescendingIterator(); iter.hasNext(); ) {
            GuiWindow overlay = iter.next();
            //Max z offset is incremented based on what is the deepest level offset we go to
            // if our gui isn't flagged as visible we won't increment it as nothing is drawn
            // we need to do this based on what the max is after having rendered the previous
            // window as while the windows don't necessarily overlap, if they do we want to
            // ensure that there is no clipping
            zOffset = maxZOffset + 150;
            pose.pushPose();
            overlay.onRenderForeground(guiGraphics, mouseX, mouseY, zOffset, zOffset);
            if (iter.hasNext()) {
                // if this isn't the focused window, render a 'blur' effect over it
                overlay.renderBlur(guiGraphics);
            }
            pose.popPose();
        }
        // then render tooltips, translating above max z offset to prevent clashing
        GuiElement tooltipElement = getWindowHovering(mouseX, mouseY);
        if (tooltipElement == null) {
            tooltipElement = (GuiElement) GuiUtils.findChild(children(), child -> child instanceof GuiElement && child.isMouseOver(mouseX, mouseY));
        }

        // translate forwards using RenderSystem. this should never have to happen as we do all the necessary translations with MatrixStacks,
        // but Minecraft has decided to not fully adopt MatrixStacks for many crucial ContainerScreen render operations. should be re-evaluated
        // when mc updates related logic on their end (IMPORTANT)
        //TODO - 1.20: Is this necessary used to occur on the model view stack
        pose.translate(0, 0, maxZOffset);

        // render tooltips
        //TODO - 1.20: Can we remove these transforms that are around the tooltip rendering
        // No, we maybe could remove from the tooltip element portion but definitely not from the stack rendering/parent renderTooltip method
        pose.translate(-leftPos, -topPos, 0);
        //modelViewStack.translate(-leftPos, -topPos, 0);
        //RenderSystem.applyModelViewMatrix();
        if (tooltipElement != null) {
            tooltipElement.renderToolTip(guiGraphics, mouseX, mouseY);
        }
        renderTooltip(guiGraphics, mouseX, mouseY);
        pose.translate(leftPos, topPos, 0);
        //modelViewStack.translate(leftPos, topPos, 0);

        // IMPORTANT: additional hacky translation so held items render okay. re-evaluate as discussed above
        // Note: It is important that we don't wrap our adjustments to the modelViewStack in so that we can
        // have the adjustments to the z-value persist into the vanilla methods
        //TODO - 1.20: Is this necessary (used to happen to the model view stack)
        pose.translate(0, 0, 200);
    }

    protected void drawForegroundText(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    @NotNull
    @Override
    public Optional<GuiEventListener> getChildAt(double mouseX, double mouseY) {
        GuiWindow window = getWindowHovering(mouseX, mouseY);
        return window == null ? super.getChildAt(mouseX, mouseY) : Optional.of(window);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double xDelta, double yDelta) {
        // first try to send the mouse event to our focused window
        GuiWindow top = windows.isEmpty() ? null : windows.iterator().next();
        if (top != null) {
            boolean windowScroll = top.mouseScrolled(mouseX, mouseY, xDelta, yDelta);
            if (windowScroll || !top.getInteractionStrategy().allowAll()) {
                //If our focused window was able to handle the scroll or doesn't allow interacting with
                // things outside the window, return our scroll result
                return windowScroll;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, xDelta, yDelta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        hasClicked = true;
        // first try to send the mouse event to our overlays
        GuiWindow top = windows.isEmpty() ? null : windows.iterator().next();
        for (GuiWindow overlay : windows) {
            if (overlay.mouseClicked(mouseX, mouseY, button)) {
                if (windows.contains(overlay)) {
                    //Validate that the focused window is still one of our windows, as if it wasn't focused/on top, and
                    // it is being closed, we don't want to update and mark it as focused, as our defocusing code won't
                    // run as we ran it when we pressed the button
                    setFocused(overlay);
                    if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                        setDragging(true);
                    }
                    // this check prevents us from moving the window to the top of the stack if the clicked window opened up an additional window
                    if (top != overlay) {
                        top.onFocusLost();
                        windows.moveUp(overlay);
                        overlay.onFocused();
                    }
                }
                return true;
            }
        }
        // otherwise, we send it to the current element (this is the same as super.super [ContainerEventHandler#mouseClicked], but in reverse order)
        //TODO: Why do we do this in reverse order?
        GuiEventListener clickedChild = GuiUtils.findChild(children(), child -> child.mouseClicked(mouseX, mouseY, button));
        if (clickedChild != null) {
            setFocused(clickedChild);
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                setDragging(true);
            }
            return true;
        } else {
            //If we can't find a child, allow clearing whatever focus we currently have
            clearFocus();
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (hasClicked) {
            // always pass mouse released events to windows for drag checks
            windows.forEach(w -> w.onRelease(mouseX, mouseY));
            return super.mouseReleased(mouseX, mouseY, button);
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return windows.stream().anyMatch(window -> window.keyPressed(keyCode, scanCode, modifiers)) ||
               GuiUtils.checkChildren(children(), child -> child instanceof GuiElement && child.keyPressed(keyCode, scanCode, modifiers)) ||
               super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int keyCode) {
        return windows.stream().anyMatch(window -> window.charTyped(c, keyCode)) ||
               GuiUtils.checkChildren(children(), child -> child instanceof GuiElement && child.charTyped(c, keyCode)) ||
               super.charTyped(c, keyCode);
    }

    /**
     * @apiNote mouseXOld and mouseYOld are just guessed mappings I couldn't find any usage from a quick glance.
     */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double mouseXOld, double mouseYOld) {
        super.mouseDragged(mouseX, mouseY, button, mouseXOld, mouseYOld);
        return getFocused() != null && isDragging() && button == GLFW.GLFW_MOUSE_BUTTON_LEFT && getFocused().mouseDragged(mouseX, mouseY, button, mouseXOld, mouseYOld);
    }

    @Nullable
    @Override
    @Deprecated//Don't use directly, this is normally private in ContainerScreen
    protected Slot findSlot(double mouseX, double mouseY) {
        //We override the implementation we have in VirtualSlotContainerScreen so that we can cache getting our window
        // and have some general performance improvements given we can batch a bunch of lookups together
        boolean checkedWindow = false;
        boolean overNoButtons = false;
        GuiWindow window = null;
        for (Slot slot : menu.slots) {
            if (!slot.isActive()) {
                continue;
            }
            boolean virtual = slot instanceof IVirtualSlot;
            int xPos = slot.x;
            int yPos = slot.y;
            if (virtual) {
                //Virtual slots need special handling to allow for matching them to the window they may be attached to
                IVirtualSlot virtualSlot = (IVirtualSlot) slot;
                if (!isVirtualSlotAvailable(virtualSlot)) {
                    //If the slot is not available just skip all checks related to it
                    continue;
                }
                xPos = virtualSlot.getActualX();
                yPos = virtualSlot.getActualY();
            }
            if (super.isHovering(xPos, yPos, 16, 16, mouseX, mouseY)) {
                if (!checkedWindow) {
                    //Only lookup the window once
                    checkedWindow = true;
                    window = getWindowHovering(mouseX, mouseY);
                    overNoButtons = overNoButtons(window, mouseX, mouseY);
                }
                if (overNoButtons && slot.isActive()) {
                    if (window == null) {
                        return slot;
                    } else if (virtual && window.childrenContainsElement(element -> element instanceof GuiVirtualSlot v && v.isElementForSlot((IVirtualSlot) slot))) {
                        return slot;
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected boolean isMouseOverSlot(@NotNull Slot slot, double mouseX, double mouseY) {
        if (slot instanceof IVirtualSlot virtualSlot) {
            //Virtual slots need special handling to allow for matching them to the window they may be attached to
            if (isVirtualSlotAvailable(virtualSlot)) {
                //Start by checking if the slot is even "active/available"
                int xPos = virtualSlot.getActualX();
                int yPos = virtualSlot.getActualY();
                if (super.isHovering(xPos, yPos, 16, 16, mouseX, mouseY)) {
                    GuiWindow window = getWindowHovering(mouseX, mouseY);
                    //If we are hovering over a window, check if the virtual slot is a child of the window
                    if (window == null || window.childrenContainsElement(element -> element instanceof GuiVirtualSlot v && v.isElementForSlot(virtualSlot))) {
                        return overNoButtons(window, mouseX, mouseY);
                    }
                }
            }
            return false;
        }
        return isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY);
    }

    private boolean overNoButtons(@Nullable GuiWindow window, double mouseX, double mouseY) {
        if (window == null) {
            return children().stream().noneMatch(button -> button.isMouseOver(mouseX, mouseY));
        }
        return !window.childrenContainsElement(e -> e.isMouseOver(mouseX, mouseY));
    }

    private boolean isVirtualSlotAvailable(IVirtualSlot virtualSlot) {
        //If there is a window linked to the slot, and it no longer exists then skip checking if the slot is available
        return !(virtualSlot.getLinkedWindow() instanceof GuiWindow linkedWindow) || windows.contains(linkedWindow);
    }

    @Override
    protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
        // overridden to prevent slot interactions when a GuiElement is blocking
        return super.isHovering(x, y, width, height, mouseX, mouseY) && getWindowHovering(mouseX, mouseY) == null &&
               overNoButtons(null, mouseX, mouseY);
    }

    protected void addSlots() {
        int size = menu.slots.size();
        for (int i = 0; i < size; i++) {
            Slot slot = menu.slots.get(i);
            if (slot instanceof InventoryContainerSlot containerSlot) {
                ContainerSlotType slotType = containerSlot.getSlotType();
                DataType dataType = findDataType(containerSlot);
                //Shift the slots by one as the elements include the border of the slot
                SlotType type;
                if (dataType != null) {
                    type = SlotType.get(dataType);
                } else if (slotType == ContainerSlotType.INPUT || slotType == ContainerSlotType.OUTPUT || slotType == ContainerSlotType.EXTRA) {
                    type = SlotType.NORMAL;
                } else if (slotType == ContainerSlotType.POWER) {
                    type = SlotType.POWER;
                } else if (slotType == ContainerSlotType.NORMAL || slotType == ContainerSlotType.VALIDITY) {
                    type = SlotType.NORMAL;
                } else {//slotType == ContainerSlotType.IGNORED: don't do anything
                    continue;
                }
                GuiSlot guiSlot = new GuiSlot(type, this, slot.x - 1, slot.y - 1);
                containerSlot.addWarnings(guiSlot);
                SlotOverlay slotOverlay = containerSlot.getSlotOverlay();
                if (slotOverlay != null) {
                    guiSlot.with(slotOverlay);
                }
                if (slotType == ContainerSlotType.VALIDITY) {
                    int index = i;
                    guiSlot.validity(() -> checkValidity(index));
                }
                addRenderableWidget(guiSlot);
            } else {
                addRenderableWidget(new GuiSlot(SlotType.NORMAL, this, slot.x - 1, slot.y - 1));
            }
        }
    }

    @Nullable
    protected DataType findDataType(InventoryContainerSlot slot) {
        if (menu instanceof MekanismTileContainer<?> container && container.getTileEntity() instanceof ISideConfiguration sideConfig) {
            return sideConfig.getActiveDataType(slot.getInventorySlot());
        }
        return null;
    }

    protected ItemStack checkValidity(int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        //Ensure the GL color is white as mods adding an overlay (such as JEI for bookmarks), might have left
        // it in an unexpected state.
        MekanismRenderer.resetColor(guiGraphics);
        if (width < 8 || height < 8) {
            Mekanism.logger.warn("Gui: {}, was too small to draw the background of. Unable to draw a background for a gui smaller than 8 by 8.", getClass().getSimpleName());
            return;
        }
        GuiUtils.renderBackgroundTexture(guiGraphics, BASE_BACKGROUND, 4, 4, leftPos, topPos, imageWidth, imageHeight, 256, 256);
    }

    @Override
    @SuppressWarnings("ConstantValue")
    public Font getFont() {
        //In theory font is never null here, but we validate it in case we are called before init finishes happening
        return font == null ? minecraft.font : font;
    }

    //TODO - 1.20: Test this and everything else relating to the switch to guigraphics
    // My guess is we may be able to remove the model view stack hacks??
    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        // shift back a whole lot so we can stack more windows
        //TODO - 1.20: Validate this, used to translate the modelViewStack
        pose.translate(0, 0, -500);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        pose.popPose();
    }

    @Override
    public boolean currentlyQuickCrafting() {
        return isQuickCrafting && !quickCraftSlots.isEmpty();
    }

    @Override
    public void addWindow(GuiWindow window) {
        GuiWindow top = windows.isEmpty() ? null : windows.iterator().next();
        if (top != null) {
            top.onFocusLost();
        }
        windows.add(window);
        window.onFocused();
    }

    @Override
    public void removeWindow(GuiWindow window) {
        if (!windows.isEmpty()) {
            GuiWindow top = windows.iterator().next();
            windows.remove(window);
            if (window == top) {
                //If the window was the top window, make it lose focus
                window.onFocusLost();
                //Amd check if a new window is now in focus
                GuiWindow newTop = windows.isEmpty() ? null : windows.iterator().next();
                if (newTop == null) {
                    //If there isn't any because they have all been removed
                    // fire an "event" for any post all windows being closed
                    lastWindowRemoved();
                } else {
                    //Otherwise, mark the new window as being focused
                    newTop.onFocused();
                }
                //Update the listener to being the window that is now selected or null if none are
                setFocused(newTop);
            }
        }
    }

    protected void lastWindowRemoved() {
        //Mark that no windows are now selected
        if (menu instanceof MekanismContainer container) {
            container.setSelectedWindow(null);
        }
    }

    @Override
    public void setSelectedWindow(SelectedWindowData selectedWindow) {
        if (menu instanceof MekanismContainer container) {
            container.setSelectedWindow(selectedWindow);
        }
    }

    @Nullable
    @Override
    public GuiWindow getWindowHovering(double mouseX, double mouseY) {
        return windows.stream().filter(w -> w.isMouseOver(mouseX, mouseY)).findFirst().orElse(null);
    }

    public Collection<GuiWindow> getWindows() {
        return windows;
    }

    public LRU<GuiWindow>.LRUIterator getWindowsDescendingIterator() {
        return windows.descendingIterator();
    }
}