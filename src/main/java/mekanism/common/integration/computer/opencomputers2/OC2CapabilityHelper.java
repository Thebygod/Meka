package mekanism.common.integration.computer.opencomputers2;

import java.util.function.BooleanSupplier;
import mekanism.common.integration.computer.IComputerTile;
import mekanism.common.registration.impl.TileEntityTypeDeferredRegister.BlockEntityTypeBuilder;
import mekanism.common.tile.base.CapabilityTileEntity;

public class OC2CapabilityHelper {

    /*private static final BlockCapability<Device, @Nullable Direction> CAPABILITY = BlockCapability.createSided(new ResourceLocation(MekanismHooks.OC2_MOD_ID, name), Device.class);
    private static final ICapabilityProvider<?, @Nullable Direction, Device> PROVIDER = getProvider();

    private static <TILE extends CapabilityTileEntity & IComputerTile> ICapabilityProvider<TILE, @Nullable Direction, Device> getProvider() {
        return CapabilityTileEntity.capabilityProvider(CAPABILITY, (tile, cap) -> {
            if (tile.isComputerCapabilityPersistent()) {
                return BasicCapabilityResolver.persistent(CAPABILITY, () -> MekanismDevice.create(tile));
            }
            return BasicCapabilityResolver.create(CAPABILITY, () -> MekanismDevice.create(tile));
        });
    }*/

    @SuppressWarnings("unchecked")
    public static <TILE extends CapabilityTileEntity & IComputerTile> void addCapability(BlockEntityTypeBuilder<TILE> builder, BooleanSupplier supportsComputer) {
        //TODO - 1.20.2: Reimplement when OC2 updates
        //builder.with(CAPABILITY, (ICapabilityProvider<? super TILE, @Nullable Direction, Device>) PROVIDER, supportsComputer);
    }
}