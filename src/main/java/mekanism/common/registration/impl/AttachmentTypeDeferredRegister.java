package mekanism.common.registration.impl;

import java.util.Objects;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import mekanism.api.IDisableableEnum;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.math.MathUtils;
import mekanism.common.attachments.containers.AttachedContainers;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.registration.MekanismDeferredHolder;
import mekanism.common.registration.MekanismDeferredRegister;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;

@NothingNullByDefault
public class AttachmentTypeDeferredRegister extends MekanismDeferredRegister<AttachmentType<?>> {

    public AttachmentTypeDeferredRegister(String namespace) {
        super(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, namespace);
    }

    public <CONTAINER extends INBTSerializable<CompoundTag>, ATTACHMENT extends AttachedContainers<CONTAINER>>
    MekanismDeferredHolder<AttachmentType<?>, AttachmentType<ATTACHMENT>> registerContainer(String name, Supplier<ContainerType<CONTAINER, ATTACHMENT, ?>> typeSupplier) {
        return register(name, () -> AttachmentType.serializable(holder -> typeSupplier.get().getDefaultWithLegacy(holder))
              .comparator(AttachedContainers::isCompatible)
              .build());
    }

    public MekanismDeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> registerBoolean(String name, boolean defaultValue) {
        return register(name, () -> AttachmentType.builder(() -> defaultValue)
              //If we are true by default we only care about serializing the value when it is false
              .serialize(defaultValue ? FALSE_SERIALIZER : TRUE_SERIALIZER)
              .comparator(Objects::equals)
              .build());
    }

    public <ENUM extends Enum<ENUM>> MekanismDeferredHolder<AttachmentType<?>, AttachmentType<ENUM>> register(String name, Class<ENUM> clazz) {
        ENUM[] values = clazz.getEnumConstants();
        ENUM defaultValue = values[0];
        IntFunction<ENUM> reader;
        if (clazz.isAssignableFrom(IDisableableEnum.class)) {
            reader = index -> {
                ENUM value = MathUtils.getByIndexMod(values, index);
                return ((IDisableableEnum<?>) value).isEnabled() ? value : defaultValue;
            };
        } else {
            reader = index -> MathUtils.getByIndexMod(values, index);
        }
        return register(name, () -> AttachmentType.builder(() -> defaultValue)
              .serialize(new IAttachmentSerializer<IntTag, ENUM>() {
                  @Nullable
                  @Override
                  public IntTag write(ENUM attachment) {
                      if (attachment == defaultValue) {
                          return null;
                      }
                      return IntTag.valueOf(attachment.ordinal());
                  }

                  @Override
                  public ENUM read(IAttachmentHolder holder, IntTag tag) {
                      return reader.apply(tag.getAsInt());
                  }
              }).comparator(Objects::equals)
              .build());
    }


    private static final IAttachmentSerializer<ByteTag, Boolean> TRUE_SERIALIZER = new IAttachmentSerializer<>() {
        @Nullable
        @Override
        public ByteTag write(Boolean attachment) {
            return attachment ? ByteTag.ONE : null;
        }

        @Override
        public Boolean read(IAttachmentHolder holder, ByteTag tag) {
            return tag.getAsByte() != 0;
        }
    };

    private static final IAttachmentSerializer<ByteTag, Boolean> FALSE_SERIALIZER = new IAttachmentSerializer<>() {
        @Nullable
        @Override
        public ByteTag write(Boolean attachment) {
            return attachment ? null : ByteTag.ZERO;
        }

        @Override
        public Boolean read(IAttachmentHolder holder, ByteTag tag) {
            return tag.getAsByte() != 0;
        }
    };
}