package mekanism.common.capabilities.energy;

import java.util.Objects;
import java.util.function.LongSupplier;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.math.FloatingLong;
import mekanism.api.math.FloatingLongSupplier;
import mekanism.api.math.Unsigned;
import mekanism.common.tier.EnergyCubeTier;
import org.jetbrains.annotations.Nullable;

@NothingNullByDefault
public class EnergyCubeEnergyContainer extends BasicEnergyContainer {

    public static EnergyCubeEnergyContainer create(EnergyCubeTier tier, @Nullable IContentsListener listener) {
        Objects.requireNonNull(tier, "Energy cube tier cannot be null");
        return new EnergyCubeEnergyContainer(tier, listener);
    }

    private final boolean isCreative;
    private final LongSupplier rate;

    private EnergyCubeEnergyContainer(EnergyCubeTier tier, @Nullable IContentsListener listener) {
        super(tier.getMaxEnergy(), alwaysTrue, alwaysTrue, listener);
        isCreative = tier == EnergyCubeTier.CREATIVE;
        rate = tier::getOutput;
    }

    @Override
    protected @Unsigned long getInsertRate(@Nullable AutomationType automationType) {
        //Only limit the internal rate to change the speed at which this can be filled from an item
        return automationType == AutomationType.INTERNAL ? rate.get() : super.getInsertRate(automationType);
    }

    @Override
    protected @Unsigned long getExtractRate(@Nullable AutomationType automationType) {
        //Only limit the internal rate to change the speed at which this can be filled from an item
        return automationType == AutomationType.INTERNAL ? rate.get() : super.getExtractRate(automationType);
    }

    @Override
    public @Unsigned long insert(@Unsigned long amount, Action action, AutomationType automationType) {
        //Note: Unlike other creative items, the creative energy cube does not allow changing it to always full
        return super.insert(amount, action.combine(!isCreative), automationType);
    }

    @Override
    public @Unsigned long extract(@Unsigned long amount, Action action, AutomationType automationType) {
        return super.extract(amount, action.combine(!isCreative), automationType);
    }
}