package mekanism.api;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasBuilder;
import mekanism.api.chemical.infuse.InfuseType;
import mekanism.api.chemical.infuse.InfuseTypeBuilder;
import mekanism.api.chemical.pigment.Pigment;
import mekanism.api.chemical.pigment.PigmentBuilder;
import mekanism.api.chemical.slurry.Slurry;
import mekanism.api.chemical.slurry.SlurryBuilder;
import mekanism.api.gear.ModuleData;
import mekanism.api.recipes.ingredients.chemical.IGasIngredient;
import mekanism.api.recipes.ingredients.chemical.IInfusionIngredient;
import mekanism.api.recipes.ingredients.chemical.IPigmentIngredient;
import mekanism.api.recipes.ingredients.chemical.ISlurryIngredient;
import mekanism.api.robit.RobitSkin;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegistryBuilder;
import org.slf4j.Logger;

@NothingNullByDefault
public class MekanismAPI {

    private MekanismAPI() {
    }

    /**
     * The version of the api classes - may not always match the mod's version
     */
    public static final String API_VERSION = "10.6.0";
    /**
     * Mekanism's Mod ID
     */
    public static final String MEKANISM_MODID = "mekanism";
    /**
     * Mekanism debug mode
     */
    public static boolean debug = false;
    /**
     * Logger for use in Mekanism's API classes
     */
    public static final Logger logger = LogUtils.getLogger();

    private static ResourceLocation rl(String path) {
        return new ResourceLocation(MEKANISM_MODID, path);
    }

    private static <T> ResourceKey<Registry<T>> registryKey(@SuppressWarnings("unused") Class<T> compileTimeTypeValidator, String path) {
        return ResourceKey.createRegistryKey(rl(path));
    }

    private static <T> ResourceKey<Registry<MapCodec<? extends T>>> codecRegistryKey(@SuppressWarnings("unused") Class<T> compileTimeTypeValidator, String path) {
        return ResourceKey.createRegistryKey(rl(path));
    }

