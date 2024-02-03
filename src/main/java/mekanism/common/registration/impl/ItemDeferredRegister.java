package mekanism.common.registration.impl;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.text.EnumColor;
import mekanism.api.text.TextComponentUtil;
import mekanism.common.content.gear.ModuleHelper;
import mekanism.common.item.ItemModule;
import mekanism.common.registration.MekanismDeferredHolder;
import mekanism.common.registration.MekanismDeferredRegister;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.jetbrains.annotations.NotNull;

@NothingNullByDefault
public class ItemDeferredRegister extends MekanismDeferredRegister<Item> {

    public ItemDeferredRegister(String modid) {
        super(Registries.ITEM, modid, ItemRegistryObject::new);
    }

    @Override
    public void register(@NotNull IEventBus bus) {
        super.register(bus);
        bus.addListener(RegisterCapabilitiesEvent.class, event -> forEntries(registryObject -> registryObject.registerCapabilities(event)));
        //Listen at the lowest priority so that it happens after our elements have been registered
        // and then see if any need to apply attachments
        bus.addListener(EventPriority.LOWEST, RegisterEvent.class, event -> {
            if (event.getRegistryKey().equals(Registries.ITEM)) {
                forEntries(registryObject -> registryObject.attachDefaultContainers(bus));
            }
        });
    }

    private void forEntries(Consumer<ItemRegistryObject<?>> consumer) {
        for (Holder<Item> entry : getEntries()) {
            //Note: All entries should be of this type
            if (entry instanceof ItemRegistryObject<?> registryObject) {
                consumer.accept(registryObject);
            } else if (!FMLEnvironment.production) {
                throw new IllegalStateException("Expected entry to be an ItemRegistryObject");
            }
        }
    }

    public ItemRegistryObject<Item> register(String name) {
        return registerItem(name, Item::new);
    }

    public ItemRegistryObject<Item> registerUnburnable(String name) {
        return registerUnburnable(name, Item::new);
    }

    public ItemRegistryObject<Item> register(String name, Rarity rarity) {
        return registerItem(name, properties -> new Item(properties.rarity(rarity)));
    }

    public ItemRegistryObject<Item> register(String name, EnumColor color) {
        return registerItem(name, properties -> new Item(properties) {
            @NotNull
            @Override
            public Component getName(@NotNull ItemStack stack) {
                return TextComponentUtil.build(color, super.getName(stack));
            }
        });
    }

    public ItemRegistryObject<ItemModule> registerModule(ModuleRegistryObject<?> moduleDataSupplier) {
        //Note: We use the internal helper just in case we end up needing to know it is an ItemModule instead of just an Item somewhere
        return register("module_" + moduleDataSupplier.getName(), () -> ModuleHelper.get().createModuleItem(moduleDataSupplier, new Item.Properties()));
    }

    public <ITEM extends Item> ItemRegistryObject<ITEM> registerItem(String name, Function<Item.Properties, ITEM> sup) {
        return register(name, () -> sup.apply(new Item.Properties()));
    }

    public <ITEM extends Item> ItemRegistryObject<ITEM> registerUnburnable(String name, Function<Item.Properties, ITEM> sup) {
        return register(name, () -> sup.apply(new Item.Properties().fireResistant()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <ITEM extends Item> ItemRegistryObject<ITEM> register(String name, Supplier<? extends ITEM> sup) {
        return (ItemRegistryObject<ITEM>) super.register(name, sup);
    }

    public ItemRegistryObject<DeferredSpawnEggItem> registerSpawnEgg(MekanismDeferredHolder<EntityType<?>, ? extends EntityType<? extends Mob>> entityTypeProvider,
          int primaryColor, int secondaryColor) {
        return registerItem(entityTypeProvider.getName() + "_spawn_egg", props -> new DeferredSpawnEggItem(entityTypeProvider, primaryColor, secondaryColor, props));
    }
}