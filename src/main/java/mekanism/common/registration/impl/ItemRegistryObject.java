package mekanism.common.registration.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import mekanism.api.chemical.merged.MergedChemicalTank;
import mekanism.api.providers.IItemProvider;
import mekanism.common.attachments.IAttachmentAware;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.capabilities.ICapabilityAware;
import mekanism.common.capabilities.merged.MergedTank;
import mekanism.common.config.IMekanismConfig;
import mekanism.common.registration.MekanismDeferredHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ItemRegistryObject<ITEM extends Item> extends MekanismDeferredHolder<Item, ITEM> implements IItemProvider {

    @Nullable
    private Map<ContainerType<?, ?, ?>, Function<ItemStack, ? extends List<?>>> defaultContainers;
    @Nullable
    private List<Consumer<RegisterCapabilitiesEvent>> containerCapabilities;

    public ItemRegistryObject(ResourceKey<Item> key) {
        super(key);
    }

    @NotNull
    @Override
    public ITEM asItem() {
        return value();
    }

    @Internal
    public <CONTAINER extends INBTSerializable<CompoundTag>> ItemRegistryObject<ITEM> addAttachmentOnlyContainer(ContainerType<CONTAINER, ?, ?> containerType,
          Function<ItemStack, CONTAINER> defaultCreator) {
        return addAttachmentOnlyContainers(containerType, defaultCreator.andThen(List::of));
    }

    @Internal
    public <CONTAINER extends INBTSerializable<CompoundTag>> ItemRegistryObject<ITEM> addAttachmentOnlyContainers(ContainerType<CONTAINER, ?, ?> containerType,
          Function<ItemStack, List<CONTAINER>> defaultCreators) {
        if (defaultContainers == null) {
            defaultContainers = new HashMap<>();
        }
        if (defaultContainers.put(containerType, defaultCreators) != null) {
            throw new IllegalStateException("Duplicate attachments added for container type: " + containerType.getAttachmentName());
        }
        return this;
    }

    @Internal
    public <CONTAINER extends INBTSerializable<CompoundTag>> ItemRegistryObject<ITEM> addAttachedContainerCapability(ContainerType<CONTAINER, ?, ?> containerType,
          Function<ItemStack, CONTAINER> defaultCreator, IMekanismConfig... requiredConfigs) {
        return addAttachedContainerCapabilities(containerType, defaultCreator.andThen(List::of), requiredConfigs);
    }

    @Internal
    public <CONTAINER extends INBTSerializable<CompoundTag>> ItemRegistryObject<ITEM> addAttachedContainerCapabilities(ContainerType<CONTAINER, ?, ?> containerType,
          Function<ItemStack, List<CONTAINER>> defaultCreators, IMekanismConfig... requiredConfigs) {
        addAttachmentOnlyContainers(containerType, defaultCreators);
        return addContainerCapability(containerType, requiredConfigs);
    }

    @Internal
    private ItemRegistryObject<ITEM> addContainerCapability(ContainerType<?, ?, ?> containerType, IMekanismConfig... requiredConfigs) {
        if (containerCapabilities == null) {
            containerCapabilities = new ArrayList<>();
        }
        containerCapabilities.add(event -> containerType.registerItemCapabilities(event, asItem(), requiredConfigs));
        return this;
    }

    @Internal
    public <TANK extends MergedChemicalTank> ItemRegistryObject<ITEM> addMissingMergedAttachments(Supplier<AttachmentType<TANK>> backingAttachment, boolean supportsFluid) {
        return addMissingMergedTanks(backingAttachment, supportsFluid, containerType -> {
        });
    }

    @Internal
    public <TANK extends MergedChemicalTank> ItemRegistryObject<ITEM> addMissingMergedCapabilityTanks(Supplier<AttachmentType<TANK>> backingAttachment, boolean supportsFluid) {
        return addMissingMergedTanks(backingAttachment, supportsFluid, this::addContainerCapability);
    }

    private <TANK extends MergedChemicalTank> ItemRegistryObject<ITEM> addMissingMergedTanks(Supplier<AttachmentType<TANK>> backingAttachment, boolean supportsFluid,
          Consumer<ContainerType<?, ?, ?>> onAdded) {
        int added = addMissingTankType(ContainerType.GAS, onAdded, stack -> stack.getData(backingAttachment).getGasTank());
        added += addMissingTankType(ContainerType.INFUSION, onAdded, stack -> stack.getData(backingAttachment).getInfusionTank());
        added += addMissingTankType(ContainerType.PIGMENT, onAdded, stack -> stack.getData(backingAttachment).getPigmentTank());
        added += addMissingTankType(ContainerType.SLURRY, onAdded, stack -> stack.getData(backingAttachment).getSlurryTank());
        if (supportsFluid) {
            Supplier<AttachmentType<MergedTank>> attachment = (Supplier) backingAttachment;
            added += addMissingTankType(ContainerType.FLUID, onAdded, stack -> stack.getData(attachment).getFluidTank());
        }
        if (added == 0) {
            throw new IllegalStateException("Unnecessary addMissingMergedAttachments call");
        }
        return this;
    }

    private <CONTAINER extends INBTSerializable<CompoundTag>> int addMissingTankType(ContainerType<CONTAINER, ?, ?> containerType, Consumer<ContainerType<?, ?, ?>> onAdded,
          Function<ItemStack, CONTAINER> defaultCreator) {
        if (defaultContainers != null && defaultContainers.containsKey(containerType)) {
            return 0;
        }
        addAttachmentOnlyContainer(containerType, defaultCreator);
        onAdded.accept(containerType);
        return 1;
    }

    @Internal
    void registerCapabilities(RegisterCapabilitiesEvent event) {
        if (asItem() instanceof ICapabilityAware capabilityAware) {
            capabilityAware.attachCapabilities(event);
        }
        if (containerCapabilities != null) {
            containerCapabilities.forEach(consumer -> consumer.accept(event));
            //We only allow registering once, and then we allow the memory to be freed up
            containerCapabilities = null;
        }
    }

    @Internal
    @SuppressWarnings({"unchecked", "rawtypes"})
    void attachDefaultContainers(IEventBus eventBus) {
        ITEM item = asItem();
        if (item instanceof IAttachmentAware attachmentAware) {
            attachmentAware.attachAttachments(eventBus);
        }
        if (defaultContainers != null) {
            //Note: We pass null for the event bus to not expose this attachment as a capability
            defaultContainers.forEach(((containerType, defaultCreators) -> containerType.addDefaultContainers(null, item, (Function) defaultCreators)));
            //We only allow them being attached once
            defaultContainers = null;
        }
    }
}