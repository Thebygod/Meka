package mekanism.client.jei;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.ChemicalType;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.infuse.InfuseType;
import mekanism.api.chemical.infuse.InfusionStack;
import mekanism.api.chemical.pigment.Pigment;
import mekanism.api.chemical.pigment.PigmentStack;
import mekanism.api.chemical.slurry.Slurry;
import mekanism.api.chemical.slurry.SlurryStack;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.api.math.FloatingLong;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.gui.robit.GuiRobitRepair;
import mekanism.client.jei.ChemicalStackHelper.GasStackHelper;
import mekanism.client.jei.ChemicalStackHelper.InfusionStackHelper;
import mekanism.client.jei.ChemicalStackHelper.PigmentStackHelper;
import mekanism.client.jei.ChemicalStackHelper.SlurryStackHelper;
import mekanism.client.jei.machine.BoilerRecipeCategory;
import mekanism.client.jei.machine.ChemicalCrystallizerRecipeCategory;
import mekanism.client.jei.machine.ChemicalDissolutionRecipeCategory;
import mekanism.client.jei.machine.ChemicalInfuserRecipeCategory;
import mekanism.client.jei.machine.CombinerRecipeCategory;
import mekanism.client.jei.machine.ElectrolysisRecipeCategory;
import mekanism.client.jei.machine.FluidSlurryToSlurryRecipeCategory;
import mekanism.client.jei.machine.FluidToFluidRecipeCategory;
import mekanism.client.jei.machine.GasToGasRecipeCategory;
import mekanism.client.jei.machine.ItemStackGasToItemStackRecipeCategory;
import mekanism.client.jei.machine.ItemStackToEnergyRecipeCategory;
import mekanism.client.jei.machine.ItemStackToFluidRecipeCategory;
import mekanism.client.jei.machine.ItemStackToGasRecipeCategory;
import mekanism.client.jei.machine.ItemStackToInfuseTypeRecipeCategory;
import mekanism.client.jei.machine.ItemStackToItemStackRecipeCategory;
import mekanism.client.jei.machine.ItemStackToPigmentRecipeCategory;
import mekanism.client.jei.machine.MetallurgicInfuserRecipeCategory;
import mekanism.client.jei.machine.NucleosynthesizingRecipeCategory;
import mekanism.client.jei.machine.PaintingRecipeCategory;
import mekanism.client.jei.machine.PigmentMixerRecipeCategory;
import mekanism.client.jei.machine.PressurizedReactionRecipeCategory;
import mekanism.client.jei.machine.RotaryCondensentratorRecipeCategory;
import mekanism.client.jei.machine.SPSRecipeCategory;
import mekanism.client.jei.machine.SawmillRecipeCategory;
import mekanism.common.Mekanism;
import mekanism.common.MekanismLang;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.config.MekanismConfig;
import mekanism.common.inventory.container.entity.robit.CraftingRobitContainer;
import mekanism.common.inventory.container.item.PortableQIODashboardContainer;
import mekanism.common.inventory.container.tile.QIODashboardContainer;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.registries.MekanismContainerTypes;
import mekanism.common.registries.MekanismFluids;
import mekanism.common.registries.MekanismItems;
import mekanism.common.util.ChemicalUtil;
import mekanism.common.util.RegistryUtils;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IStackHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IModIngredientRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

@JeiPlugin
public class MekanismJEI implements IModPlugin {

    public static final IIngredientType<GasStack> TYPE_GAS = () -> GasStack.class;
    public static final IIngredientType<InfusionStack> TYPE_INFUSION = () -> InfusionStack.class;
    public static final IIngredientType<PigmentStack> TYPE_PIGMENT = () -> PigmentStack.class;
    public static final IIngredientType<SlurryStack> TYPE_SLURRY = () -> SlurryStack.class;

    public static final GasStackHelper GAS_STACK_HELPER = new GasStackHelper();
    public static final InfusionStackHelper INFUSION_STACK_HELPER = new InfusionStackHelper();
    public static final PigmentStackHelper PIGMENT_STACK_HELPER = new PigmentStackHelper();
    public static final SlurryStackHelper SLURRY_STACK_HELPER = new SlurryStackHelper();

    private static final Map<MekanismJEIRecipeType<?>, RecipeType<?>> recipeTypeInstanceCache = new HashMap<>();

