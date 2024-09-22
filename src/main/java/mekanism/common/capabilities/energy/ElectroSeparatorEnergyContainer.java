package mekanism.common.capabilities.energy;

import java.util.function.Predicate;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.Upgrade;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.functions.LongObjectToLongFunction;
import mekanism.common.block.attribute.AttributeEnergy;
import mekanism.common.tile.machine.TileEntityElectrolyticSeparator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@NothingNullByDefault
public class ElectroSeparatorEnergyContainer extends MachineEnergyContainer<TileEntityElectrolyticSeparator> {

    public static ElectroSeparatorEnergyContainer input(TileEntityElectrolyticSeparator tile, LongObjectToLongFunction<TileEntityElectrolyticSeparator> baseEnergyCalculator,
          @Nullable IContentsListener listener) {
        AttributeEnergy electricBlock = validateBlock(tile);
        return new ElectroSeparatorEnergyContainer(electricBlock.getStorage(), electricBlock.getUsage(), notExternal, alwaysTrue, tile, baseEnergyCalculator, listener);
    }

    private final LongObjectToLongFunction<TileEntityElectrolyticSeparator> baseEnergyCalculator;

    protected ElectroSeparatorEnergyContainer(long maxEnergy, long energyPerTick, Predicate<@NotNull AutomationType> canExtract,
          Predicate<@NotNull AutomationType> canInsert, TileEntityElectrolyticSeparator tile, LongObjectToLongFunction<TileEntityElectrolyticSeparator> baseEnergyCalculator, @Nullable IContentsListener listener) {
        super(maxEnergy, energyPerTick, canExtract, canInsert, tile, listener);
        this.baseEnergyCalculator = baseEnergyCalculator;
    }

    @Override
    public long getBaseEnergyPerTick() {
        long base = baseEnergyCalculator.applyAsLong(super.getBaseEnergyPerTick(), tile);
        //todo better check
        if (tile.isMakingHydrogen()) {
            base = (long) (base * Math.pow(2, tile.getComponent().getUpgrades(Upgrade.SPEED)));
        }
        return base;
    }

    @Override
    public void updateEnergyPerTick() {
        if (tile.isMakingHydrogen()) {
            //Energy upgrades only increase storage
            this.currentEnergyPerTick = getBaseEnergyPerTick();
        } else {
            super.updateEnergyPerTick();
        }
    }
}
