package mekanism.common.content.entangloporter;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.Coord4D;
import mekanism.api.NBTConstants;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.ChemicalTankBuilder;
import mekanism.api.chemical.ChemicalUtils;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.gas.IGasTank;
import mekanism.api.chemical.infuse.IInfusionTank;
import mekanism.api.chemical.pigment.IPigmentTank;
import mekanism.api.chemical.slurry.ISlurryTank;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.energy.IMekanismStrictEnergyHandler;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.api.fluid.IMekanismFluidHandler;
import mekanism.api.heat.HeatAPI;
import mekanism.api.heat.IHeatCapacitor;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.inventory.IMekanismInventory;
import mekanism.api.math.FloatingLong;
import mekanism.common.capabilities.chemical.dynamic.IGasTracker;
import mekanism.common.capabilities.chemical.dynamic.IInfusionTracker;
import mekanism.common.capabilities.chemical.dynamic.IPigmentTracker;
import mekanism.common.capabilities.chemical.dynamic.ISlurryTracker;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
import mekanism.common.capabilities.fluid.BasicFluidTank;
import mekanism.common.capabilities.heat.BasicHeatCapacitor;
import mekanism.common.capabilities.heat.ITileHeatHandler;
import mekanism.common.config.MekanismConfig;
import mekanism.common.content.network.distribution.ChemicalHandlerTarget;
import mekanism.common.content.network.distribution.EnergyAcceptorTarget;
import mekanism.common.content.network.distribution.FluidHandlerTarget;
import mekanism.common.integration.energy.EnergyCompatUtils;
import mekanism.common.inventory.slot.EntangloporterInventorySlot;
import mekanism.common.lib.frequency.Frequency;
import mekanism.common.lib.frequency.FrequencyType;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.TileEntityQuantumEntangloporter;
import mekanism.common.tile.component.config.ConfigInfo;
import mekanism.common.util.ChemicalUtil;
import mekanism.common.util.EmitUtils;
import mekanism.common.util.EnumUtils;
import mekanism.common.util.FluidUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InventoryFrequency extends Frequency implements IMekanismInventory, IMekanismFluidHandler, IMekanismStrictEnergyHandler, ITileHeatHandler, IGasTracker,
      IInfusionTracker, IPigmentTracker, ISlurryTracker {

    private final Map<Coord4D, TileEntityQuantumEntangloporter> activeQEs = new Object2ObjectOpenHashMap<>();
    private long lastEject = -1;

    private BasicFluidTank storedFluid;
    private IGasTank storedGas;
    private IInfusionTank storedInfusion;
    private IPigmentTank storedPigment;
    private ISlurryTank storedSlurry;
    private IInventorySlot storedItem;
    public IEnergyContainer storedEnergy;
    private BasicHeatCapacitor storedHeat;

    private List<IInventorySlot> inventorySlots;
    private List<IGasTank> gasTanks;
    private List<IInfusionTank> infusionTanks;
    private List<IPigmentTank> pigmentTanks;
    private List<ISlurryTank> slurryTanks;
    private List<IExtendedFluidTank> fluidTanks;
    private List<IEnergyContainer> energyContainers;
    private List<IHeatCapacitor> heatCapacitors;

    /**
     * @param uuid Should only be null if we have incomplete data that we are loading
     */
    public InventoryFrequency(String n, @Nullable UUID uuid) {
        super(FrequencyType.INVENTORY, n, uuid);
        presetVariables();
    }

    public InventoryFrequency() {
        super(FrequencyType.INVENTORY);
        presetVariables();
    }

    private void presetVariables() {
        fluidTanks = Collections.singletonList(storedFluid = BasicFluidTank.create(MekanismConfig.general.entangloporterFluidBuffer.get(), this));
        gasTanks = Collections.singletonList(storedGas = ChemicalTankBuilder.GAS.create(MekanismConfig.general.entangloporterChemicalBuffer.get(), this));
        infusionTanks = Collections.singletonList(storedInfusion = ChemicalTankBuilder.INFUSION.create(MekanismConfig.general.entangloporterChemicalBuffer.get(), this));
        pigmentTanks = Collections.singletonList(storedPigment = ChemicalTankBuilder.PIGMENT.create(MekanismConfig.general.entangloporterChemicalBuffer.get(), this));
        slurryTanks = Collections.singletonList(storedSlurry = ChemicalTankBuilder.SLURRY.create(MekanismConfig.general.entangloporterChemicalBuffer.get(), this));
        inventorySlots = Collections.singletonList(storedItem = EntangloporterInventorySlot.create(this));
        energyContainers = Collections.singletonList(storedEnergy = BasicEnergyContainer.create(MekanismConfig.general.entangloporterEnergyBuffer.get(), this));
        heatCapacitors = Collections.singletonList(storedHeat = BasicHeatCapacitor.create(HeatAPI.DEFAULT_HEAT_CAPACITY, HeatAPI.DEFAULT_INVERSE_CONDUCTION,
              1_000, null, this));
    }

    @Override
    public void write(CompoundTag nbtTags) {
        super.write(nbtTags);
        nbtTags.put(NBTConstants.ENERGY_STORED, storedEnergy.serializeNBT());
        nbtTags.put(NBTConstants.FLUID_STORED, storedFluid.serializeNBT());
        nbtTags.put(NBTConstants.GAS_STORED, storedGas.serializeNBT());
        nbtTags.put(NBTConstants.INFUSE_TYPE_STORED, storedInfusion.serializeNBT());
        nbtTags.put(NBTConstants.PIGMENT_STORED, storedPigment.serializeNBT());
        nbtTags.put(NBTConstants.SLURRY_STORED, storedSlurry.serializeNBT());
        nbtTags.put(NBTConstants.ITEM, storedItem.serializeNBT());
        nbtTags.put(NBTConstants.HEAT_STORED, storedHeat.serializeNBT());
    }

    @Override
    protected void read(CompoundTag nbtTags) {
        super.read(nbtTags);
        storedEnergy.deserializeNBT(nbtTags.getCompound(NBTConstants.ENERGY_STORED));
        storedFluid.deserializeNBT(nbtTags.getCompound(NBTConstants.FLUID_STORED));
        storedGas.deserializeNBT(nbtTags.getCompound(NBTConstants.GAS_STORED));
        storedInfusion.deserializeNBT(nbtTags.getCompound(NBTConstants.INFUSE_TYPE_STORED));
        storedPigment.deserializeNBT(nbtTags.getCompound(NBTConstants.PIGMENT_STORED));
        storedSlurry.deserializeNBT(nbtTags.getCompound(NBTConstants.SLURRY_STORED));
        storedItem.deserializeNBT(nbtTags.getCompound(NBTConstants.ITEM));
        storedHeat.deserializeNBT(nbtTags.getCompound(NBTConstants.HEAT_STORED));
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        super.write(buffer);
        storedEnergy.getEnergy().writeToBuffer(buffer);
        buffer.writeFluidStack(storedFluid.getFluid());
        ChemicalUtils.writeChemicalStack(buffer, storedGas.getStack());
        ChemicalUtils.writeChemicalStack(buffer, storedInfusion.getStack());
        ChemicalUtils.writeChemicalStack(buffer, storedPigment.getStack());
        ChemicalUtils.writeChemicalStack(buffer, storedSlurry.getStack());
        buffer.writeNbt(storedItem.serializeNBT());
        buffer.writeDouble(storedHeat.getHeat());
    }

    @Override
    protected void read(FriendlyByteBuf dataStream) {
        super.read(dataStream);
        presetVariables();
        storedEnergy.setEnergy(FloatingLong.readFromBuffer(dataStream));
        storedFluid.setStack(dataStream.readFluidStack());
        storedGas.setStack(ChemicalUtils.readGasStack(dataStream));
        storedInfusion.setStack(ChemicalUtils.readInfusionStack(dataStream));
        storedPigment.setStack(ChemicalUtils.readPigmentStack(dataStream));
        storedSlurry.setStack(ChemicalUtils.readSlurryStack(dataStream));
        storedItem.deserializeNBT(dataStream.readNbt());
        storedHeat.setHeat(dataStream.readDouble());
    }

    @NotNull
    @Override
    public List<IInventorySlot> getInventorySlots(@Nullable Direction side) {
        return inventorySlots;
    }

    @NotNull
    @Override
    public List<IGasTank> getGasTanks(@Nullable Direction side) {
        return gasTanks;
    }

    @NotNull
    @Override
    public List<IInfusionTank> getInfusionTanks(@Nullable Direction side) {
        return infusionTanks;
    }

    @NotNull
    @Override
    public List<IPigmentTank> getPigmentTanks(@Nullable Direction side) {
        return pigmentTanks;
    }

    @NotNull
    @Override
    public List<ISlurryTank> getSlurryTanks(@Nullable Direction side) {
        return slurryTanks;
    }

    @NotNull
    @Override
    public List<IExtendedFluidTank> getFluidTanks(@Nullable Direction side) {
        return fluidTanks;
    }

    @NotNull
    @Override
    public List<IEnergyContainer> getEnergyContainers(@Nullable Direction side) {
        return energyContainers;
    }

    @NotNull
    @Override
    public List<IHeatCapacitor> getHeatCapacitors(@Nullable Direction side) {
        return heatCapacitors;
    }

    @Override
    public void onContentsChanged() {
        dirty = true;
    }

    @Override
    public boolean update(BlockEntity tile) {
        boolean changedData = super.update(tile);
        if (tile instanceof TileEntityQuantumEntangloporter entangloporter) {
            //This should always be the case, but validate it and remove if it isn't
            activeQEs.put(entangloporter.getTileCoord(), entangloporter);
        } else {
            activeQEs.remove(new Coord4D(tile));
        }
        return changedData;
    }

    @Override
    public boolean onDeactivate(BlockEntity tile) {
        boolean changedData = super.onDeactivate(tile);
        activeQEs.remove(new Coord4D(tile));
        return changedData;
    }

    public void handleEject(long gameTime) {
        if (isValid() && !activeQEs.isEmpty() && lastEject != gameTime) {
            lastEject = gameTime;
            Map<TransmissionType, CapabilityHandler> typesToEject = new EnumMap<>(TransmissionType.class);
            //All but heat and item
            List<Runnable> transferHandlers = new ArrayList<>(EnumUtils.TRANSMISSION_TYPES.length - 2);
            int expected = 6 * activeQEs.size();
            addEnergyTransferHandler(typesToEject, transferHandlers, expected);
            addFluidTransferHandler(typesToEject, transferHandlers, expected);
            addChemicalTransferHandler(TransmissionType.GAS, storedGas, typesToEject, transferHandlers, expected);
            addChemicalTransferHandler(TransmissionType.INFUSION, storedInfusion, typesToEject, transferHandlers, expected);
            addChemicalTransferHandler(TransmissionType.PIGMENT, storedPigment, typesToEject, transferHandlers, expected);
            addChemicalTransferHandler(TransmissionType.SLURRY, storedSlurry, typesToEject, transferHandlers, expected);
            if (!typesToEject.isEmpty()) {
                //If we have at least one type to eject (we are not entirely empty)
                // then go through all the QEs and build up the target locations
                for (TileEntityQuantumEntangloporter qe : activeQEs.values()) {
                    if (!MekanismUtils.canFunction(qe)) {
                        //Skip trying to eject for this QE if it can't function
                        continue;
                    }
                    Map<Direction, CachedBlockInfo> adjacentBlocks = null;
                    for (Map.Entry<TransmissionType, CapabilityHandler> entry : typesToEject.entrySet()) {
                        TransmissionType transmissionType = entry.getKey();
                        ConfigInfo config = qe.getConfig().getConfig(transmissionType);
                        //Validate the ejector for the config allows ejecting this transmission type. In theory, we already check all
                        // of this except config#isEjecting before we get here, but we do so anyway for consistency
                        if (config != null && qe.getEjector().isEjecting(config, transmissionType)) {
                            Set<Direction> outputSides = config.getAllOutputtingSides();
                            if (!outputSides.isEmpty()) {
                                if (adjacentBlocks == null) {
                                    //Lazy init the map of adjacent tiles
                                    adjacentBlocks = new EnumMap<>(Direction.class);
                                }
                                Level level = qe.getLevel();
                                for (Direction side : outputSides) {
                                    CachedBlockInfo blockInfo;
                                    if (adjacentBlocks.containsKey(side)) {
                                        //Need to use contains because we allow for null values
                                        blockInfo = adjacentBlocks.get(side);
                                    } else {
                                        //Get the block and tile and provide if not null and the block is loaded, prevents ghost chunk loading
                                        BlockPos pos = qe.getBlockPos().relative(side);
                                        blockInfo = WorldUtils.getBlockState(level, pos)
                                              .map(state -> new CachedBlockInfo(pos, state, WorldUtils.getTileEntity(level, pos)))
                                              .orElse(null);
                                        adjacentBlocks.put(side, blockInfo);
                                    }
                                    if (blockInfo != null) {
                                        entry.getValue().handle(level, blockInfo.pos(), blockInfo.state(), blockInfo.blockEntity(), side);
                                    }
                                }
                            }
                        }
                    }
                }
                //Run all our transfer handlers that we have
                for (Runnable transferHandler : transferHandlers) {
                    transferHandler.run();
                }
            }
        }
    }

    private void addEnergyTransferHandler(Map<TransmissionType, CapabilityHandler> typesToEject, List<Runnable> transferHandlers, int expected) {
        FloatingLong toSend = storedEnergy.extract(storedEnergy.getMaxEnergy(), Action.SIMULATE, AutomationType.INTERNAL);
        if (!toSend.isZero()) {
            EnergyAcceptorTarget target = new EnergyAcceptorTarget(expected);
            typesToEject.put(TransmissionType.ENERGY, (level, pos, state, blockEntity, side) -> {
                IStrictEnergyHandler handler = EnergyCompatUtils.getStrictEnergyHandler(level, pos, side.getOpposite());
                if (handler != null) {
                    target.addHandler(handler);
                }
            });
            transferHandlers.add(() -> {
                if (target.getHandlerCount() > 0) {
                    storedEnergy.extract(EmitUtils.sendToAcceptors(target, toSend), Action.EXECUTE, AutomationType.INTERNAL);
                }
            });
        }
    }

    private void addFluidTransferHandler(Map<TransmissionType, CapabilityHandler> typesToEject, List<Runnable> transferHandlers, int expected) {
        FluidStack fluidToSend = storedFluid.extract(storedFluid.getCapacity(), Action.SIMULATE, AutomationType.INTERNAL);
        if (!fluidToSend.isEmpty()) {
            FluidHandlerTarget target = new FluidHandlerTarget(fluidToSend, expected);
            typesToEject.put(TransmissionType.FLUID, (level, pos, state, blockEntity, side) -> {
                IFluidHandler handler = WorldUtils.getCapability(level, FluidHandler.BLOCK, pos, state, blockEntity, side.getOpposite());
                if (handler != null && FluidUtils.canFill(handler, fluidToSend)) {
                    target.addHandler(handler);
                }
            });
            transferHandlers.add(() -> {
                if (target.getHandlerCount() > 0) {
                    storedFluid.extract(EmitUtils.sendToAcceptors(target, fluidToSend.getAmount(), fluidToSend), Action.EXECUTE, AutomationType.INTERNAL);
                }
            });
        }
    }

    private <CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>> void addChemicalTransferHandler(TransmissionType chemicalType,
          IChemicalTank<CHEMICAL, STACK> tank, Map<TransmissionType, CapabilityHandler> typesToEject, List<Runnable> transferHandlers, int expected) {
        STACK toSend = tank.extract(tank.getCapacity(), Action.SIMULATE, AutomationType.INTERNAL);
        if (!toSend.isEmpty()) {
            BlockCapability<IChemicalHandler<CHEMICAL, STACK>, @Nullable Direction> capability = ChemicalUtil.getCapabilityForChemical(toSend).block();
            ChemicalHandlerTarget<CHEMICAL, STACK, IChemicalHandler<CHEMICAL, STACK>> target = new ChemicalHandlerTarget<>(toSend, expected);
            typesToEject.put(chemicalType, (level, pos, state, blockEntity, side) -> {
                IChemicalHandler<CHEMICAL, STACK> handler = WorldUtils.getCapability(level, capability, pos, state, blockEntity, side.getOpposite());
                if (handler != null && ChemicalUtil.canInsert(handler, toSend)) {
                    target.addHandler(handler);
                }
            });
            transferHandlers.add(() -> {
                if (target.getHandlerCount() > 0) {
                    tank.extract(EmitUtils.sendToAcceptors(target, toSend.getAmount(), toSend), Action.EXECUTE, AutomationType.INTERNAL);
                }
            });
        }
    }

    //TODO - 1.20.2: Better name
    private interface CapabilityHandler {

        void handle(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, Direction side);
    }

    private record CachedBlockInfo(BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
    }
}