    public static RecipeType<?> genericRecipeType(MekanismJEIRecipeType<?> recipeType) {
        return recipeTypeInstanceCache.computeIfAbsent(recipeType, r -> {
            if (r.requiresHolder()) {
                return new RecipeType<>(r.uid(), RecipeHolder.class);
            }
            return new RecipeType<>(r.uid(), r.recipeClass());
        });
    }

    @SuppressWarnings("unchecked")
    public static <TYPE> RecipeType<TYPE> recipeType(MekanismJEIRecipeType<TYPE> recipeType) {
        if (recipeType.requiresHolder()) {
            throw new IllegalStateException("Basic recipe type requested for a recipe that uses holders");
        }
        return (RecipeType<TYPE>) genericRecipeType(recipeType);
    }

    @SuppressWarnings("unchecked")
    public static <TYPE extends Recipe<?>> RecipeType<RecipeHolder<TYPE>> holderRecipeType(MekanismJEIRecipeType<TYPE> recipeType) {
        if (!recipeType.requiresHolder()) {
            throw new IllegalStateException("Holder recipe type requested for a recipe that doesn't use holders");
        }
        return (RecipeType<RecipeHolder<TYPE>>) genericRecipeType(recipeType);
    }

    public static RecipeType<?>[] recipeType(MekanismJEIRecipeType<?>... recipeTypes) {
        return Arrays.stream(recipeTypes).map(MekanismJEI::genericRecipeType).toArray(RecipeType[]::new);
    }

    private static final IIngredientSubtypeInterpreter<ItemStack> MEKANISM_NBT_INTERPRETER = (stack, context) -> {
        if (context == UidContext.Ingredient && (stack.hasTag() || stack.hasAttachments())) {
            String representation = getChemicalComponent(stack, ContainerType.GAS, Capabilities.GAS.item());
            representation = addInterpretation(representation, getChemicalComponent(stack, ContainerType.INFUSION, Capabilities.INFUSION.item()));
            representation = addInterpretation(representation, getChemicalComponent(stack, ContainerType.PIGMENT, Capabilities.PIGMENT.item()));
            representation = addInterpretation(representation, getChemicalComponent(stack, ContainerType.SLURRY, Capabilities.SLURRY.item()));
            representation = addInterpretation(representation, getFluidComponent(stack));
            representation = addInterpretation(representation, getEnergyComponent(stack));
            return representation;
        }
        return IIngredientSubtypeInterpreter.NONE;
    };

    private static String addInterpretation(String nbtRepresentation, String component) {
        return nbtRepresentation.isEmpty() ? component : nbtRepresentation + ":" + component;
    }

    private static String getChemicalComponent(ItemStack stack, ContainerType<?, ? extends IChemicalHandler<?, ?>, ?> containerType,
          ItemCapability<? extends IChemicalHandler<?, ?>, Void> capability) {
        IChemicalHandler<?, ?> handler = containerType.getAttachmentIfPresent(stack);
        if (handler == null) {
            handler = stack.getCapability(capability);
        }
        if (handler != null) {
            String component = "";
            for (int tank = 0, tanks = handler.getTanks(); tank < tanks; tank++) {
                ChemicalStack<?> chemicalStack = handler.getChemicalInTank(tank);
                if (!chemicalStack.isEmpty()) {
                    component = addInterpretation(component, chemicalStack.getTypeRegistryName().toString());
                } else if (tanks > 1) {
                    component = addInterpretation(component, "empty");
                }
            }
            return component;
        }
        return IIngredientSubtypeInterpreter.NONE;
    }

    private static String getFluidComponent(ItemStack stack) {
        IFluidHandler handler = ContainerType.FLUID.getAttachmentIfPresent(stack);
        if (handler == null) {
            handler = Capabilities.FLUID.getCapability(stack);
        }
        if (handler != null) {
            String component = "";
            for (int tank = 0, tanks = handler.getTanks(); tank < tanks; tank++) {
                FluidStack fluidStack = handler.getFluidInTank(tank);
                if (!fluidStack.isEmpty()) {
                    component = addInterpretation(component, RegistryUtils.getName(fluidStack.getFluid()).toString());
                } else if (tanks > 1) {
                    component = addInterpretation(component, "empty");
                }
            }
            return component;
        }
        return IIngredientSubtypeInterpreter.NONE;
    }

