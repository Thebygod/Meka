package mekanism.common.tile.machine;

import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.NBTConstants;
import mekanism.api.Upgrade;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.math.FloatingLong;
import mekanism.common.CommonWorldTickHandler;
import mekanism.common.Mekanism;
import mekanism.common.attachments.FormulaAttachment;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.content.assemblicator.RecipeFormula;
import mekanism.common.integration.computer.ComputerException;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper;
import mekanism.common.integration.computer.annotation.ComputerMethod;
import mekanism.common.integration.computer.annotation.SyntheticComputerMethod;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.inventory.container.sync.SyncableBoolean;
import mekanism.common.inventory.container.sync.SyncableInt;
import mekanism.common.inventory.container.sync.SyncableItemStack;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.inventory.slot.FormulaicCraftingSlot;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.item.ItemCraftingFormula;
import mekanism.common.lib.inventory.HashedItem;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.ConfigInfo;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.component.config.slot.InventorySlotInfo;
import mekanism.common.tile.interfaces.IHasMode;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import mekanism.common.util.InventoryUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.UpgradeUtils;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TileEntityFormulaicAssemblicator extends TileEntityConfigurableMachine implements IHasMode {

    public static final Predicate<@NotNull ItemStack> FORMULA_SLOT_VALIDATOR = stack -> stack.getItem() instanceof ItemCraftingFormula;
    private static final NonNullList<ItemStack> EMPTY_LIST = NonNullList.create();

    private static final int BASE_TICKS_REQUIRED = 2 * SharedConstants.TICKS_PER_SECOND;

    private final CraftingContainer dummyInv = MekanismUtils.getDummyCraftingInv();

    private int ticksRequired = BASE_TICKS_REQUIRED;
    private int operatingTicks;
    private boolean usedEnergy = false;
    private boolean autoMode = false;
    private boolean isRecipe = false;
    private boolean stockControl = false;
    private boolean needsOrganize = true; //organize on load
    private boolean canTryToMove = true; //allow trying to move on load
    private final HashedItem[] stockControlMap = new HashedItem[18];

    private int pulseOperations;

    public RecipeFormula formula;
    @Nullable
    private RecipeHolder<CraftingRecipe> cachedRecipe = null;
    @SyntheticComputerMethod(getter = "getExcessRemainingItems")
    NonNullList<ItemStack> lastRemainingItems = EMPTY_LIST;

    private ItemStack lastFormulaStack = ItemStack.EMPTY;
    private ItemStack lastOutputStack = ItemStack.EMPTY;

    private MachineEnergyContainer<TileEntityFormulaicAssemblicator> energyContainer;
    private List<IInventorySlot> craftingGridSlots;
    private List<IInventorySlot> inputSlots;
    private List<IInventorySlot> outputSlots;
    @WrappingComputerMethod(wrapper = ComputerIInventorySlotWrapper.class, methodNames = "getFormulaItem", docPlaceholder = "formula slot")
    BasicInventorySlot formulaSlot;
    @WrappingComputerMethod(wrapper = ComputerIInventorySlotWrapper.class, methodNames = "getEnergyItem", docPlaceholder = "energy slot")
    EnergyInventorySlot energySlot;

    public TileEntityFormulaicAssemblicator(BlockPos pos, BlockState state) {
        super(MekanismBlocks.FORMULAIC_ASSEMBLICATOR, pos, state);
        configComponent.setupItemIOConfig(inputSlots, outputSlots, energySlot, false);
        ConfigInfo itemConfig = configComponent.getConfig(TransmissionType.ITEM);
        if (itemConfig != null) {
            //Expose formula slot via extra
            itemConfig.addSlotInfo(DataType.EXTRA, new InventorySlotInfo(true, true, formulaSlot));
            itemConfig.setDefaults();
        }
        configComponent.setupInputConfig(TransmissionType.ENERGY, energyContainer);
        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(configComponent, TransmissionType.ITEM);
    }

    @NotNull
    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        EnergyContainerHelper builder = EnergyContainerHelper.forSideWithConfig(this::getDirection, this::getConfig);
        builder.addContainer(energyContainer = MachineEnergyContainer.input(this, listener));
        return builder.build();
    }

    @NotNull
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        craftingGridSlots = new ArrayList<>();
        inputSlots = new ArrayList<>();
        outputSlots = new ArrayList<>();
        IContentsListener inputSlotChanged = () -> {
            listener.onContentsChanged();
            //If an input slot changes allow trying to move items to the crafting grid again as potentially we have something that can be moved
            // and if we have stock control enabled, allow attempting to re-organize the inventory
            needsOrganize = stockControl;
            canTryToMove = true;
        };
        IContentsListener listenAndRecheckRecipe = () -> {
            listener.onContentsChanged();
            recalculateRecipe();
        };

        InventorySlotHelper builder = InventorySlotHelper.forSideWithConfig(this::getDirection, this::getConfig);
        //If the formula slot changes we want to make sure to recheck the recipe
        builder.addSlot(formulaSlot = BasicInventorySlot.at(FORMULA_SLOT_VALIDATOR, listenAndRecheckRecipe, 6, 26, 1))
              .setSlotOverlay(SlotOverlay.FORMULA);
        for (int slotY = 0; slotY < 2; slotY++) {
            for (int slotX = 0; slotX < 9; slotX++) {
                int index = slotY * 9 + slotX;
                InputInventorySlot inputSlot = InputInventorySlot.at(stack -> {
                    //Is item valid
                    if (formula == null) {
                        return true;
                    } else if (!formula.isValidFormula()) {
                        return false;
                    } else if (stockControl) {
                        HashedItem stockItem = stockControlMap[index];
                        if (stockItem != null) {
                            return ItemHandlerHelper.canItemStacksStack(stockItem.getInternalStack(), stack);
                        }
                    }
                    return formula.isValidIngredient(level, stack);
                }, BasicInventorySlot.alwaysTrue, inputSlotChanged, 8 + slotX * 18, 98 + slotY * 18);
                inputSlots.add(builder.addSlot(inputSlot));
            }
        }
        for (int slotY = 0; slotY < 3; slotY++) {
            for (int slotX = 0; slotX < 3; slotX++) {
                //If a crafting slot changes then we want to make sure that we recheck the recipe
                IInventorySlot craftingSlot = FormulaicCraftingSlot.at(this::getAutoMode, listenAndRecheckRecipe, 26 + slotX * 18, 17 + slotY * 18);
                craftingGridSlots.add(builder.addSlot(craftingSlot));
            }
        }
        for (int slotY = 0; slotY < 3; slotY++) {
            for (int slotX = 0; slotX < 2; slotX++) {
                OutputInventorySlot outputSlot = OutputInventorySlot.at(listener, 116 + slotX * 18, 17 + slotY * 18);
                outputSlots.add(builder.addSlot(outputSlot));
            }
        }
        //Add the energy slot after adding the other slots so that it has the lowest priority in shift clicking
        builder.addSlot(energySlot = EnergyInventorySlot.fillOrConvert(energyContainer, this::getLevel, listener, 152, 76));
        return builder.build();
    }

    public BasicInventorySlot getFormulaSlot() {
        return formulaSlot;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) {
            checkFormula();
            recalculateRecipe();
            if (formula != null && stockControl) {
                //Ensure stock control is loaded before our first tick in case something inserting ticks before our first tick
                // and inserts into the wrong slots
                buildStockControlMap();
            }
        }
    }

    @Override
    protected void onUpdateServer() {
        super.onUpdateServer();
        if (CommonWorldTickHandler.flushTagAndRecipeCaches) {
            //Invalidate the cached recipe and recalculate
            cachedRecipe = null;
            recalculateRecipe();
        }
        if (formula != null && stockControl && needsOrganize) {
            buildStockControlMap();
            organizeStock();
            //Mark as no longer needing to organize after organizing it so that it rearranging things doesn't cause it to organize again
            needsOrganize = false;
        }
        energySlot.fillContainerOrConvert();
        if (getControlType() != RedstoneControl.PULSE) {
            pulseOperations = 0;
        } else if (MekanismUtils.canFunction(this)) {
            pulseOperations++;
        }
        checkFormula();
        if (autoMode && formula == null) {
            nextMode();
        }

        FloatingLong clientEnergyUsed = FloatingLong.ZERO;
        if (autoMode && formula != null && ((getControlType() == RedstoneControl.PULSE && pulseOperations > 0) || MekanismUtils.canFunction(this))) {
            boolean canOperate = true;
            if (!isRecipe) {
                canOperate = moveItemsToGrid();
            }
            if (canOperate) {
                isRecipe = true;
                if (operatingTicks >= ticksRequired) {
                    if (doSingleCraft()) {
                        operatingTicks = 0;
                        if (pulseOperations > 0) {
                            pulseOperations--;
                        }
                    }
                } else {
                    FloatingLong energyPerTick = energyContainer.getEnergyPerTick();
                    if (energyContainer.extract(energyPerTick, Action.SIMULATE, AutomationType.INTERNAL).equals(energyPerTick)) {
                        clientEnergyUsed = energyContainer.extract(energyPerTick, Action.EXECUTE, AutomationType.INTERNAL);
                        operatingTicks++;
                    }
                }
            } else {
                operatingTicks = 0;
            }
        } else {
            operatingTicks = 0;
        }
        usedEnergy = !clientEnergyUsed.isZero();
    }

    private void checkFormula() {
        ItemStack formulaStack = formulaSlot.getStack();
        Optional<FormulaAttachment> formulaAttachment = FormulaAttachment.existingFormula(formulaStack)
              .filter(FormulaAttachment::isValid);
        if (formulaAttachment.isPresent()) {
            if (formula == null || lastFormulaStack != formulaStack) {
                loadFormula(formulaAttachment.get());
            }
        } else {
            formula = null;
        }
        lastFormulaStack = formulaStack;
    }

    //Note: Assumes attachment is not invalid
    private void loadFormula(FormulaAttachment attachment) {
        RecipeFormula recipe = new RecipeFormula(level, attachment.getItems());
        if (recipe.isValidFormula()) {
            if (formula == null) {
                formula = recipe;
            } else if (!formula.isFormulaEqual(recipe)) {
                formula = recipe;
                operatingTicks = 0;
            }
        } else {
            formula = null;
            attachment.setInvalid();
        }
    }

    private void recalculateRecipe() {
        if (level != null && !isRemote()) {
            boolean wasRecipe = isRecipe;
            ItemStack previousOutput = lastOutputStack;
            NonNullList<ItemStack> previousRemaining = lastRemainingItems;
            if (hasValidFormula()) {
                isRecipe = formula.matches(level, craftingGridSlots);
                if (isRecipe) {
                    lastOutputStack = formula.assemble(level.registryAccess());
                    lastRemainingItems = formula.getRemainingItems();
                } else {
                    lastOutputStack = ItemStack.EMPTY;
                }
            } else {
                //Should always be 9 for the size
                for (int i = 0; i < craftingGridSlots.size(); i++) {
                    dummyInv.setItem(i, craftingGridSlots.get(i).getStack().copyWithCount(1));
                }
                lastRemainingItems = EMPTY_LIST;
                if (cachedRecipe == null || !cachedRecipe.value().matches(dummyInv, level)) {
                    cachedRecipe = MekanismRecipeType.getRecipeFor(RecipeType.CRAFTING, dummyInv, level).orElse(null);
                }
                if (cachedRecipe == null) {
                    lastOutputStack = ItemStack.EMPTY;
                } else {
                    lastOutputStack = cachedRecipe.value().assemble(dummyInv, level.registryAccess());
                    lastRemainingItems = cachedRecipe.value().getRemainingItems(dummyInv);
                }
                isRecipe = !lastOutputStack.isEmpty();
            }
            boolean recipeChanged = false;
            if (isRecipe != wasRecipe || !ItemStack.matches(lastOutputStack, previousOutput) || lastRemainingItems.size() != previousRemaining.size()) {
                recipeChanged = true;
            } else {
                for (int i = 0; i < lastRemainingItems.size(); i++) {
                    if (!ItemStack.matches(lastRemainingItems.get(i), previousRemaining.get(i))) {
                        recipeChanged = true;
                        break;
                    }
                }
            }
            if (recipeChanged) {
                needsOrganize = true;
                canTryToMove = true;
            }
        }
    }

    private boolean doSingleCraft() {
        ItemStack output = lastOutputStack;
        if (!output.isEmpty() && tryMoveToOutput(output, Action.SIMULATE) &&
            (lastRemainingItems.isEmpty() || lastRemainingItems.stream().allMatch(it -> it.isEmpty() || tryMoveToOutput(it, Action.SIMULATE)))) {
            tryMoveToOutput(output, Action.EXECUTE);
            //TODO: Fix this as I believe if things overlap there is a chance it won't work properly.
            // For example if there are multiple stacks of dirt, or even just different item types, in remaining and we have room for one stack,
            // but given we only check one stack at a time...)
            // Basically simulating fitting the last remaining items doesn't do enough validation about intermediary state
            for (ItemStack remainingItem : lastRemainingItems) {
                if (!remainingItem.isEmpty()) {
                    //TODO: Check if it matters that we are not actually updating the list of remaining items?
                    // The better solution would be to not allow continuing until we moved output AND all remaining items
                    // instead of trying to move all at once??
                    tryMoveToOutput(remainingItem, Action.EXECUTE);
                }
            }

            for (IInventorySlot craftingSlot : craftingGridSlots) {
                if (!craftingSlot.isEmpty()) {
                    MekanismUtils.logMismatchedStackSize(craftingSlot.shrinkStack(1, Action.EXECUTE), 1);
                }
            }
            if (formula != null) {
                moveItemsToGrid();
            }
            return true;
        }
        return false;
    }

    public boolean craftSingle() {
        boolean canOperate = true;
        if (formula != null && !formula.matches(getLevel(), craftingGridSlots)) {
            canOperate = moveItemsToGrid();
        }
        return canOperate && doSingleCraft();
    }

    private boolean moveItemsToGrid() {
        if (!canTryToMove) {
            return false;
        }
        boolean ret = true;
        for (int i = 0; i < craftingGridSlots.size(); i++) {
            IInventorySlot recipeSlot = craftingGridSlots.get(i);
            ItemStack recipeStack = recipeSlot.getStack();
            if (formula.isIngredientInPos(level, recipeStack, i)) {
                continue;
            }
            if (recipeStack.isEmpty()) {
                Set<HashedItem> checkedTypes = null;
                for (int j = inputSlots.size() - 1; j >= 0; j--) {
                    //The stack stored in the stock inventory
                    IInventorySlot stockSlot = inputSlots.get(j);
                    if (!stockSlot.isEmpty()) {
                        ItemStack stockStack = stockSlot.getStack();
                        //Note: As we don't mutate it (except potentially when we found it as a match, at which point we don't need it anymore),
                        // we can just use a raw view rather than having to copy the stack
                        HashedItem stockStackType = HashedItem.raw(stockStack);
                        //If we already checked this stack type for being valid in the recipe for this position, we can skip checking it again
                        if (checkedTypes == null || checkedTypes.add(stockStackType)) {
                            if (formula.isIngredientInPos(level, stockStack, i)) {
                                recipeSlot.setStack(stockStack.copyWithCount(1));
                                MekanismUtils.logMismatchedStackSize(stockSlot.shrinkStack(1, Action.EXECUTE), 1);
                                break;
                            } else if (checkedTypes == null) {
                                checkedTypes = new HashSet<>();
                                //Note: If the types set was not null, then we will have added it above when checking if we already checked the type
                                checkedTypes.add(stockStackType);
                            }
                        }
                    }
                }
                if (recipeSlot.isEmpty()) {
                    //We didn't find a stack to replace it with, that means we won't be able to operate on our recipe
                    ret = false;
                }
            } else {
                //Update recipeStack as well, so we can check if it is empty without having to get it again
                recipeSlot.setStack(recipeStack = tryMoveToInput(recipeStack));
                if (!recipeStack.isEmpty()) {
                    ret = false;
                }
            }
        }
        if (!ret) {
            //If we failed to move items, then we know none of the currently stored items are valid for the recipe,
            // so we can skip trying to move them until something changes
            canTryToMove = false;
        }
        return ret;
    }

    public void craftAll() {
        //TODO: Can we somehow optimize this, maybe by moving multiple items at once
        while (craftSingle()) {
        }
    }

    public void fillGrid() {
        if (formula != null) {
            moveItemsToGrid();
        }
    }

    public void emptyGrid() {
        if (formula == null) {
            moveItemsToInput(true);
        }
    }

    private void moveItemsToInput(boolean forcePush) {
        for (int i = 0; i < craftingGridSlots.size(); i++) {
            IInventorySlot recipeSlot = craftingGridSlots.get(i);
            ItemStack recipeStack = recipeSlot.getStack();
            if (!recipeStack.isEmpty() && (forcePush || (formula != null && !formula.isIngredientInPos(getLevel(), recipeStack, i)))) {
                recipeSlot.setStack(tryMoveToInput(recipeStack));
            }
        }
    }

    @Override
    public void nextMode() {
        if (autoMode) {
            operatingTicks = 0;
            autoMode = false;
            markForSave();
        } else if (formula != null) {
            moveItemsToInput(false);
            autoMode = true;
            markForSave();
        }
    }

    @Override
    public void previousMode() {
        //We only have two modes just flip it
        nextMode();
    }

    @ComputerMethod
    public boolean hasRecipe() {
        return isRecipe;
    }

    @ComputerMethod(nameOverride = "getRecipeProgress")
    public int getOperatingTicks() {
        return operatingTicks;
    }

    @ComputerMethod
    public int getTicksRequired() {
        return ticksRequired;
    }

    public boolean getStockControl() {
        return stockControl;
    }

    public boolean getAutoMode() {
        return autoMode;
    }

    public void toggleStockControl() {
        if (!isRemote() && formula != null) {
            stockControl = !stockControl;
            if (stockControl) {
                organizeStock();
            }
            //We either just organized, so can mark it as not actually needing organize in case things changed,
            // or we don't want to organize so if we had a queued organization we can just remove it
            needsOrganize = false;
        }
    }

    private void organizeStock() {
        if (formula == null) {
            return;
        }
        // build map of what items we have to organize
        // Note: We keep track of the order so that it is more consistent
        Object2IntMap<HashedItem> storedMap = new Object2IntLinkedOpenHashMap<>();
        for (IInventorySlot inputSlot : inputSlots) {
            if (!inputSlot.isEmpty()) {
                ItemStack stack = inputSlot.getStack();
                HashedItem hashed = HashedItem.create(stack);
                storedMap.mergeInt(hashed, stack.getCount(), Integer::sum);
            }
        }
        // place items into respective controlled slots
        IntSet unused = new IntArraySet(stockControlMap.length);
        for (int i = 0; i < inputSlots.size(); i++) {
            HashedItem hashedItem = stockControlMap[i];
            if (hashedItem == null) {
                unused.add(i);
            } else {
                IInventorySlot slot = inputSlots.get(i);
                int stored = storedMap.getInt(hashedItem);
                if (stored > 0) {
                    int count = Math.min(hashedItem.getMaxStackSize(), stored);
                    if (count == stored) {
                        storedMap.removeInt(hashedItem);
                    } else {
                        storedMap.put(hashedItem, stored - count);
                    }
                    setSlotIfChanged(slot, hashedItem, count);
                } else if (!slot.isEmpty()) {
                    //If we don't have the item stored anymore (already filled all previous slots with it),
                    // then we need to empty the slot as the items in it has been moved to a more "optimal" slot
                    //Note: We only set them to empty if they are not already empty to avoid onContentsChanged being called
                    // Technically our default implementation doesn't fire onContentsChanged if the stack was already empty
                    // but this is not an API contract
                    slot.setEmpty();
                }
            }
        }
        // if we still have items, first try to add remaining items to known unused (non-controlled) slots
        boolean empty = storedMap.isEmpty();
        for (int i : unused) {
            IInventorySlot slot = inputSlots.get(i);
            if (empty) {
                //If we don't have any more items to sort, clear all the other slots that we haven't set something in
                //Note: We only set them to empty if they are not already empty to avoid onContentsChanged being called
                // Technically our default implementation doesn't fire onContentsChanged if the stack was already empty
                // but this is not an API contract
                if (!slot.isEmpty()) {
                    slot.setEmpty();
                }
            } else {
                empty = setSlotIfChanged(storedMap, slot);
            }
        }
        if (empty) {
            //If we are empty exit
            return;
        }
        // if we still have items, just add them to any slots that are still empty
        for (IInventorySlot inputSlot : inputSlots) {
            if (inputSlot.isEmpty()) {
                if (setSlotIfChanged(storedMap, inputSlot)) {
                    //Exit all items accounted for
                    return;
                }
            }
        }
        if (!storedMap.isEmpty()) {
            Mekanism.logger.error("Critical error: Formulaic Assemblicator had items left over after organizing stock. Impossible!");
        }
    }

    private boolean setSlotIfChanged(Object2IntMap<HashedItem> storedMap, IInventorySlot inputSlot) {
        boolean empty = false;
        ObjectIterator<Entry<HashedItem>> iterator = storedMap.object2IntEntrySet().iterator();
        Object2IntMap.Entry<HashedItem> next = iterator.next();
        HashedItem item = next.getKey();
        int stored = next.getIntValue();
        int count = Math.min(item.getMaxStackSize(), stored);
        if (count == stored) {
            iterator.remove();
            empty = storedMap.isEmpty();
        } else {
            next.setValue(stored - count);
        }
        setSlotIfChanged(inputSlot, item, count);
        return empty;
    }

    private static void setSlotIfChanged(IInventorySlot slot, HashedItem item, int count) {
        ItemStack stack = item.createStack(count);
        if (!ItemStack.matches(slot.getStack(), stack)) {
            slot.setStack(stack);
        }
    }

    private void buildStockControlMap() {
        if (formula == null) {
            return;
        }
        for (int i = 0; i < 9; i++) {
            int j = i * 2;
            ItemStack stack = formula.getInputStack(i);
            if (stack.isEmpty()) {
                stockControlMap[j] = null;
                stockControlMap[j + 1] = null;
            } else {
                HashedItem hashedItem = HashedItem.create(stack);
                stockControlMap[j] = hashedItem;
                stockControlMap[j + 1] = hashedItem;
            }
        }
    }

    private ItemStack tryMoveToInput(ItemStack stack) {
        return InventoryUtils.insertItem(inputSlots, stack, Action.EXECUTE, AutomationType.INTERNAL);
    }

    private boolean tryMoveToOutput(ItemStack stack, Action action) {
        //Try to insert the item (simulating as needed), and overwrite our local reference to point to the remainder
        // We can then continue on to the next slot if we did not fit it all and try to insert it.
        // The logic is relatively simple due to only having one stack we are trying to insert, so we don't have to worry
        // about the fact the slot doesn't actually get updated if we simulated, and then is invalid for the next simulation
        stack = InventoryUtils.insertItem(outputSlots, stack, action, AutomationType.INTERNAL);
        return stack.isEmpty();
    }

    public void encodeFormula() {
        if (formulaSlot.isEmpty()) {
            return;
        }
        ItemStack stack = formulaSlot.getStack().copy();
        Optional<FormulaAttachment> formulaAttachment = FormulaAttachment.formula(stack)
              .filter(FormulaAttachment::isEmpty);
        if (formulaAttachment.isPresent()) {
            RecipeFormula formula = new RecipeFormula(level, craftingGridSlots);
            if (formula.isValidFormula()) {
                formulaAttachment.get().setItems(formula.input);
                formulaSlot.setStack(stack);
            }
        }
    }

    @Override
    public void load(@NotNull CompoundTag nbt) {
        super.load(nbt);
        autoMode = nbt.getBoolean(NBTConstants.AUTO);
        operatingTicks = nbt.getInt(NBTConstants.PROGRESS);
        pulseOperations = nbt.getInt(NBTConstants.PULSE);
        stockControl = nbt.getBoolean(NBTConstants.STOCK_CONTROL);
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag nbtTags) {
        super.saveAdditional(nbtTags);
        nbtTags.putBoolean(NBTConstants.AUTO, autoMode);
        nbtTags.putInt(NBTConstants.PROGRESS, operatingTicks);
        nbtTags.putInt(NBTConstants.PULSE, pulseOperations);
        nbtTags.putBoolean(NBTConstants.STOCK_CONTROL, stockControl);
    }

    @Override
    public boolean supportsMode(RedstoneControl mode) {
        return true;
    }

    @Override
    public void recalculateUpgrades(Upgrade upgrade) {
        super.recalculateUpgrades(upgrade);
        if (upgrade == Upgrade.SPEED) {
            ticksRequired = MekanismUtils.getTicks(this, BASE_TICKS_REQUIRED);
        }
    }

    @NotNull
    @Override
    public List<Component> getInfo(@NotNull Upgrade upgrade) {
        return UpgradeUtils.getMultScaledInfo(this, upgrade);
    }

    public MachineEnergyContainer<TileEntityFormulaicAssemblicator> getEnergyContainer() {
        return energyContainer;
    }

    public boolean usedEnergy() {
        return usedEnergy;
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(SyncableBoolean.create(this::getAutoMode, value -> autoMode = value));
        container.track(SyncableInt.create(this::getOperatingTicks, value -> operatingTicks = value));
        container.track(SyncableInt.create(this::getTicksRequired, value -> ticksRequired = value));
        container.track(SyncableBoolean.create(this::hasRecipe, value -> isRecipe = value));
        container.track(SyncableBoolean.create(this::getStockControl, value -> stockControl = value));
        container.track(SyncableBoolean.create(this::usedEnergy, value -> usedEnergy = value));
        container.track(SyncableBoolean.create(() -> formula != null, hasFormula -> {
            if (hasFormula) {
                if (formula == null && isRemote()) {
                    //If we are on the client (which we should be when setting anyway) and we don't have a formula yet
                    // but should, then create an empty formula
                    formula = new RecipeFormula();
                }
            } else {
                formula = null;
            }
        }));
        for (int i = 0; i < 9; i++) {
            int index = i;
            container.track(SyncableItemStack.create(() -> formula == null ? ItemStack.EMPTY : formula.input.get(index), stack -> {
                if (!stack.isEmpty() && formula == null && isRemote()) {
                    //If we are on the client (which we should be when setting anyway) and we don't have a formula yet
                    // but should, then create an empty formula. Also make sure it isn't just us trying to clear the formula slot
                    formula = new RecipeFormula();
                }
                if (formula != null) {
                    formula.setStack(getLevel(), index, stack);
                }
            }));
        }
    }

    @ComputerMethod
    public boolean hasValidFormula() {
        return formula != null && formula.isValidFormula();
    }

    //Methods relating to IComputerTile
    @ComputerMethod
    ItemStack getCraftingInputSlot(int slot) throws ComputerException {
        if (slot < 0 || slot >= craftingGridSlots.size()) {
            throw new ComputerException("Crafting Input Slot '%d' is out of bounds, must be between 0 and %d.", slot, craftingGridSlots.size());
        }
        return craftingGridSlots.get(slot).getStack();
    }

    @ComputerMethod
    int getCraftingOutputSlots() {
        return outputSlots.size();
    }

    @ComputerMethod
    ItemStack getCraftingOutputSlot(int slot) throws ComputerException {
        int size = getCraftingOutputSlots();
        if (slot < 0 || slot >= size) {
            throw new ComputerException("Crafting Output Slot '%d' is out of bounds, must be between 0 and %d.", slot, size);
        }
        return outputSlots.get(slot).getStack();
    }

    @ComputerMethod(nameOverride = "getSlots")
    int computerGetSlots() {
        return inputSlots.size();
    }

    @ComputerMethod
    ItemStack getItemInSlot(int slot) throws ComputerException {
        int size = computerGetSlots();
        if (slot < 0 || slot >= size) {
            throw new ComputerException("Slot '%d' is out of bounds, must be between 0 and %d.", slot, size);
        }
        return inputSlots.get(slot).getStack();
    }

    @ComputerMethod(nameOverride = "encodeFormula", requiresPublicSecurity = true, methodDescription = "Requires an unencoded formula in the formula slot and a valid recipe")
    void computerEncodeFormula() throws ComputerException {
        validateSecurityIsPublic();
        Optional<FormulaAttachment> formulaAttachment = FormulaAttachment.formula(formulaSlot.getStack());
        if (formulaAttachment.isEmpty()) {
            throw new ComputerException("No formula found.");
        } else if (hasValidFormula() || formulaAttachment.get().hasItems()) {
            throw new ComputerException("Formula has already been encoded.");
        } else if (!hasRecipe()) {
            throw new ComputerException("Encoding formulas require that there is a valid recipe to actually encode.");
        }
        encodeFormula();
    }

    @ComputerMethod(nameOverride = "emptyGrid", requiresPublicSecurity = true, methodDescription = "Requires auto mode to be disabled")
    void computerEmptyGrid() throws ComputerException {
        validateSecurityIsPublic();
        if (autoMode) {
            throw new ComputerException("Emptying the grid requires Auto-Mode to be disabled.");
        }
        emptyGrid();
    }

    @ComputerMethod(nameOverride = "fillGrid", requiresPublicSecurity = true, methodDescription = "Requires auto mode to be disabled")
    void computerFillGrid() throws ComputerException {
        validateSecurityIsPublic();
        if (autoMode) {
            throw new ComputerException("Filling the grid requires Auto-Mode to be disabled.");
        }
        fillGrid();
    }

    private void validateCanCraft() throws ComputerException {
        validateSecurityIsPublic();
        if (!hasRecipe()) {
            throw new ComputerException("Unable to perform craft as there is currently no matching recipe in the grid.");
        } else if (autoMode) {
            throw new ComputerException("Unable to perform craft as Auto-Mode is enabled.");
        }
    }

    @ComputerMethod(requiresPublicSecurity = true, methodDescription = "Requires recipe and auto mode to be disabled")
    void craftSingleItem() throws ComputerException {
        validateCanCraft();
        craftSingle();
    }

    @ComputerMethod(requiresPublicSecurity = true, methodDescription = "Requires recipe and auto mode to be disabled")
    void craftAvailableItems() throws ComputerException {
        validateCanCraft();
        craftAll();
    }

    private void validateHasValidFormula(String operation) throws ComputerException {
        validateSecurityIsPublic();
        if (!hasValidFormula()) {
            throw new ComputerException("%s requires a valid formula.", operation);
        }
    }

    @ComputerMethod(nameOverride = "getStockControl", requiresPublicSecurity = true, methodDescription = "Requires valid encoded formula")
    boolean computerGetStockControl() throws ComputerException {
        validateHasValidFormula("Stock Control");
        return getStockControl();
    }

    @ComputerMethod(requiresPublicSecurity = true, methodDescription = "Requires valid encoded formula")
    void setStockControl(boolean mode) throws ComputerException {
        validateHasValidFormula("Stock Control");
        if (stockControl != mode) {
            toggleStockControl();
        }
    }

    @ComputerMethod(nameOverride = "getAutoMode", requiresPublicSecurity = true, methodDescription = "Requires valid encoded formula")
    boolean computerGetAutoMode() throws ComputerException {
        validateHasValidFormula("Auto-Mode");
        return getAutoMode();
    }

    @ComputerMethod(requiresPublicSecurity = true, methodDescription = "Requires valid encoded formula")
    void setAutoMode(boolean mode) throws ComputerException {
        validateHasValidFormula("Auto-Mode");
        if (autoMode != mode) {
            nextMode();
        }
    }
    //End methods IComputerTile
}
