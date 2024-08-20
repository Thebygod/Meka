package mekanism.common.tile.machine;

import java.util.List;
import mekanism.api.IContentsListener;
import mekanism.api.Upgrade;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.recipes.ChemicalChemicalToChemicalRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.api.recipes.cache.ChemicalChemicalToChemicalCachedRecipe;
import mekanism.api.recipes.inputs.IInputHandler;
import mekanism.api.recipes.inputs.InputHelper;
import mekanism.api.recipes.outputs.IOutputHandler;
import mekanism.api.recipes.outputs.OutputHelper;
import mekanism.api.recipes.vanilla_input.BiChemicalRecipeInput;
import mekanism.client.recipe_viewer.type.IRecipeViewerRecipeType;
import mekanism.client.recipe_viewer.type.RecipeViewerRecipeType;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.chemical.ChemicalTankHelper;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper.ComputerChemicalTankWrapper;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper;
import mekanism.common.integration.computer.annotation.ComputerMethod;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import mekanism.common.integration.computer.computercraft.ComputerConstants;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.inventory.container.sync.SyncableLong;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.inventory.slot.chemical.ChemicalInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.recipe.IMekanismRecipeTypeProvider;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.recipe.lookup.IEitherSideRecipeLookupHandler.EitherSideChemicalRecipeLookupHandler;
import mekanism.common.recipe.lookup.cache.InputRecipeCache.EitherSideChemical;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.ConfigInfo;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.component.config.slot.ChemicalSlotInfo;
import mekanism.common.tile.component.config.slot.InventorySlotInfo;
import mekanism.common.tile.prefab.TileEntityRecipeMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TileEntityChemicalInfuser extends TileEntityRecipeMachine<ChemicalChemicalToChemicalRecipe> implements
      EitherSideChemicalRecipeLookupHandler<ChemicalChemicalToChemicalRecipe> {

    private static final List<RecipeError> TRACKED_ERROR_TYPES = List.of(
          RecipeError.NOT_ENOUGH_ENERGY,
          RecipeError.NOT_ENOUGH_ENERGY_REDUCED_RATE,
          RecipeError.NOT_ENOUGH_LEFT_INPUT,
          RecipeError.NOT_ENOUGH_RIGHT_INPUT,
          RecipeError.NOT_ENOUGH_OUTPUT_SPACE,
          RecipeError.INPUT_DOESNT_PRODUCE_OUTPUT
    );
    public static final long MAX_GAS = 10 * FluidType.BUCKET_VOLUME;

    @WrappingComputerMethod(wrapper = ComputerChemicalTankWrapper.class, methodNames = {"getLeftInput", "getLeftInputCapacity", "getLeftInputNeeded",
                                                                                        "getLeftInputFilledPercentage"}, docPlaceholder = "left input tank")
    public IChemicalTank leftTank;
    @WrappingComputerMethod(wrapper = ComputerChemicalTankWrapper.class, methodNames = {"getRightInput", "getRightInputCapacity", "getRightInputNeeded",
                                                                                        "getRightInputFilledPercentage"}, docPlaceholder = "right input tank")
    public IChemicalTank rightTank;
    @WrappingComputerMethod(wrapper = ComputerChemicalTankWrapper.class, methodNames = {"getOutput", "getOutputCapacity", "getOutputNeeded",
                                                                                        "getOutputFilledPercentage"}, docPlaceholder = "output (center) tank")
    public IChemicalTank centerTank;

    private long clientEnergyUsed = 0L;
    private int baselineMaxOperations = 1;

    private final IOutputHandler<@NotNull ChemicalStack> outputHandler;
    private final IInputHandler<@NotNull ChemicalStack> leftInputHandler;
    private final IInputHandler<@NotNull ChemicalStack> rightInputHandler;

    private MachineEnergyContainer<TileEntityChemicalInfuser> energyContainer;
    @WrappingComputerMethod(wrapper = ComputerIInventorySlotWrapper.class, methodNames = "getLeftInputItem", docPlaceholder = "left input item slot")
    ChemicalInventorySlot leftInputSlot;
    @WrappingComputerMethod(wrapper = ComputerIInventorySlotWrapper.class, methodNames = "getOutputItem", docPlaceholder = "output item slot")
    ChemicalInventorySlot outputSlot;
    @WrappingComputerMethod(wrapper = ComputerIInventorySlotWrapper.class, methodNames = "getRightInputItem", docPlaceholder = "right input item slot")
    ChemicalInventorySlot rightInputSlot;
    @WrappingComputerMethod(wrapper = ComputerIInventorySlotWrapper.class, methodNames = "getEnergyItem", docPlaceholder = "energy slot")
    EnergyInventorySlot energySlot;

    public TileEntityChemicalInfuser(BlockPos pos, BlockState state) {
        super(MekanismBlocks.CHEMICAL_INFUSER, pos, state, TRACKED_ERROR_TYPES);

        ConfigInfo itemConfig = configComponent.getConfig(TransmissionType.ITEM);
        if (itemConfig != null) {
            itemConfig.addSlotInfo(DataType.INPUT_1, new InventorySlotInfo(true, true, leftInputSlot));
            itemConfig.addSlotInfo(DataType.INPUT_2, new InventorySlotInfo(true, true, rightInputSlot));
            itemConfig.addSlotInfo(DataType.OUTPUT, new InventorySlotInfo(true, true, outputSlot));
            itemConfig.addSlotInfo(DataType.INPUT_OUTPUT, new InventorySlotInfo(true, true, leftInputSlot, rightInputSlot, outputSlot));
            itemConfig.addSlotInfo(DataType.ENERGY, new InventorySlotInfo(true, true, energySlot));
        }

        ConfigInfo gasConfig = configComponent.getConfig(TransmissionType.CHEMICAL);
        if (gasConfig != null) {
            gasConfig.addSlotInfo(DataType.INPUT_1, new ChemicalSlotInfo(true, false, leftTank));
            gasConfig.addSlotInfo(DataType.INPUT_2, new ChemicalSlotInfo(true, false, rightTank));
            gasConfig.addSlotInfo(DataType.OUTPUT, new ChemicalSlotInfo(false, true, centerTank));
            gasConfig.addSlotInfo(DataType.INPUT_OUTPUT, new ChemicalSlotInfo(true, true, leftTank, rightTank, centerTank));
        }

        configComponent.setupInputConfig(TransmissionType.ENERGY, energyContainer);

        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(configComponent, TransmissionType.ITEM, TransmissionType.CHEMICAL)
              .setCanTankEject(tank -> tank == centerTank);

        leftInputHandler = InputHelper.getInputHandler(leftTank, RecipeError.NOT_ENOUGH_LEFT_INPUT);
        rightInputHandler = InputHelper.getInputHandler(rightTank, RecipeError.NOT_ENOUGH_RIGHT_INPUT);
        outputHandler = OutputHelper.getOutputHandler(centerTank, RecipeError.NOT_ENOUGH_OUTPUT_SPACE);
    }

    @NotNull
    @Override
    public IChemicalTankHolder getInitialChemicalTanks(IContentsListener listener, IContentsListener recipeCacheListener, IContentsListener recipeCacheUnpauseListener) {
        ChemicalTankHelper builder = ChemicalTankHelper.forSideWithConfig(this::getDirection, this::getConfig);
        builder.addTank(leftTank = BasicChemicalTank.input(MAX_GAS, gas -> containsRecipe(gas, rightTank.getStack()), this::containsRecipe, recipeCacheListener));
        builder.addTank(rightTank = BasicChemicalTank.input(MAX_GAS, gas -> containsRecipe(gas, leftTank.getStack()), this::containsRecipe, recipeCacheListener));
        builder.addTank(centerTank = BasicChemicalTank.output(MAX_GAS, recipeCacheUnpauseListener));
        return builder.build();
    }

    @NotNull
    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener, IContentsListener recipeCacheListener, IContentsListener recipeCacheUnpauseListener) {
        EnergyContainerHelper builder = EnergyContainerHelper.forSideWithConfig(this::getDirection, this::getConfig);
        builder.addContainer(energyContainer = MachineEnergyContainer.input(this, recipeCacheUnpauseListener));
        return builder.build();
    }

    @NotNull
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener, IContentsListener recipeCacheListener, IContentsListener recipeCacheUnpauseListener) {
        InventorySlotHelper builder = InventorySlotHelper.forSideWithConfig(this::getDirection, this::getConfig);
        builder.addSlot(leftInputSlot = ChemicalInventorySlot.fill(leftTank, listener, 6, 56));
        builder.addSlot(rightInputSlot = ChemicalInventorySlot.fill(rightTank, listener, 154, 56));
        builder.addSlot(outputSlot = ChemicalInventorySlot.drain(centerTank, listener, 80, 65));
        builder.addSlot(energySlot = EnergyInventorySlot.fillOrConvert(energyContainer, this::getLevel, listener, 154, 14));
        leftInputSlot.setSlotType(ContainerSlotType.INPUT);
        leftInputSlot.setSlotOverlay(SlotOverlay.MINUS);
        rightInputSlot.setSlotType(ContainerSlotType.INPUT);
        rightInputSlot.setSlotOverlay(SlotOverlay.MINUS);
        outputSlot.setSlotType(ContainerSlotType.OUTPUT);
        outputSlot.setSlotOverlay(SlotOverlay.PLUS);
        return builder.build();
    }

    @Override
    protected boolean onUpdateServer() {
        boolean sendUpdatePacket = super.onUpdateServer();
        energySlot.fillContainerOrConvert();
        leftInputSlot.fillTank();
        rightInputSlot.fillTank();
        outputSlot.drainTank();
        clientEnergyUsed = recipeCacheLookupMonitor.updateAndProcess(energyContainer);
        return sendUpdatePacket;
    }

    @ComputerMethod(nameOverride = "getEnergyUsage", methodDescription = ComputerConstants.DESCRIPTION_GET_ENERGY_USAGE)
    public long getEnergyUsed() {
        return clientEnergyUsed;
    }

    @NotNull
    @Override
    public IMekanismRecipeTypeProvider<BiChemicalRecipeInput, ChemicalChemicalToChemicalRecipe, EitherSideChemical<ChemicalChemicalToChemicalRecipe>> getRecipeType() {
        return MekanismRecipeType.CHEMICAL_INFUSING;
    }

    @Override
    public IRecipeViewerRecipeType<ChemicalChemicalToChemicalRecipe> recipeViewerType() {
        return RecipeViewerRecipeType.CHEMICAL_INFUSING;
    }

    @Nullable
    @Override
    public ChemicalChemicalToChemicalRecipe getRecipe(int cacheIndex) {
        return findFirstRecipe(leftInputHandler, rightInputHandler);
    }

    @NotNull
    @Override
    public CachedRecipe<ChemicalChemicalToChemicalRecipe> createNewCachedRecipe(@NotNull ChemicalChemicalToChemicalRecipe recipe, int cacheIndex) {
        return new ChemicalChemicalToChemicalCachedRecipe<>(recipe, recheckAllRecipeErrors, leftInputHandler, rightInputHandler, outputHandler)
              .setErrorsChanged(this::onErrorsChanged)
              .setCanHolderFunction(this::canFunction)
              .setActive(this::setActive)
              .setEnergyRequirements(energyContainer::getEnergyPerTick, energyContainer)
              .setBaselineMaxOperations(() -> baselineMaxOperations)
              .setOnFinish(this::markForSave);
    }

    @Override
    public void recalculateUpgrades(Upgrade upgrade) {
        super.recalculateUpgrades(upgrade);
        if (upgrade == Upgrade.SPEED) {
            baselineMaxOperations = (int) Math.pow(2, upgradeComponent.getUpgrades(Upgrade.SPEED));
        }
    }

    public MachineEnergyContainer<TileEntityChemicalInfuser> getEnergyContainer() {
        return energyContainer;
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(SyncableLong.create(this::getEnergyUsed, value -> clientEnergyUsed = value));
    }
}