    private static String getEnergyComponent(ItemStack stack) {
        IStrictEnergyHandler energyHandlerItem = ContainerType.ENERGY.getAttachmentIfPresent(stack);
        if (energyHandlerItem == null) {
            energyHandlerItem = Capabilities.STRICT_ENERGY.getCapability(stack);
        }
        if (energyHandlerItem != null) {
            String component = "";
            int containers = energyHandlerItem.getEnergyContainerCount();
            for (int container = 0; container < containers; container++) {
                FloatingLong neededEnergy = energyHandlerItem.getNeededEnergy(container);
                if (neededEnergy.isZero()) {
                    component = addInterpretation(component, "filled");
                } else if (containers > 1) {
                    component = addInterpretation(component, "empty");
                }
            }
            return component;
        }
        return IIngredientSubtypeInterpreter.NONE;
    }

    public static IIngredientType<? extends ChemicalStack<?>> getIngredientType(ChemicalType chemicalType) {
        return switch (chemicalType) {
            case GAS -> TYPE_GAS;
            case INFUSION -> TYPE_INFUSION;
            case PIGMENT -> TYPE_PIGMENT;
            case SLURRY -> TYPE_SLURRY;
        };
    }

    @NotNull
    @Override
    public ResourceLocation getPluginUid() {
        return Mekanism.rl("jei_plugin");
    }

