package mekanism.common.inventory.slot.chemical;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import mekanism.api.IContentsListener;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;
import mekanism.api.chemical.gas.IGasTank;
import mekanism.api.recipes.ItemStackToGasRecipe;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.recipe.MekanismRecipeType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@NothingNullByDefault
public class GasInventorySlot extends ChemicalInventorySlot<Gas, GasStack> {

    @Nullable
    public static IGasHandler getCapability(ItemStack stack) {
        return getCapability(stack, Capabilities.GAS_HANDLER.item());
    }

    /**
     * Gets the GasStack from ItemStack conversion, ignoring the size of the item stack.
     */
    private static GasStack getPotentialConversion(@Nullable Level world, ItemStack itemStack) {
        return getPotentialConversion(MekanismRecipeType.GAS_CONVERSION, world, itemStack, GasStack.EMPTY);
    }

    /**
     * Drains the tank depending on if this item has any contents in it AND if the supplied boolean's mode supports it
     */
    public static GasInventorySlot rotaryDrain(IGasTank gasTank, BooleanSupplier modeSupplier, @Nullable IContentsListener listener, int x, int y) {
        Objects.requireNonNull(gasTank, "Gas tank cannot be null");
        Objects.requireNonNull(modeSupplier, "Mode supplier cannot be null");
        Predicate<@NotNull ItemStack> insertPredicate = getDrainInsertPredicate(gasTank, GasInventorySlot::getCapability).and(stack -> modeSupplier.getAsBoolean());
        return new GasInventorySlot(gasTank, insertPredicate.negate(), insertPredicate, stack -> stack.getCapability(Capabilities.GAS_HANDLER.item()) != null, listener, x, y);
    }

    /**
     * Fills the tank depending on if this item has any contents in it AND if the supplied boolean's mode supports it
     */
    public static GasInventorySlot rotaryFill(IGasTank gasTank, BooleanSupplier modeSupplier, @Nullable IContentsListener listener, int x, int y) {
        Objects.requireNonNull(gasTank, "Gas tank cannot be null");
        Objects.requireNonNull(modeSupplier, "Mode supplier cannot be null");
        return new GasInventorySlot(gasTank, getFillExtractPredicate(gasTank, GasInventorySlot::getCapability),
              stack -> !modeSupplier.getAsBoolean() && fillInsertCheck(gasTank, getCapability(stack)),
              stack -> stack.getCapability(Capabilities.GAS_HANDLER.item()) != null, listener, x, y);
    }

    /**
     * Fills the tank from this item OR converts the given item to a gas
     */
    public static GasInventorySlot fillOrConvert(IGasTank gasTank, Supplier<Level> worldSupplier, @Nullable IContentsListener listener, int x, int y) {
        Objects.requireNonNull(gasTank, "Gas tank cannot be null");
        Objects.requireNonNull(worldSupplier, "World supplier cannot be null");
        Function<ItemStack, GasStack> potentialConversionSupplier = stack -> getPotentialConversion(worldSupplier.get(), stack);
        return new GasInventorySlot(gasTank, worldSupplier, getFillOrConvertExtractPredicate(gasTank, GasInventorySlot::getCapability, potentialConversionSupplier),
              getFillOrConvertInsertPredicate(gasTank, GasInventorySlot::getCapability, potentialConversionSupplier), stack -> {
            if (stack.getCapability(Capabilities.GAS_HANDLER.item()) != null) {
                //Note: we mark all gas items as valid and have a more restrictive insert check so that we allow full tanks when they are done being filled
                return true;
            }
            //Allow gas conversion of items that have a gas that is valid
            GasStack gasConversion = getPotentialConversion(worldSupplier.get(), stack);
            return !gasConversion.isEmpty() && gasTank.isValid(gasConversion);
        }, listener, x, y);
    }

    /**
     * Fills the tank from this item
     */
    public static GasInventorySlot fill(IGasTank gasTank, @Nullable IContentsListener listener, int x, int y) {
        Objects.requireNonNull(gasTank, "Gas tank cannot be null");
        return new GasInventorySlot(gasTank, getFillExtractPredicate(gasTank, GasInventorySlot::getCapability),
              stack -> fillInsertCheck(gasTank, getCapability(stack)), stack -> stack.getCapability(Capabilities.GAS_HANDLER.item()) != null, listener, x, y);
    }

    /**
     * Accepts any items that can be filled with the current contents of the gas tank, or if it is a gas tank container and the tank is currently empty
     * <p>
     * Drains the tank into this item.
     */
    public static GasInventorySlot drain(IGasTank gasTank, @Nullable IContentsListener listener, int x, int y) {
        Objects.requireNonNull(gasTank, "Gas tank cannot be null");
        Predicate<@NotNull ItemStack> insertPredicate = getDrainInsertPredicate(gasTank, GasInventorySlot::getCapability);
        return new GasInventorySlot(gasTank, insertPredicate.negate(), insertPredicate, stack -> stack.getCapability(Capabilities.GAS_HANDLER.item()) != null,
              listener, x, y);
    }

    private GasInventorySlot(IGasTank gasTank, Predicate<@NotNull ItemStack> canExtract, Predicate<@NotNull ItemStack> canInsert,
          Predicate<@NotNull ItemStack> validator, @Nullable IContentsListener listener, int x, int y) {
        this(gasTank, () -> null, canExtract, canInsert, validator, listener, x, y);
    }

    private GasInventorySlot(IGasTank gasTank, Supplier<Level> worldSupplier, Predicate<@NotNull ItemStack> canExtract, Predicate<@NotNull ItemStack> canInsert,
          Predicate<@NotNull ItemStack> validator, @Nullable IContentsListener listener, int x, int y) {
        super(gasTank, worldSupplier, canExtract, canInsert, validator, listener, x, y);
    }

    @Nullable
    @Override
    protected IChemicalHandler<Gas, GasStack> getCapability() {
        return getCapability(current);
    }

    @Nullable
    @Override
    protected ItemStackToGasRecipe getConversionRecipe(@Nullable Level world, ItemStack stack) {
        return MekanismRecipeType.GAS_CONVERSION.getInputCache().findFirstRecipe(world, stack);
    }
}