package mekanism.common.inventory.slot;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.NBTConstants;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.merged.MergedTank;
import mekanism.common.inventory.slot.chemical.ChemicalInventorySlot;
import mekanism.common.inventory.slot.chemical.GasInventorySlot;
import mekanism.common.inventory.slot.chemical.InfusionInventorySlot;
import mekanism.common.inventory.slot.chemical.MergedChemicalInventorySlot;
import mekanism.common.inventory.slot.chemical.PigmentInventorySlot;
import mekanism.common.inventory.slot.chemical.SlurryInventorySlot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HybridInventorySlot extends MergedChemicalInventorySlot<MergedTank> implements IFluidHandlerSlot {

    private static boolean hasCapability(@NotNull ItemStack stack) {
        return Capabilities.FLUID.hasCapability(stack) || Capabilities.GAS.hasCapability(stack) || Capabilities.INFUSION.hasCapability(stack) ||
               Capabilities.PIGMENT.hasCapability(stack) || Capabilities.SLURRY.hasCapability(stack);
    }

    public static HybridInventorySlot inputOrDrain(MergedTank mergedTank, @Nullable IContentsListener listener, int x, int y) {
        Objects.requireNonNull(mergedTank, "Merged tank cannot be null");
        Predicate<@NotNull ItemStack> fluidInsertPredicate = FluidInventorySlot.getInputPredicate(mergedTank.getFluidTank());
        Predicate<@NotNull ItemStack> gasInsertPredicate = ChemicalInventorySlot.getDrainInsertPredicate(mergedTank.getGasTank(), GasInventorySlot::getCapability);
        Predicate<@NotNull ItemStack> infusionInsertPredicate = ChemicalInventorySlot.getDrainInsertPredicate(mergedTank.getInfusionTank(), InfusionInventorySlot::getCapability);
        Predicate<@NotNull ItemStack> pigmentInsertPredicate = ChemicalInventorySlot.getDrainInsertPredicate(mergedTank.getPigmentTank(), PigmentInventorySlot::getCapability);
        Predicate<@NotNull ItemStack> slurryInsertPredicate = ChemicalInventorySlot.getDrainInsertPredicate(mergedTank.getSlurryTank(), SlurryInventorySlot::getCapability);
        BiPredicate<@NotNull ItemStack, @NotNull AutomationType> insertPredicate = (stack, automationType) -> switch (mergedTank.getCurrentType()) {
            case FLUID -> fluidInsertPredicate.test(stack);
            case GAS -> gasInsertPredicate.test(stack);
            case INFUSION -> infusionInsertPredicate.test(stack);
            case PIGMENT -> pigmentInsertPredicate.test(stack);
            case SLURRY -> slurryInsertPredicate.test(stack);
            //Tank is empty, check if any insert predicate is valid
            case EMPTY -> fluidInsertPredicate.test(stack) || gasInsertPredicate.test(stack) || infusionInsertPredicate.test(stack) ||
                          pigmentInsertPredicate.test(stack) || slurryInsertPredicate.test(stack);
        };
        //Extract predicate, always allow the player to manually extract or if the insert predicate no longer matches allow for it to be extracted
        return new HybridInventorySlot(mergedTank, (stack, automationType) -> automationType == AutomationType.MANUAL || !insertPredicate.test(stack, automationType),
              insertPredicate, HybridInventorySlot::hasCapability, listener, x, y);
    }

    public static HybridInventorySlot outputOrFill(MergedTank mergedTank, @Nullable IContentsListener listener, int x, int y) {
        Objects.requireNonNull(mergedTank, "Merged tank cannot be null");
        Predicate<@NotNull ItemStack> gasExtractPredicate = ChemicalInventorySlot.getFillExtractPredicate(mergedTank.getGasTank(), GasInventorySlot::getCapability);
        Predicate<@NotNull ItemStack> infusionExtractPredicate = ChemicalInventorySlot.getFillExtractPredicate(mergedTank.getInfusionTank(), InfusionInventorySlot::getCapability);
        Predicate<@NotNull ItemStack> pigmentExtractPredicate = ChemicalInventorySlot.getFillExtractPredicate(mergedTank.getPigmentTank(), PigmentInventorySlot::getCapability);
        Predicate<@NotNull ItemStack> slurryExtractPredicate = ChemicalInventorySlot.getFillExtractPredicate(mergedTank.getSlurryTank(), SlurryInventorySlot::getCapability);
        Predicate<@NotNull ItemStack> gasInsertPredicate = stack -> ChemicalInventorySlot.fillInsertCheck(mergedTank.getGasTank(), GasInventorySlot.getCapability(stack));
        Predicate<@NotNull ItemStack> infusionInsertPredicate = stack -> ChemicalInventorySlot.fillInsertCheck(mergedTank.getInfusionTank(), InfusionInventorySlot.getCapability(stack));
        Predicate<@NotNull ItemStack> pigmentInsertPredicate = stack -> ChemicalInventorySlot.fillInsertCheck(mergedTank.getPigmentTank(), PigmentInventorySlot.getCapability(stack));
        Predicate<@NotNull ItemStack> slurryInsertPredicate = stack -> ChemicalInventorySlot.fillInsertCheck(mergedTank.getSlurryTank(), SlurryInventorySlot.getCapability(stack));
        return new HybridInventorySlot(mergedTank, (stack, automationType) -> {
            if (automationType == AutomationType.MANUAL) {
                //Always allow the player to manually extract
                return true;
            }
            return switch (mergedTank.getCurrentType()) {
                //Always allow extracting from a "fluid output" slot
                case FLUID -> true;
                case GAS -> gasExtractPredicate.test(stack);
                case INFUSION -> infusionExtractPredicate.test(stack);
                case PIGMENT -> pigmentExtractPredicate.test(stack);
                case SLURRY -> slurryExtractPredicate.test(stack);
                //Tank is empty, check all our extraction predicates
                case EMPTY -> gasExtractPredicate.test(stack) && infusionExtractPredicate.test(stack) && pigmentExtractPredicate.test(stack) &&
                              slurryExtractPredicate.test(stack);
            };
        }, (stack, automationType) -> switch (mergedTank.getCurrentType()) {
            //Only allow inserting internally for "fluid output" slots
            case FLUID -> automationType == AutomationType.INTERNAL;
            case GAS -> gasInsertPredicate.test(stack);
            case INFUSION -> infusionInsertPredicate.test(stack);
            case PIGMENT -> pigmentInsertPredicate.test(stack);
            case SLURRY -> slurryInsertPredicate.test(stack);
            case EMPTY -> {
                //Tank is empty, if the item is a fluid handler, and it is an internal check allow it
                if (automationType == AutomationType.INTERNAL && Capabilities.FLUID.hasCapability(stack)) {
                    yield true;
                }
                //otherwise, only allow it if one of the chemical insert predicates matches
                yield gasInsertPredicate.test(stack) || infusionInsertPredicate.test(stack) || pigmentInsertPredicate.test(stack) || slurryInsertPredicate.test(stack);
            }
        }, HybridInventorySlot::hasCapability, listener, x, y);
    }

    // used by IFluidHandlerSlot
    private boolean isDraining;
    private boolean isFilling;

    private HybridInventorySlot(MergedTank mergedTank, BiPredicate<@NotNull ItemStack, @NotNull AutomationType> canExtract,
          BiPredicate<@NotNull ItemStack, @NotNull AutomationType> canInsert, Predicate<@NotNull ItemStack> validator, @Nullable IContentsListener listener, int x, int y) {
        super(mergedTank, canExtract, canInsert, validator, listener, x, y);
    }

    @Override
    public IExtendedFluidTank getFluidTank() {
        return mergedTank.getFluidTank();
    }

    @Override
    public boolean isDraining() {
        return isDraining;
    }

    @Override
    public boolean isFilling() {
        return isFilling;
    }

    @Override
    public void setDraining(boolean draining) {
        isDraining = draining;
    }

    @Override
    public void setFilling(boolean filling) {
        isFilling = filling;
    }

    @NotNull
    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = super.serializeNBT();
        if (isDraining) {
            nbt.putBoolean(NBTConstants.DRAINING, true);
        }
        if (isFilling) {
            nbt.putBoolean(NBTConstants.FILLING, true);
        }
        return nbt;
    }

    @Override
    public void deserializeNBT(@NotNull CompoundTag nbt) {
        //Grab the booleans regardless if they are present as if they aren't that means they are false
        isDraining = nbt.getBoolean(NBTConstants.DRAINING);
        isFilling = nbt.getBoolean(NBTConstants.FILLING);
        super.deserializeNBT(nbt);
    }
}