    public static void registerItemSubtypes(ISubtypeRegistration registry, Collection<? extends Holder<? extends ItemLike>> itemProviders) {
        for (Holder<? extends ItemLike> itemProvider : itemProviders) {
            //Handle items
            ItemStack stack = new ItemStack(itemProvider.value());
            if (Capabilities.STRICT_ENERGY.hasCapability(stack) || Capabilities.GAS.hasCapability(stack) ||
                Capabilities.INFUSION.hasCapability(stack) || Capabilities.PIGMENT.hasCapability(stack) ||
                Capabilities.SLURRY.hasCapability(stack) || Capabilities.FLUID.hasCapability(stack)) {
                registry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, stack.getItem(), MEKANISM_NBT_INTERPRETER);
            }
        }
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registry) {
        registerItemSubtypes(registry, MekanismItems.ITEMS.getEntries());
        registerItemSubtypes(registry, MekanismBlocks.BLOCKS.getSecondaryEntries());
    }

    @Override
    @SuppressWarnings("RedundantTypeArguments")
    public void registerIngredients(IModIngredientRegistration registry) {
        //The types cannot properly be inferred at runtime
        this.<Gas, GasStack>registerIngredientType(registry, MekanismAPI.GAS_REGISTRY, TYPE_GAS, GAS_STACK_HELPER);
        this.<InfuseType, InfusionStack>registerIngredientType(registry, MekanismAPI.INFUSE_TYPE_REGISTRY, TYPE_INFUSION, INFUSION_STACK_HELPER);
        this.<Pigment, PigmentStack>registerIngredientType(registry, MekanismAPI.PIGMENT_REGISTRY, TYPE_PIGMENT, PIGMENT_STACK_HELPER);
        this.<Slurry, SlurryStack>registerIngredientType(registry, MekanismAPI.SLURRY_REGISTRY, TYPE_SLURRY, SLURRY_STACK_HELPER);
    }

    private <CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>> void registerIngredientType(IModIngredientRegistration registration,
          Registry<CHEMICAL> registry, IIngredientType<STACK> ingredientType, ChemicalStackHelper<CHEMICAL, STACK> stackHelper) {
        List<STACK> types = registry.stream()
              .filter(chemical -> !chemical.isEmptyType() && !chemical.isHidden())
              .map(chemical -> ChemicalUtil.<CHEMICAL, STACK>withAmount(chemical, FluidType.BUCKET_VOLUME))
              .toList();
        stackHelper.setColorHelper(registration.getColorHelper());
        registration.register(ingredientType, types, stackHelper, new ChemicalStackRenderer<>());
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        IGuiHelper guiHelper = registry.getJeiHelpers().getGuiHelper();

        registry.addRecipeCategories(new ChemicalCrystallizerRecipeCategory(guiHelper, MekanismJEIRecipeType.CRYSTALLIZING));
        registry.addRecipeCategories(new ChemicalDissolutionRecipeCategory(guiHelper, MekanismJEIRecipeType.DISSOLUTION));
        registry.addRecipeCategories(new ChemicalInfuserRecipeCategory(guiHelper, MekanismJEIRecipeType.CHEMICAL_INFUSING));
        registry.addRecipeCategories(new FluidSlurryToSlurryRecipeCategory(guiHelper, MekanismJEIRecipeType.WASHING));
        registry.addRecipeCategories(new ElectrolysisRecipeCategory(guiHelper, MekanismJEIRecipeType.SEPARATING));
        registry.addRecipeCategories(new MetallurgicInfuserRecipeCategory(guiHelper, MekanismJEIRecipeType.METALLURGIC_INFUSING));
        registry.addRecipeCategories(new PressurizedReactionRecipeCategory(guiHelper, MekanismJEIRecipeType.REACTION));
        registry.addRecipeCategories(new ItemStackToPigmentRecipeCategory(guiHelper, MekanismJEIRecipeType.PIGMENT_EXTRACTING, MekanismBlocks.PIGMENT_EXTRACTOR));
        registry.addRecipeCategories(new PigmentMixerRecipeCategory(guiHelper, MekanismJEIRecipeType.PIGMENT_MIXING));
        registry.addRecipeCategories(new PaintingRecipeCategory(guiHelper, MekanismJEIRecipeType.PAINTING));

        //Register both methods of rotary condensentrator recipes
        registry.addRecipeCategories(new RotaryCondensentratorRecipeCategory(guiHelper, true));
        registry.addRecipeCategories(new RotaryCondensentratorRecipeCategory(guiHelper, false));

        registry.addRecipeCategories(new ItemStackToGasRecipeCategory(guiHelper, MekanismJEIRecipeType.OXIDIZING, MekanismBlocks.CHEMICAL_OXIDIZER));
        registry.addRecipeCategories(new ItemStackToFluidRecipeCategory(guiHelper, MekanismJEIRecipeType.NUTRITIONAL_LIQUIFICATION, MekanismBlocks.NUTRITIONAL_LIQUIFIER, false));

        registry.addRecipeCategories(new GasToGasRecipeCategory(guiHelper, MekanismJEIRecipeType.ACTIVATING, MekanismBlocks.SOLAR_NEUTRON_ACTIVATOR));
        registry.addRecipeCategories(new GasToGasRecipeCategory(guiHelper, MekanismJEIRecipeType.CENTRIFUGING, MekanismBlocks.ISOTOPIC_CENTRIFUGE));

        registry.addRecipeCategories(new CombinerRecipeCategory(guiHelper, MekanismJEIRecipeType.COMBINING));

        registry.addRecipeCategories(new ItemStackGasToItemStackRecipeCategory(guiHelper, MekanismJEIRecipeType.PURIFYING, MekanismBlocks.PURIFICATION_CHAMBER));
        registry.addRecipeCategories(new ItemStackGasToItemStackRecipeCategory(guiHelper, MekanismJEIRecipeType.COMPRESSING, MekanismBlocks.OSMIUM_COMPRESSOR));
        registry.addRecipeCategories(new ItemStackGasToItemStackRecipeCategory(guiHelper, MekanismJEIRecipeType.INJECTING, MekanismBlocks.CHEMICAL_INJECTION_CHAMBER));

        registry.addRecipeCategories(new NucleosynthesizingRecipeCategory(guiHelper, MekanismJEIRecipeType.NUCLEOSYNTHESIZING));

        registry.addRecipeCategories(new SPSRecipeCategory(guiHelper, MekanismJEIRecipeType.SPS));
        registry.addRecipeCategories(new BoilerRecipeCategory(guiHelper, MekanismJEIRecipeType.BOILER));

        registry.addRecipeCategories(new SawmillRecipeCategory(guiHelper, MekanismJEIRecipeType.SAWING));

        registry.addRecipeCategories(new ItemStackToItemStackRecipeCategory(guiHelper, MekanismJEIRecipeType.ENRICHING, MekanismBlocks.ENRICHMENT_CHAMBER));
        registry.addRecipeCategories(new ItemStackToItemStackRecipeCategory(guiHelper, MekanismJEIRecipeType.CRUSHING, MekanismBlocks.CRUSHER));
        registry.addRecipeCategories(new ItemStackToItemStackRecipeCategory(guiHelper, MekanismJEIRecipeType.SMELTING, MekanismBlocks.ENERGIZED_SMELTER));

        registry.addRecipeCategories(new FluidToFluidRecipeCategory(guiHelper, MekanismJEIRecipeType.EVAPORATING));

        //Conversion recipes
        registry.addRecipeCategories(new ItemStackToEnergyRecipeCategory(guiHelper, MekanismJEIRecipeType.ENERGY_CONVERSION));
        registry.addRecipeCategories(new ItemStackToGasRecipeCategory(guiHelper, MekanismJEIRecipeType.GAS_CONVERSION));
        registry.addRecipeCategories(new ItemStackToInfuseTypeRecipeCategory(guiHelper, MekanismJEIRecipeType.INFUSION_CONVERSION));
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registry) {
        registry.addRecipeClickArea(GuiRobitRepair.class, 102, 48, 22, 15, RecipeTypes.ANVIL);
        registry.addGenericGuiContainerHandler(GuiMekanism.class, new GuiElementHandler(registry.getJeiHelpers().getIngredientManager()));
        registry.addGhostIngredientHandler(GuiMekanism.class, new GhostIngredientHandler<>());
    }

    @Override
    public void registerRecipes(IRecipeRegistration registry) {
        RecipeRegistryHelper.register(registry, MekanismJEIRecipeType.SMELTING, MekanismRecipeType.SMELTING);
        RecipeRegistryHelper.register(registry, MekanismJEIRecipeType.ENRICHING, MekanismRecipeType.ENRICHING);
        RecipeRegistryHelper.register(registry, MekanismJEIRecipeType.CRUSHING, MekanismRecipeType.CRUSHING);
        RecipeRegistryHelper.register(registry, MekanismJEIRecipeType.COMBINING, MekanismRecipeType.COMBINING);
        RecipeRegistryHelper.register(registry, MekanismJEIRecipeType.PURIFYING, MekanismRecipeType.PURIFYING);
        RecipeRegistryHelper.register(registry, MekanismJEIRecipeType.COMPRESSING, MekanismRecipeType.COMPRESSING);
        RecipeRegistryHelper.register(registry, MekanismJEIRecipeType.INJECTING, MekanismRecipeType.INJECTING);
        RecipeRegistryHelper.register(registry, MekanismJEIRecipeType.SAWING, MekanismRecipeType.SAWING);
        RecipeRegistryHelper.register(registry, MekanismJEIRecipeType.METALLURGIC_INFUSING, MekanismRecipeType.METALLURGIC_INFUSING);
        RecipeRegistryHelper.register(registry, MekanismJEIRecipeType.CRYSTALLIZING, MekanismRecipeType.CRYSTALLIZING);
        RecipeRegistryHelper.register(registry, MekanismJEIRecipeType.DISSOLUTION, MekanismRecipeType.DISSOLUTION);
        RecipeRegistryHelper.register(registry, MekanismJEIRecipeType.CHEMICAL_INFUSING, MekanismRecipeType.CHEMICAL_INFUSING);
        RecipeRegistryHelper.register(registry, MekanismJEIRecipeType.OXIDIZING, MekanismRecipeType.OXIDIZING);
        RecipeRegistryHelper.register(registry, MekanismJEIRecipeType.WASHING, MekanismRecipeType.WASHING);
        RecipeRegistryHelper.register(registry, MekanismJEIRecipeType.ACTIVATING, MekanismRecipeType.ACTIVATING);
        RecipeRegistryHelper.register(registry, MekanismJEIRecipeType.CENTRIFUGING, MekanismRecipeType.CENTRIFUGING);
        RecipeRegistryHelper.register(registry, MekanismJEIRecipeType.SEPARATING, MekanismRecipeType.SEPARATING);
        RecipeRegistryHelper.register(registry, MekanismJEIRecipeType.EVAPORATING, MekanismRecipeType.EVAPORATING);
        RecipeRegistryHelper.register(registry, MekanismJEIRecipeType.REACTION, MekanismRecipeType.REACTION);
        RecipeRegistryHelper.register(registry, MekanismJEIRecipeType.NUCLEOSYNTHESIZING, MekanismRecipeType.NUCLEOSYNTHESIZING);
        RecipeRegistryHelper.register(registry, MekanismJEIRecipeType.PIGMENT_EXTRACTING, MekanismRecipeType.PIGMENT_EXTRACTING);
        RecipeRegistryHelper.register(registry, MekanismJEIRecipeType.PIGMENT_MIXING, MekanismRecipeType.PIGMENT_MIXING);
        RecipeRegistryHelper.register(registry, MekanismJEIRecipeType.PAINTING, MekanismRecipeType.PAINTING);
        RecipeRegistryHelper.registerCondensentrator(registry);
        RecipeRegistryHelper.registerNutritionalLiquifier(registry);
        RecipeRegistryHelper.register(registry, MekanismJEIRecipeType.SPS, SPSRecipeCategory.getSPSRecipes());
        RecipeRegistryHelper.register(registry, MekanismJEIRecipeType.BOILER, BoilerRecipeCategory.getBoilerRecipes());
        RecipeRegistryHelper.register(registry, MekanismJEIRecipeType.ENERGY_CONVERSION, MekanismRecipeType.ENERGY_CONVERSION);
        RecipeRegistryHelper.register(registry, MekanismJEIRecipeType.GAS_CONVERSION, MekanismRecipeType.GAS_CONVERSION);
        RecipeRegistryHelper.register(registry, MekanismJEIRecipeType.INFUSION_CONVERSION, MekanismRecipeType.INFUSION_CONVERSION);
        RecipeRegistryHelper.addAnvilRecipes(registry, MekanismItems.HDPE_REINFORCED_ELYTRA, item -> new ItemStack[]{MekanismItems.HDPE_SHEET.getItemStack()});
        //Note: Use a "full" bucket's worth of heavy water, so that JEI renders it as desired in the info page
        registry.addIngredientInfo(MekanismFluids.HEAVY_WATER.getFluidStack(FluidType.BUCKET_VOLUME), NeoForgeTypes.FLUID_STACK,
              MekanismLang.JEI_INFO_HEAVY_WATER.translate(MekanismConfig.general.pumpHeavyWaterAmount.get()));
        registry.addIngredientInfo(MekanismAPI.MODULE_REGISTRY.stream().map(data -> data.getItemProvider().getItemStack()).toList(),
              VanillaTypes.ITEM_STACK, MekanismLang.JEI_INFO_MODULE_INSTALLATION.translate());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registry) {
        //TODO: Eventually we may want to look into trying to make output definitions be invisibly added to categories, and then
        // have the output get calculated in draw, except it would also need to override getTooltip related stuff which won't be
        // super straightforward.
        CatalystRegistryHelper.register(registry, MekanismBlocks.ENRICHMENT_CHAMBER);
        CatalystRegistryHelper.register(registry, MekanismBlocks.CRUSHER);
        CatalystRegistryHelper.register(registry, MekanismBlocks.COMBINER);
        CatalystRegistryHelper.register(registry, MekanismBlocks.PURIFICATION_CHAMBER, MekanismJEIRecipeType.GAS_CONVERSION);
        CatalystRegistryHelper.register(registry, MekanismBlocks.OSMIUM_COMPRESSOR, MekanismJEIRecipeType.GAS_CONVERSION);
        CatalystRegistryHelper.register(registry, MekanismBlocks.CHEMICAL_INJECTION_CHAMBER, MekanismJEIRecipeType.GAS_CONVERSION);
        CatalystRegistryHelper.register(registry, MekanismBlocks.PRECISION_SAWMILL);
        CatalystRegistryHelper.register(registry, MekanismBlocks.METALLURGIC_INFUSER, MekanismJEIRecipeType.INFUSION_CONVERSION);
        CatalystRegistryHelper.register(registry, MekanismBlocks.CHEMICAL_CRYSTALLIZER);
        CatalystRegistryHelper.register(registry, MekanismBlocks.CHEMICAL_DISSOLUTION_CHAMBER, MekanismJEIRecipeType.GAS_CONVERSION);
        CatalystRegistryHelper.register(registry, MekanismBlocks.CHEMICAL_INFUSER);
        CatalystRegistryHelper.register(registry, MekanismBlocks.CHEMICAL_OXIDIZER);
        CatalystRegistryHelper.register(registry, MekanismBlocks.CHEMICAL_WASHER);
        CatalystRegistryHelper.register(registry, MekanismBlocks.SOLAR_NEUTRON_ACTIVATOR);
        CatalystRegistryHelper.register(registry, MekanismBlocks.ELECTROLYTIC_SEPARATOR);
        CatalystRegistryHelper.register(registry, MekanismJEIRecipeType.SPS, MekanismBlocks.SPS_CASING, MekanismBlocks.SPS_PORT, MekanismBlocks.SUPERCHARGED_COIL);
        CatalystRegistryHelper.register(registry, MekanismJEIRecipeType.EVAPORATING, MekanismBlocks.THERMAL_EVAPORATION_CONTROLLER,
              MekanismBlocks.THERMAL_EVAPORATION_VALVE, MekanismBlocks.THERMAL_EVAPORATION_BLOCK);
        CatalystRegistryHelper.register(registry, MekanismJEIRecipeType.BOILER, MekanismBlocks.BOILER_CASING, MekanismBlocks.BOILER_VALVE,
              MekanismBlocks.PRESSURE_DISPERSER, MekanismBlocks.SUPERHEATING_ELEMENT);
        CatalystRegistryHelper.register(registry, MekanismBlocks.PRESSURIZED_REACTION_CHAMBER);
        CatalystRegistryHelper.register(registry, MekanismBlocks.ISOTOPIC_CENTRIFUGE);
        CatalystRegistryHelper.register(registry, MekanismBlocks.NUTRITIONAL_LIQUIFIER);
        CatalystRegistryHelper.register(registry, MekanismBlocks.PIGMENT_EXTRACTOR);
        CatalystRegistryHelper.register(registry, MekanismBlocks.PIGMENT_MIXER);
        CatalystRegistryHelper.register(registry, MekanismBlocks.PAINTING_MACHINE);
        CatalystRegistryHelper.register(registry, MekanismBlocks.ANTIPROTONIC_NUCLEOSYNTHESIZER, MekanismJEIRecipeType.GAS_CONVERSION);
        CatalystRegistryHelper.registerRecipeItem(registry, MekanismBlocks.ROTARY_CONDENSENTRATOR, MekanismJEIRecipeType.CONDENSENTRATING,
              MekanismJEIRecipeType.DECONDENSENTRATING);
        CatalystRegistryHelper.registerRecipeItem(registry, MekanismBlocks.ENERGIZED_SMELTER, MekanismJEIRecipeType.SMELTING, RecipeTypes.SMELTING);
        CatalystRegistryHelper.registerRecipeItem(registry, MekanismBlocks.FORMULAIC_ASSEMBLICATOR, RecipeTypes.CRAFTING);
        CatalystRegistryHelper.registerRecipeItem(registry, MekanismItems.ROBIT, MekanismJEIRecipeType.SMELTING, RecipeTypes.ANVIL,
              RecipeTypes.CRAFTING, RecipeTypes.SMELTING);
        //TODO: Decide if we want to make it so all mekanism energy supporting blocks that have gui's are added as catalysts?
        CatalystRegistryHelper.register(registry, MekanismJEIRecipeType.ENERGY_CONVERSION, MekanismBlocks.BASIC_ENERGY_CUBE, MekanismBlocks.ADVANCED_ENERGY_CUBE,
              MekanismBlocks.ELITE_ENERGY_CUBE, MekanismBlocks.ULTIMATE_ENERGY_CUBE);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registry) {
        IRecipeTransferHandlerHelper transferHelper = registry.getTransferHelper();
        IStackHelper stackHelper = registry.getJeiHelpers().getStackHelper();
        registry.addRecipeTransferHandler(CraftingRobitContainer.class, MekanismContainerTypes.CRAFTING_ROBIT.get(), RecipeTypes.CRAFTING, 1, 9, 10, 36);
        registry.addRecipeTransferHandler(new FormulaicRecipeTransferInfo());
        registry.addRecipeTransferHandler(new QIOCraftingTransferHandler<>(transferHelper, stackHelper, QIODashboardContainer.class), RecipeTypes.CRAFTING);
        registry.addRecipeTransferHandler(new QIOCraftingTransferHandler<>(transferHelper, stackHelper, PortableQIODashboardContainer.class), RecipeTypes.CRAFTING);
    }
}