    /**
     * Gets the {@link ResourceKey} representing the name of the Registry for {@link Gas gases}.
     *
     * @apiNote When registering {@link Gas gases} using {@link DeferredRegister}, use this field to get access to the {@link ResourceKey}.
     * @since 10.4.0
     */
    public static final ResourceKey<Registry<Gas>> GAS_REGISTRY_NAME = registryKey(Gas.class, "gas");
    /**
     * Gets the {@link ResourceKey} representing the name of the Registry for {@link IGasIngredient} ingredient type serializers.
     *
     * @apiNote When registering gas ingredient types using {@link DeferredRegister}, use this field to get access to the {@link ResourceKey}.
     * @since 10.6.0
     */
    public static final ResourceKey<Registry<MapCodec<? extends IGasIngredient>>> GAS_INGREDIENT_TYPE_REGISTRY_NAME = codecRegistryKey(IGasIngredient.class, "gas_ingredient_type");
    /**
     * Gets the {@link ResourceKey} representing the name of the Registry for {@link InfuseType infuse types}.
     *
     * @apiNote When registering {@link InfuseType infuse types} using {@link DeferredRegister}, use this field to get access to the {@link ResourceKey}.
     * @since 10.4.0
     */
    public static final ResourceKey<Registry<InfuseType>> INFUSE_TYPE_REGISTRY_NAME = registryKey(InfuseType.class, "infuse_type");
    /**
     * Gets the {@link ResourceKey} representing the name of the Registry for {@link IInfusionIngredient} ingredient type serializers.
     *
     * @apiNote When registering infusion ingredient types using {@link DeferredRegister}, use this field to get access to the {@link ResourceKey}.
     * @since 10.6.0
     */
    public static final ResourceKey<Registry<MapCodec<? extends IInfusionIngredient>>> INFUSION_INGREDIENT_TYPE_REGISTRY_NAME = codecRegistryKey(IInfusionIngredient.class, "infusion_ingredient_type");
    /**
     * Gets the {@link ResourceKey} representing the name of the Registry for {@link Pigment pigments}.
     *
     * @apiNote When registering {@link Pigment pigments} using {@link DeferredRegister}, use this field to get access to the {@link ResourceKey}.
     * @since 10.4.0
     */
    public static final ResourceKey<Registry<Pigment>> PIGMENT_REGISTRY_NAME = registryKey(Pigment.class, "pigment");
    /**
     * Gets the {@link ResourceKey} representing the name of the Registry for {@link IPigmentIngredient} ingredient type serializers.
     *
     * @apiNote When registering pigment ingredient types using {@link DeferredRegister}, use this field to get access to the {@link ResourceKey}.
     * @since 10.6.0
     */
    public static final ResourceKey<Registry<MapCodec<? extends IPigmentIngredient>>> PIGMENT_INGREDIENT_TYPE_REGISTRY_NAME = codecRegistryKey(IPigmentIngredient.class, "pigment_ingredient_type");
    /**
     * Gets the {@link ResourceKey} representing the name of the Registry for {@link Slurry sluries}.
     *
     * @apiNote When registering {@link Slurry sluries} using {@link DeferredRegister}, use this field to get access to the {@link ResourceKey}.
     * @since 10.4.0
     */
    public static final ResourceKey<Registry<Slurry>> SLURRY_REGISTRY_NAME = registryKey(Slurry.class, "slurry");
    /**
     * Gets the {@link ResourceKey} representing the name of the Registry for {@link ISlurryIngredient} ingredient type serializers.
     *
     * @apiNote When registering slurry ingredient types using {@link DeferredRegister}, use this field to get access to the {@link ResourceKey}.
     * @since 10.6.0
     */
    public static final ResourceKey<Registry<MapCodec<? extends ISlurryIngredient>>> SLURRY_INGREDIENT_TYPE_REGISTRY_NAME = codecRegistryKey(ISlurryIngredient.class, "slurry_ingredient_type");
    /**
     * Gets the {@link ResourceKey} representing the name of the Registry for {@link ModuleData modules}.
     *
     * @apiNote When registering {@link ModuleData modules} using {@link DeferredRegister}, use this field to get access to the {@link ResourceKey}.
     * @since 10.4.0
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static final ResourceKey<Registry<ModuleData<?>>> MODULE_REGISTRY_NAME = registryKey((Class) ModuleData.class, "module");
    /**
     * Gets the {@link ResourceKey} representing the name of the Datapack Registry for {@link RobitSkin robit skins}.
     *
     * @since 10.4.0
     */
    public static final ResourceKey<Registry<RobitSkin>> ROBIT_SKIN_REGISTRY_NAME = registryKey(RobitSkin.class, "robit_skin");
    /**
     * Gets the {@link ResourceKey} representing the name of the Registry for {@link RobitSkin robit skin} serializers.
     *
     * @apiNote When registering {@link RobitSkin robit skin} serializers using {@link DeferredRegister}, use this field to get access to the {@link ResourceKey}.
     * @since 10.4.0
     */
    public static final ResourceKey<Registry<MapCodec<? extends RobitSkin>>> ROBIT_SKIN_SERIALIZER_REGISTRY_NAME = codecRegistryKey(RobitSkin.class, "robit_skin_serializer");

