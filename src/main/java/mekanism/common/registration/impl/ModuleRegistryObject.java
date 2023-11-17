package mekanism.common.registration.impl;

import mekanism.api.gear.ICustomModule;
import mekanism.api.gear.ModuleData;
import mekanism.api.providers.IModuleDataProvider;
import mekanism.common.registration.WrappedRegistryObject;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.NotNull;

public class ModuleRegistryObject<MODULE extends ICustomModule<MODULE>> extends WrappedRegistryObject<ModuleData<?>, ModuleData<MODULE>> implements IModuleDataProvider<MODULE> {

    public ModuleRegistryObject(DeferredHolder<ModuleData<?>, ModuleData<MODULE>> registryObject) {
        super(registryObject);
    }

    @NotNull
    @Override
    public ModuleData<MODULE> getModuleData() {
        return get();
    }
}