package mekanism.api;

import java.util.Objects;
import mekanism.api.providers.IModuleDataProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;

/**
 * Class containing various helpers for sending IMC messages to Mekanism.
 */
public class MekanismIMC {

    private MekanismIMC() {
    }

    /**
     * This method registers an item as a module supporting container. The body of the message should either be a {@link ModuleContainerTarget}.
     * {@link #addModuleContainer(ModuleContainerTarget)} can be used as a helper to send properly structured messages of this type.
     *
     * @since 10.5.0
     * 
     * @see mekanism.api.gear.IModuleHelper#applyModuleContainerProperties(Item.Properties) 
     */
    public static final String ADD_MODULE_CONTAINER = "add_module_container";
    /**
     * This method registers a module or modules as supporting the Meka-Tool. The body of the message should either be an {@link IModuleDataProvider} or an array of
     * {@link IModuleDataProvider}s. {@link #addMekaToolModules(IModuleDataProvider[])} can be used as a helper to send properly structured messages of this type.
     */
    public static final String ADD_MEKA_TOOL_MODULES = "add_meka_tool_modules";
    /**
     * This method registers a module or modules as supporting the MekaSuit Helmet. The body of the message should either be an {@link IModuleDataProvider} or an array of
     * {@link IModuleDataProvider}s. {@link #addMekaSuitHelmetModules(IModuleDataProvider[])} can be used as a helper to send properly structured messages of this type.
     */
    public static final String ADD_MEKA_SUIT_HELMET_MODULES = "add_meka_suit_helmet_modules";
    /**
     * This method registers a module or modules as supporting the MekaSuit Bodyarmor. The body of the message should either be an {@link IModuleDataProvider} or an array
     * of {@link IModuleDataProvider}s. {@link #addMekaSuitBodyarmorModules(IModuleDataProvider[])} can be used as a helper to send properly structured messages of this
     * type.
     */
    public static final String ADD_MEKA_SUIT_BODYARMOR_MODULES = "add_meka_suit_bodyarmor_modules";
    /**
     * This method registers a module or modules as supporting the MekaSuit Pants. The body of the message should either be an {@link IModuleDataProvider} or an array of
     * {@link IModuleDataProvider}s. {@link #addMekaSuitPantsModules(IModuleDataProvider[])} can be used as a helper to send properly structured messages of this type.
     */
    public static final String ADD_MEKA_SUIT_PANTS_MODULES = "add_meka_suit_pants_modules";
    /**
     * This method registers a module or modules as supporting the MekaSuit Boots. The body of the message should either be an {@link IModuleDataProvider} or an array of
     * {@link IModuleDataProvider}s. {@link #addMekaSuitBootsModules(IModuleDataProvider[])} can be used as a helper to send properly structured messages of this type.
     */
    public static final String ADD_MEKA_SUIT_BOOTS_MODULES = "add_meka_suit_boots_modules";

    /**
     * Helper method to register a new module container (for example the MekaSuit and Meka-Tool).
     *
     * @param container Item that will be the module container.
     * @param imcMethod Method used to add modules as supported to the container.
     *
     * @apiNote Call this method during the {@link InterModEnqueueEvent}.
     * @see #addModuleContainer(ModuleContainerTarget)
     */
    public static void addModuleContainer(ItemLike container, String imcMethod) {
        addModuleContainer(new ModuleContainerTarget(container, imcMethod));
    }

    /**
     * Helper method to register a new module container (for example the MekaSuit and Meka-Tool).
     *
     * @param moduleContainer Targeting information for the module container.
     *
     * @apiNote Call this method during the {@link InterModEnqueueEvent}.
     * @see #addModuleContainer(ItemLike, String)
     * @since 10.5.0
     */
    public static void addModuleContainer(ModuleContainerTarget moduleContainer) {
        Objects.requireNonNull(moduleContainer, "Module container cannot be null");
        InterModComms.sendTo(MekanismAPI.MEKANISM_MODID, ADD_MODULE_CONTAINER, () -> moduleContainer);
    }

    /**
     * Helper method to register modules as supported to all module supporting items (MekaSuit and Meka-Tool).
     *
     * @param moduleDataProviders Modules to register as supported.
     *
     * @apiNote Call this method during the {@link InterModEnqueueEvent}.
     * @since 10.5.0
     */
    public static void addModulesToAll(IModuleDataProvider<?>... moduleDataProviders) {
        //TODO - 1.20.4: Evaluate if we want a special IMC call to allow it to be registered to custom module containers as well
        addMekaToolModules(moduleDataProviders);
        addMekaSuitModules(moduleDataProviders);
    }