    /**
     * Gets the Registry for {@link Gas}.
     *
     * @see #GAS_REGISTRY_NAME
     * @since 10.5.0
     */
    public static final Registry<Gas> GAS_REGISTRY = new RegistryBuilder<>(GAS_REGISTRY_NAME)
          .defaultKey(rl("empty"))
          .sync(true)
          .create();
    /**
     * Gets the Registry for {@link IGasIngredient} type serializers.
     *
     * @see #GAS_INGREDIENT_TYPE_REGISTRY_NAME
     * @since 10.6.0
     */
    public static final Registry<MapCodec<? extends IGasIngredient>> GAS_INGREDIENT_TYPES = new RegistryBuilder<>(GAS_INGREDIENT_TYPE_REGISTRY_NAME)
          .sync(true)
          .create();
    /**
     * Gets the Registry for {@link InfuseType}.
     *
     * @see #INFUSE_TYPE_REGISTRY_NAME
     * @since 10.5.0
     */
    public static final Registry<InfuseType> INFUSE_TYPE_REGISTRY = new RegistryBuilder<>(INFUSE_TYPE_REGISTRY_NAME)
          .defaultKey(rl("empty"))
          .sync(true)
          .create();
    /**
     * Gets the Registry for {@link IInfusionIngredient} type serializers.
     *
     * @see #INFUSION_INGREDIENT_TYPE_REGISTRY_NAME
     * @since 10.6.0
     */
    public static final Registry<MapCodec<? extends IInfusionIngredient>> INFUSION_INGREDIENT_TYPES = new RegistryBuilder<>(INFUSION_INGREDIENT_TYPE_REGISTRY_NAME)
          .sync(true)
          .create();
    /**
     * Gets the Registry for {@link Pigment}.
     *
     * @see #PIGMENT_REGISTRY_NAME
     * @since 10.5.0
     */
    public static final Registry<Pigment> PIGMENT_REGISTRY = new RegistryBuilder<>(PIGMENT_REGISTRY_NAME)
          .defaultKey(rl("empty"))
          .sync(true)
          .create();
    /**
     * Gets the Registry for {@link IPigmentIngredient} type serializers.
     *
     * @see #PIGMENT_INGREDIENT_TYPE_REGISTRY_NAME
     * @since 10.6.0
     */
    public static final Registry<MapCodec<? extends IPigmentIngredient>> PIGMENT_INGREDIENT_TYPES = new RegistryBuilder<>(PIGMENT_INGREDIENT_TYPE_REGISTRY_NAME)
          .sync(true)
          .create();
    /**
     * Gets the Registry for {@link Slurry}.
     *
     * @see #SLURRY_REGISTRY_NAME
     * @since 10.5.0
     */
    public static final Registry<Slurry> SLURRY_REGISTRY = new RegistryBuilder<>(SLURRY_REGISTRY_NAME)
          .defaultKey(rl("empty"))
          .sync(true)
          .create();
    /**
     * Gets the Registry for {@link ISlurryIngredient} type serializers.
     *
     * @see #SLURRY_INGREDIENT_TYPE_REGISTRY_NAME
     * @since 10.6.0
     */
    public static final Registry<MapCodec<? extends ISlurryIngredient>> SLURRY_INGREDIENT_TYPES = new RegistryBuilder<>(SLURRY_INGREDIENT_TYPE_REGISTRY_NAME)
          .sync(true)
          .create();
    /**
     * Gets the Registry for {@link ModuleData}.
     *
     * @see #MODULE_REGISTRY_NAME
     * @since 10.5.0
     */
    public static final Registry<ModuleData<?>> MODULE_REGISTRY = new RegistryBuilder<>(MODULE_REGISTRY_NAME)
          .sync(true)
          .create();
    /**
     * Gets the Registry for {@link RobitSkin} serializers.
     *
     * @see #ROBIT_SKIN_SERIALIZER_REGISTRY_NAME
     * @since 10.5.0
     */
    public static final Registry<MapCodec<? extends RobitSkin>> ROBIT_SKIN_SERIALIZER_REGISTRY = new RegistryBuilder<>(ROBIT_SKIN_SERIALIZER_REGISTRY_NAME).create();

    /**
     * Constant location representing the name all empty chemicals will be registered under.
     *
     * @since 10.6.0
     */
    public static final ResourceLocation EMPTY_CHEMICAL_NAME = new ResourceLocation(MEKANISM_MODID, "empty");

    //TODO: Potentially define these with DeferredHolder for purposes of fully defining them outside of the API
    // would have some minor issues with how the empty stacks are declared
    /**
     * Empty Gas instance.
     */
    public static final Gas EMPTY_GAS = new Gas(GasBuilder.builder().hidden());
    /**
     * Empty Infuse Type instance.
     */
    public static final InfuseType EMPTY_INFUSE_TYPE = new InfuseType(InfuseTypeBuilder.builder().hidden());
    /**
     * Empty Pigment instance.
     */
    public static final Pigment EMPTY_PIGMENT = new Pigment(PigmentBuilder.builder().hidden());
    /**
     * Empty Slurry instance.
     */
    public static final Slurry EMPTY_SLURRY = new Slurry(SlurryBuilder.clean().hidden());
}