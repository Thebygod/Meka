package mekanism.common.capabilities.chemical.item;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.chemical.ChemicalTankBuilder;
import mekanism.api.chemical.infuse.IInfusionTank;
import mekanism.api.chemical.infuse.InfuseType;
import mekanism.common.capabilities.chemical.variable.RateLimitChemicalTank.RateLimitInfusionTank;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

@NothingNullByDefault
public class RateLimitInfusionHandler extends ItemStackMekanismInfusionHandler {

    public static RateLimitInfusionHandler create(ItemStack stack, LongSupplier rate, LongSupplier capacity) {
        return create(stack, rate, capacity, ChemicalTankBuilder.INFUSION.alwaysTrueBi, ChemicalTankBuilder.INFUSION.alwaysTrueBi, ChemicalTankBuilder.INFUSION.alwaysTrue);
    }

    public static RateLimitInfusionHandler create(ItemStack stack, LongSupplier rate, LongSupplier capacity, BiPredicate<@NotNull InfuseType, @NotNull AutomationType> canExtract,
          BiPredicate<@NotNull InfuseType, @NotNull AutomationType> canInsert, Predicate<@NotNull InfuseType> isValid) {
        Objects.requireNonNull(rate, "Rate supplier cannot be null");
        Objects.requireNonNull(capacity, "Capacity supplier cannot be null");
        Objects.requireNonNull(canExtract, "Extraction validity check cannot be null");
        Objects.requireNonNull(canInsert, "Insertion validity check cannot be null");
        Objects.requireNonNull(isValid, "Infuse type validity check cannot be null");
        return new RateLimitInfusionHandler(stack, listener -> new RateLimitInfusionTank(rate, capacity, canExtract, canInsert, isValid, listener));
    }

    private RateLimitInfusionHandler(ItemStack stack, Function<IContentsListener, IInfusionTank> tankProvider) {
        super(stack, tankProvider);
    }
}