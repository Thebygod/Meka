package mekanism.common.integration.energy;

import java.util.Collection;
import java.util.Collections;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.common.capabilities.Capabilities.MultiTypeCapability;
import mekanism.common.config.value.CachedValue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.Nullable;

@NothingNullByDefault
public interface IEnergyCompat {//TODO - 1.20.2: Document methods we added

    /**
     * Whether this energy compat is actually enabled.
     *
     * @return if this energy compat is enabled.
     */
    boolean isUsable();

    default boolean capabilityExists() {
        return true;
    }

    /**
     * Gets the configs that back {@link #isUsable()} so that caching for usable and enabled energy types can be done.
     *
     * @implNote If this {@link IEnergyCompat} will never be usable due to missing required mods, this should just return an empty collection to allow the enabled caching
     * to skip listening to the corresponding config settings.
     */
    default Collection<CachedValue<?>> getBackingConfigs() {
        return Collections.emptySet();
    }

    /**
     * Gets the capability this compat integrates with.
     *
     * @return The capability this compat is integrating with.
     */
    MultiTypeCapability<?> getCapability();

    /**
     * Gets the {@link IStrictEnergyHandler} as a lazy optional for the capability this energy compat is for.
     *
     * @param handler The handler to wrap
     *
     * @return A lazy optional for this capability
     */
    <OBJECT, CONTEXT> ICapabilityProvider<OBJECT, CONTEXT, ?> getProviderAs(ICapabilityProvider<OBJECT, CONTEXT, IStrictEnergyHandler> provider);

    Object wrapStrictEnergyHandler(IStrictEnergyHandler handler);

    /**
     * Wraps the capability implemented in the provider into a lazy optional {@link IStrictEnergyHandler}, or returns {@code LazyOptional.empty()} if the capability is
     * not implemented.
     *
     * @param provider Capability provider
     * @param side     Side
     *
     * @return The capability implemented in the provider into an {@link IStrictEnergyHandler}, or {@code null} if the capability is not implemented.
     */
    @Nullable
    IStrictEnergyHandler getAsStrictEnergyHandler(Level level, BlockPos pos, @Nullable Direction context);

    CacheConverter<?> getCacheAndConverter(ServerLevel level, BlockPos pos, @Nullable Direction context, BooleanSupplier isValid, Runnable invalidationListener);

    @Nullable
    IStrictEnergyHandler getStrictEnergyHandler(ItemStack stack);

    record CacheConverter<CAPABILITY>(BlockCapabilityCache<CAPABILITY, @Nullable Direction> rawCache, Function<CAPABILITY, IStrictEnergyHandler> convertToStrict) {
    }
}