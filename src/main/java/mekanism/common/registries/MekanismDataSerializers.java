package mekanism.common.registries;

import java.util.UUID;
import mekanism.api.MekanismAPI;
import mekanism.api.robit.RobitSkin;
import mekanism.api.security.SecurityMode;
import mekanism.common.Mekanism;
import mekanism.common.registration.MekanismDeferredHolder;
import mekanism.common.registration.impl.DataSerializerDeferredRegister;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.ResourceKey;

public class MekanismDataSerializers {

    public static final DataSerializerDeferredRegister DATA_SERIALIZERS = new DataSerializerDeferredRegister(Mekanism.MODID);

    public static final MekanismDeferredHolder<EntityDataSerializer<?>, EntityDataSerializer<ResourceKey<RobitSkin>>> ROBIT_SKIN = DATA_SERIALIZERS.register("robit_skin", MekanismAPI.ROBIT_SKIN_REGISTRY_NAME);
    public static final MekanismDeferredHolder<EntityDataSerializer<?>, EntityDataSerializer<SecurityMode>> SECURITY = DATA_SERIALIZERS.registerEnum("security", SecurityMode.class);
    public static final MekanismDeferredHolder<EntityDataSerializer<?>, EntityDataSerializer<UUID>> UUID = DATA_SERIALIZERS.registerSimple("uuid", FriendlyByteBuf::writeUUID, FriendlyByteBuf::readUUID);
}