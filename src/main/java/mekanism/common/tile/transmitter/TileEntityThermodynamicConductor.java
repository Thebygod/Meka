package mekanism.common.tile.transmitter;

import java.util.Collections;
import java.util.List;
import mekanism.api.heat.IHeatCapacitor;
import mekanism.api.heat.IHeatHandler;
import mekanism.api.heat.IMekanismHeatHandler;
import mekanism.api.providers.IBlockProvider;
import mekanism.api.tier.BaseTier;
import mekanism.common.block.states.BlockStateHelper;
import mekanism.common.block.states.TransmitterType;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.resolver.manager.HeatHandlerManager;
import mekanism.common.content.network.transmitter.ThermodynamicConductor;
import mekanism.common.lib.transmitter.ConnectionType;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.util.EnumUtils;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TileEntityThermodynamicConductor extends TileEntityTransmitter {

    public static final ICapabilityProvider<? super TileEntityThermodynamicConductor, @Nullable Direction, IHeatHandler> HEAT_HANDLER_PROVIDER =
          (tile, side) -> tile.getCapability(Capabilities.HEAT_HANDLER.block(), () -> tile.heatHandlerManager, side);

    private final HeatHandlerManager heatHandlerManager;

    public TileEntityThermodynamicConductor(IBlockProvider blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state);
        //Resolver registered via the provider
        heatHandlerManager = new HeatHandlerManager(direction -> {
            ThermodynamicConductor conductor = getTransmitter();
            if (direction != null && (conductor.getConnectionTypeRaw(direction) == ConnectionType.NONE) || conductor.isRedstoneActivated()) {
                //If we actually have a side, and our connection type on that side is none, or we are currently activated by redstone,
                // then return that we have no capacitors
                return Collections.emptyList();
            }
            return conductor.getHeatCapacitors(direction);
        }, new IMekanismHeatHandler() {
            @NotNull
            @Override
            public List<IHeatCapacitor> getHeatCapacitors(@Nullable Direction side) {
                return heatHandlerManager.getContainers(side);
            }

            @Override
            public void onContentsChanged() {
            }
        });
    }

    @Override
    protected ThermodynamicConductor createTransmitter(IBlockProvider blockProvider) {
        return new ThermodynamicConductor(blockProvider, this);
    }

    @Override
    public ThermodynamicConductor getTransmitter() {
        return (ThermodynamicConductor) super.getTransmitter();
    }

    @Override
    public TransmitterType getTransmitterType() {
        return TransmitterType.THERMODYNAMIC_CONDUCTOR;
    }

    @NotNull
    @Override
    protected BlockState upgradeResult(@NotNull BlockState current, @NotNull BaseTier tier) {
        return BlockStateHelper.copyStateData(current, switch (tier) {
            case BASIC -> MekanismBlocks.BASIC_THERMODYNAMIC_CONDUCTOR;
            case ADVANCED -> MekanismBlocks.ADVANCED_THERMODYNAMIC_CONDUCTOR;
            case ELITE -> MekanismBlocks.ELITE_THERMODYNAMIC_CONDUCTOR;
            case ULTIMATE -> MekanismBlocks.ULTIMATE_THERMODYNAMIC_CONDUCTOR;
            default -> null;
        });
    }

    @Override
    public void sideChanged(@NotNull Direction side, @NotNull ConnectionType old, @NotNull ConnectionType type) {
        super.sideChanged(side, old, type);
        if (type == ConnectionType.NONE) {
            invalidateCapability(Capabilities.HEAT_HANDLER.block(), side);
            //Notify the neighbor on that side our state changed and we no longer have a capability
            //TODO - 1.20.2: I believe we can remove this and other neighbor notify on capability invalidation?
            WorldUtils.notifyNeighborOfChange(level, side, worldPosition);
        } else if (old == ConnectionType.NONE) {
            //Notify the neighbor on that side our state changed, and we now do have a capability
            WorldUtils.notifyNeighborOfChange(level, side, worldPosition);
        }
    }

    @Override
    public void redstoneChanged(boolean powered) {
        super.redstoneChanged(powered);
        if (powered) {
            //The transmitter now is powered by redstone and previously was not
            //Note: While at first glance the below invalidation may seem over aggressive, it is not actually that aggressive as
            // if a cap has not been initialized yet on a side then invalidating it will just NO-OP
            invalidateCapability(Capabilities.HEAT_HANDLER.block(), EnumUtils.DIRECTIONS);
        }
        //Note: We do not have to invalidate any caps if we are going from powered to unpowered as all the caps would already be "empty"
        //TODO: Re-evaluate that ^ (I think we now should be doing so)
    }
}