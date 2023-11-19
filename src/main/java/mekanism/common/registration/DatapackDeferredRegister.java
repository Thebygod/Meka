package mekanism.common.registration;

import com.mojang.serialization.Codec;
import java.util.function.Function;
import mekanism.api.MekanismAPI;
import mekanism.api.robit.RobitSkin;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.StructureModifier;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;

public class DatapackDeferredRegister<T> extends DeferredCodecRegister<T> {

    public static DatapackDeferredRegister<RobitSkin> robitSkins(String modid) {
        return new DatapackDeferredRegister<>(modid, MekanismAPI.ROBIT_SKIN_SERIALIZER_REGISTRY_NAME, MekanismAPI.ROBIT_SKIN_REGISTRY_NAME);
    }

    public static DatapackDeferredRegister<BiomeModifier> biomeModifiers(String modid) {
        return new DatapackDeferredRegister<>(modid, NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, NeoForgeRegistries.Keys.BIOME_MODIFIERS);
    }

    public static DatapackDeferredRegister<StructureModifier> structureModifiers(String modid) {
        return new DatapackDeferredRegister<>(modid, NeoForgeRegistries.Keys.STRUCTURE_MODIFIER_SERIALIZERS, NeoForgeRegistries.Keys.STRUCTURE_MODIFIERS);
    }

    private final ResourceKey<Registry<T>> datapackRegistryName;

    public DatapackDeferredRegister(String modid, ResourceKey<? extends Registry<Codec<? extends T>>> serializerRegistryName,
          ResourceKey<Registry<T>> datapackRegistryName) {
        this(modid, serializerRegistryName, datapackRegistryName, DeferredCodecHolder::new);
    }

    public DatapackDeferredRegister(String modid, ResourceKey<? extends Registry<Codec<? extends T>>> serializerRegistryName,
          ResourceKey<Registry<T>> datapackRegistryName, Function<ResourceKey<Codec<? extends T>>, ? extends DeferredCodecHolder<T, ? extends T>> holderCreator) {
        super(serializerRegistryName, modid, holderCreator);
        this.datapackRegistryName = datapackRegistryName;
    }

    /**
     * Only call this from mekanism and for custom datapack registries
     */
    public void createAndRegisterDatapack(IEventBus bus, Codec<T> directCodec, @Nullable Codec<T> networkCodec) {
        register(bus);
        //Create a new datapack registry using the direct codec that is created based on the serializer's codec
        bus.addListener((DataPackRegistryEvent.NewRegistry event) -> event.dataPackRegistry(datapackRegistryName, directCodec, networkCodec));
    }

    public ResourceKey<T> dataKey(String name) {
        return ResourceKey.create(datapackRegistryName, new ResourceLocation(getNamespace(), name));
    }
}