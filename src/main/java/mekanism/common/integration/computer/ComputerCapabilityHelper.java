package mekanism.common.integration.computer;

import java.util.function.BooleanSupplier;
import mekanism.common.Mekanism;
import mekanism.common.integration.computer.computercraft.CCCapabilityHelper;
import mekanism.common.integration.computer.opencomputers2.OC2CapabilityHelper;
import mekanism.common.registration.impl.TileEntityTypeDeferredRegister.BlockEntityTypeBuilder;
import mekanism.common.tile.base.CapabilityTileEntity;

public class ComputerCapabilityHelper {

    public static <TILE extends CapabilityTileEntity & IComputerTile> void addComputerCapabilities(BlockEntityTypeBuilder<TILE> builder, BooleanSupplier supportsComputer) {
        if (Mekanism.hooks.CCLoaded) {
            //If ComputerCraft is loaded add the capability for it
            CCCapabilityHelper.addCapability(builder, supportsComputer);
        }
        if (Mekanism.hooks.OC2Loaded) {
            //If OpenComputers2 is loaded add the capability for it
            OC2CapabilityHelper.addCapability(builder, supportsComputer);
        }
    }
}