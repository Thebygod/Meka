package mekanism.common.registration.impl;

import mekanism.common.registration.WrappedRegistryObject;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.neoforged.neoforge.registries.DeferredHolder;

public class PlacementModifierRegistryObject<PROVIDER extends PlacementModifier> extends WrappedRegistryObject<PlacementModifierType<?>, PlacementModifierType<PROVIDER>> {

    public PlacementModifierRegistryObject(DeferredHolder<PlacementModifierType<?>, PlacementModifierType<PROVIDER>> registryObject) {
        super(registryObject);
    }
}