    /**
     * Helper method to register modules as supported to all MekaSuit pieces.
     *
     * @param moduleDataProviders Modules to register as supported.
     *
     * @apiNote Call this method during the {@link InterModEnqueueEvent}.
     */
    public static void addMekaSuitModules(IModuleDataProvider<?>... moduleDataProviders) {
        addMekaSuitHelmetModules(moduleDataProviders);
        addMekaSuitBodyarmorModules(moduleDataProviders);
        addMekaSuitPantsModules(moduleDataProviders);
        addMekaSuitBootsModules(moduleDataProviders);
    }

    /**
     * Helper method to register modules as supported by the Meka-Tool.
     *
     * @param moduleDataProviders Modules to register as supported.
     *
     * @apiNote Call this method during the {@link InterModEnqueueEvent}.
     */
    public static void addMekaToolModules(IModuleDataProvider<?>... moduleDataProviders) {
        sendModuleIMC(ADD_MEKA_TOOL_MODULES, moduleDataProviders);
    }

    /**
     * Helper method to register modules as supported by the MekaSuit Helmet.
     *
     * @param moduleDataProviders Modules to register as supported.
     *
     * @apiNote Call this method during the {@link InterModEnqueueEvent}.
     */
    public static void addMekaSuitHelmetModules(IModuleDataProvider<?>... moduleDataProviders) {
        sendModuleIMC(ADD_MEKA_SUIT_HELMET_MODULES, moduleDataProviders);
    }

    /**
     * Helper method to register modules as supported by the MekaSuit Bodyarmor.
     *
     * @param moduleDataProviders Modules to register as supported.
     *
     * @apiNote Call this method during the {@link InterModEnqueueEvent}.
     */
    public static void addMekaSuitBodyarmorModules(IModuleDataProvider<?>... moduleDataProviders) {
        sendModuleIMC(ADD_MEKA_SUIT_BODYARMOR_MODULES, moduleDataProviders);
    }

    /**
     * Helper method to register modules as supported by the MekaSuit Pants.
     *
     * @param moduleDataProviders Modules to register as supported.
     *
     * @apiNote Call this method during the {@link InterModEnqueueEvent}.
     */
    public static void addMekaSuitPantsModules(IModuleDataProvider<?>... moduleDataProviders) {
        sendModuleIMC(ADD_MEKA_SUIT_PANTS_MODULES, moduleDataProviders);
    }

    /**
     * Helper method to register modules as supported by the MekaSuit Boots.
     *
     * @param moduleDataProviders Modules to register as supported.
     *
     * @apiNote Call this method during the {@link InterModEnqueueEvent}.
     */
    public static void addMekaSuitBootsModules(IModuleDataProvider<?>... moduleDataProviders) {
        sendModuleIMC(ADD_MEKA_SUIT_BOOTS_MODULES, moduleDataProviders);
    }

    /**
     * Helper method to register modules as supported by module container's that use the given imcMethod.
     *
     * @param imcMethod           Method to call, should match a method that is registered as part of {@link #addModuleContainer(ModuleContainerTarget)}
     * @param moduleDataProviders Modules to register as supported.
     *
     * @apiNote Call this method during the {@link InterModEnqueueEvent}.
     * @since 10.5.0
     */
    public static void sendModuleIMC(String imcMethod, IModuleDataProvider<?>... moduleDataProviders) {
        Objects.requireNonNull(imcMethod, "IMC method cannot be null");
        if (moduleDataProviders == null || moduleDataProviders.length == 0) {
            throw new IllegalArgumentException("No module data providers given.");
        }
        InterModComms.sendTo(MekanismAPI.MEKANISM_MODID, imcMethod, () -> moduleDataProviders);
    }

    /**
     * Contains targeting information for the module container.
     *
     * @param container Item that will be the module container.
     * @param imcMethod Method used to add modules as supported to the container.
     *
     * @since 10.5.0
     */
    public record ModuleContainerTarget(Item container, String imcMethod) {

        /**
         * @param container Item that will be the module container.
         * @param imcMethod Method used to add modules as supported to the container.
         */
        public ModuleContainerTarget(ItemLike container, String imcMethod) {
            this(Objects.requireNonNull(container, "Item cannot be null").asItem(), imcMethod);
        }

        public ModuleContainerTarget {
            Objects.requireNonNull(container, "Item cannot be null");
            Objects.requireNonNull(imcMethod, "IMC method cannot be null");
        }
    }
}