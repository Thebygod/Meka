package mekanism.common.upgrade;

import java.util.List;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.inventory.slot.chemical.ChemicalInventorySlot;
import mekanism.common.tile.component.ITileComponent;
import mekanism.common.tile.interfaces.IRedstoneControl.RedstoneControl;
import net.minecraft.core.HolderLookup;

public class MetallurgicInfuserUpgradeData extends MachineUpgradeData {

    public final IChemicalTank stored;
    public final ChemicalInventorySlot infusionSlot;

    //Metallurgic Infuser Constructor
    public MetallurgicInfuserUpgradeData(HolderLookup.Provider provider, boolean redstone, RedstoneControl controlType, IEnergyContainer energyContainer,
          int operatingTicks, IChemicalTank stored, ChemicalInventorySlot infusionSlot, EnergyInventorySlot energySlot, InputInventorySlot inputSlot,
          OutputInventorySlot outputSlot, List<ITileComponent> components) {
        super(provider, redstone, controlType, energyContainer, operatingTicks, energySlot, inputSlot, outputSlot, components);
        this.stored = stored;
        this.infusionSlot = infusionSlot;
    }

    //Infusing Factory Constructor
    public MetallurgicInfuserUpgradeData(HolderLookup.Provider provider, boolean redstone, RedstoneControl controlType, IEnergyContainer energyContainer, int[] progress,
          IChemicalTank stored, ChemicalInventorySlot infusionSlot, EnergyInventorySlot energySlot, List<IInventorySlot> inputSlots, List<IInventorySlot> outputSlots,
          boolean sorting, List<ITileComponent> components) {
        super(provider, redstone, controlType, energyContainer, progress, energySlot, inputSlots, outputSlots, sorting, components);
        this.stored = stored;
        this.infusionSlot = infusionSlot;
    }
}