package mekanism.common.integration.energy.forgeenergy;

import java.util.Collection;
import java.util.Set;
import java.util.function.BooleanSupplier;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.Capabilities.MultiTypeCapability;
import mekanism.common.config.MekanismConfig;
import mekanism.common.config.value.CachedValue;
import mekanism.common.integration.energy.IEnergyCompat;
import mekanism.common.util.UnitDisplayUtils.EnergyUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

@NothingNullByDefault
public class ForgeEnergyCompat implements IEnergyCompat {

    @Override
    public MultiTypeCapability<IEnergyStorage> getCapability() {
        return Capabilities.ENERGY;
    }

    @Override
    public boolean isUsable() {
        return EnergyUnit.FORGE_ENERGY.isEnabled();
    }

    @Override
    public Collection<CachedValue<?>> getBackingConfigs() {
        return Set.of(MekanismConfig.general.blacklistForge);
    }

    @Override
    public <OBJECT, CONTEXT> ICapabilityProvider<OBJECT, CONTEXT, IEnergyStorage> getProviderAs(ICapabilityProvider<OBJECT, CONTEXT, IStrictEnergyHandler> provider) {
        return (obj, ctx) -> {
            IStrictEnergyHandler handler = provider.getCapability(obj, ctx);
            return handler != null && isUsable() ? wrapStrictEnergyHandler(handler) : null;
        };
    }

    @Override
    public IEnergyStorage wrapStrictEnergyHandler(IStrictEnergyHandler handler) {
        return new ForgeEnergyIntegration(handler);
    }

    @Nullable
    @Override
    public IStrictEnergyHandler getAsStrictEnergyHandler(Level level, BlockPos pos, @Nullable Direction context) {
        IEnergyStorage capability = level.getCapability(getCapability().block(), pos, context);
        return capability == null ? null : new ForgeStrictEnergyHandler(capability);
    }

    @Override
    public CacheConverter<IEnergyStorage> getCacheAndConverter(ServerLevel level, BlockPos pos, @Nullable Direction context, BooleanSupplier isValid,
          Runnable invalidationListener) {
        return new CacheConverter<>(BlockCapabilityCache.create(getCapability().block(), level, pos, context, isValid, invalidationListener), ForgeStrictEnergyHandler::new);
    }

    @Nullable
    @Override
    public IStrictEnergyHandler getStrictEnergyHandler(ItemStack stack) {
        IEnergyStorage capability = stack.getCapability(getCapability().item());
        return capability == null ? null : new ForgeStrictEnergyHandler(capability);
    }
}