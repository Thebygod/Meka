package mekanism.common;

import java.util.Locale;
import mekanism.api.EnumColor;
import mekanism.common.block.states.BlockStateMachine;
import mekanism.common.config.MekanismConfig;
import mekanism.common.util.LangUtils;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.ResourceLocation;

/**
 * Tier information for Mekanism.  This currently includes tiers for Energy Cubes and Smelting Factories.
 *
 * @author aidancbrady
 */
public final class Tier {

    /**
     * The default tiers used in Mekanism.
     *
     * @author aidancbrady
     */
    public enum BaseTier implements IStringSerializable {
        BASIC("Basic", EnumColor.BRIGHT_GREEN),
        ADVANCED("Advanced", EnumColor.DARK_RED),
        ELITE("Elite", EnumColor.DARK_BLUE),
        ULTIMATE("Ultimate", EnumColor.PURPLE),
        CREATIVE("Creative", EnumColor.BLACK);

        private String name;
        private EnumColor color;

        BaseTier(String s, EnumColor c) {
            name = s;
            color = c;
        }

        public String getSimpleName() {
            return name;
        }

        public String getLocalizedName() {
            return LangUtils.localize("tier." + getSimpleName());
        }

        public EnumColor getColor() {
            return color;
        }

        public boolean isObtainable() {
            return this != CREATIVE;
        }

        @Override
        public String getName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum EnergyCubeTier implements ITier, IStringSerializable {
        BASIC(2000000, 800),
        ADVANCED(8000000, 3200),
        ELITE(32000000, 12800),
        ULTIMATE(128000000, 51200),
        CREATIVE(Double.MAX_VALUE, Double.MAX_VALUE);

        private final double baseMaxEnergy;
        private final double baseOutput;
        private final BaseTier baseTier;

        EnergyCubeTier(double max, double out) {
            baseMaxEnergy = max;
            baseOutput = out;
            baseTier = BaseTier.values()[ordinal()];
        }

        @Override
        public BaseTier getBaseTier() {
            return baseTier;
        }

        @Override
        public String getName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public double getMaxEnergy() {
            return MekanismConfig.current().general.tiers.get(baseTier).EnergyCubeMaxEnergy.val();
        }

        public double getOutput() {
            return MekanismConfig.current().general.tiers.get(baseTier).EnergyCubeOutput.val();
        }

        public double getBaseMaxEnergy() {
            return baseMaxEnergy;
        }

        public double getBaseOutput() {
            return baseOutput;
        }
    }

    public enum InductionCellTier implements ITier {
        BASIC(1E9D),
        ADVANCED(8E9D),
        ELITE(64E9D),
        ULTIMATE(512E9D);

        private final double baseMaxEnergy;
        private final BaseTier baseTier;

        InductionCellTier(double max) {
            baseMaxEnergy = max;
            baseTier = BaseTier.values()[ordinal()];
        }

        @Override
        public BaseTier getBaseTier() {
            return baseTier;
        }

        public double getMaxEnergy() {
            return MekanismConfig.current().general.tiers.get(baseTier).InductionCellMaxEnergy.val();
        }

        public double getBaseMaxEnergy() {
            return baseMaxEnergy;
        }
    }

    public enum InductionProviderTier implements ITier {
        BASIC(64000),
        ADVANCED(512000),
        ELITE(4096000),
        ULTIMATE(32768000);

        private final double baseOutput;
        private final BaseTier baseTier;

        InductionProviderTier(double out) {
            baseOutput = out;
            baseTier = BaseTier.values()[ordinal()];
        }

        @Override
        public BaseTier getBaseTier() {
            return baseTier;
        }

        public double getOutput() {
            return MekanismConfig.current().general.tiers.get(baseTier).InductionProviderOutput.val();
        }

        public double getBaseOutput() {
            return baseOutput;
        }
    }

    public enum FactoryTier implements ITier {
        BASIC(3, new ResourceLocation("mekanism", "gui/factory/GuiBasicFactory.png"),
              BlockStateMachine.MachineType.BASIC_FACTORY),
        ADVANCED(5, new ResourceLocation("mekanism", "gui/factory/GuiAdvancedFactory.png"),
              BlockStateMachine.MachineType.ADVANCED_FACTORY),
        ELITE(7, new ResourceLocation("mekanism", "gui/factory/GuiEliteFactory.png"),
              BlockStateMachine.MachineType.ELITE_FACTORY);

        public final BlockStateMachine.MachineType machineType;
        public int processes;
        public ResourceLocation guiLocation;
        private final BaseTier baseTier;

        FactoryTier(int process, ResourceLocation gui, BlockStateMachine.MachineType machineTypeIn) {
            processes = process;
            guiLocation = gui;
            machineType = machineTypeIn;
            baseTier = BaseTier.values()[ordinal()];
        }

        @Override
        public BaseTier getBaseTier() {
            return baseTier;
        }
    }

    public enum CableTier implements ITier {
        BASIC(3200),
        ADVANCED(12800),
        ELITE(64000),
        ULTIMATE(320000);

        private final int baseCapacity;
        private final BaseTier baseTier;

        CableTier(int capacity) {
            baseCapacity = capacity;
            baseTier = BaseTier.values()[ordinal()];
        }

        public static CableTier get(BaseTier tier) {
            for (CableTier transmitter : values()) {
                if (transmitter.getBaseTier() == tier) {
                    return transmitter;
                }
            }

            return BASIC;
        }

        @Override
        public BaseTier getBaseTier() {
            return baseTier;
        }

        public int getCableCapacity() {
            return MekanismConfig.current().general.tiers.get(baseTier).CableCapacity.val();
        }

        public int getBaseCapacity() {
            return baseCapacity;
        }
    }

    public enum PipeTier implements ITier {
        BASIC(1000, 100),
        ADVANCED(4000, 400),
        ELITE(16000, 1600),
        ULTIMATE(64000, 6400);

        private final int baseCapacity;
        private final int basePull;
        private final BaseTier baseTier;

        PipeTier(int capacity, int pullAmount) {
            baseCapacity = capacity;
            basePull = pullAmount;
            baseTier = BaseTier.values()[ordinal()];
        }

        public static PipeTier get(BaseTier tier) {
            for (PipeTier transmitter : values()) {
                if (transmitter.getBaseTier() == tier) {
                    return transmitter;
                }
            }

            return BASIC;
        }

        @Override
        public BaseTier getBaseTier() {
            return baseTier;
        }

        public int getPipeCapacity() {
            return MekanismConfig.current().general.tiers.get(baseTier).PipeCapacity.val();
        }

        public int getPipePullAmount() {
            return MekanismConfig.current().general.tiers.get(baseTier).PipePullAmount.val();
        }

        public int getBaseCapacity() {
            return baseCapacity;
        }

        public int getBasePull() {
            return basePull;
        }
    }

    public enum TubeTier implements ITier {
        BASIC(256, 64),
        ADVANCED(1024, 256),
        ELITE(4096, 1024),
        ULTIMATE(16384, 4096);

        private final int baseCapacity;
        private final int basePull;
        private final BaseTier baseTier;

        TubeTier(int capacity, int pullAmount) {
            baseCapacity = capacity;
            basePull = pullAmount;
            baseTier = BaseTier.values()[ordinal()];
        }

        public static TubeTier get(BaseTier tier) {
            for (TubeTier transmitter : values()) {
                if (transmitter.getBaseTier() == tier) {
                    return transmitter;
                }
            }

            return BASIC;
        }

        @Override
        public BaseTier getBaseTier() {
            return baseTier;
        }

        public int getTubeCapacity() {
            return MekanismConfig.current().general.tiers.get(baseTier).TubeCapacity.val();
        }

        public int getTubePullAmount() {
            return MekanismConfig.current().general.tiers.get(baseTier).TubePullAmount.val();
        }

        public int getBaseCapacity() {
            return baseCapacity;
        }

        public int getBasePull() {
            return basePull;
        }
    }

    public enum TransporterTier implements ITier {
        BASIC(1, 5),
        ADVANCED(16, 10),
        ELITE(32, 20),
        ULTIMATE(64, 50);

        private final int basePull;
        private final int baseSpeed;
        private final BaseTier baseTier;

        TransporterTier(int pull, int s) {
            basePull = pull;
            baseSpeed = s;
            baseTier = BaseTier.values()[ordinal()];
        }

        public static TransporterTier get(BaseTier tier) {
            for (TransporterTier transmitter : values()) {
                if (transmitter.getBaseTier() == tier) {
                    return transmitter;
                }
            }

            return BASIC;
        }

        @Override
        public BaseTier getBaseTier() {
            return baseTier;
        }

        public int getPullAmount() {
            return MekanismConfig.current().general.tiers.get(baseTier).TransporterPullAmount.val();
        }

        public int getSpeed() {
            return MekanismConfig.current().general.tiers.get(baseTier).TransporterSpeed.val();
        }

        public int getBasePull() {
            return basePull;
        }

        public int getBaseSpeed() {
            return baseSpeed;
        }
    }

    public enum ConductorTier implements ITier {
        BASIC(5, 1, 10, new ColourRGBA(0.2, 0.2, 0.2, 1)),
        ADVANCED(5, 1, 400, new ColourRGBA(0.2, 0.2, 0.2, 1)),
        ELITE(5, 1, 8000, new ColourRGBA(0.2, 0.2, 0.2, 1)),
        ULTIMATE(5, 1, 100000, new ColourRGBA(0.2, 0.2, 0.2, 1));

        private ColourRGBA baseColour;
        private final double baseConduction;
        private final double baseHeatCapacity;
        private final double baseConductionInsulation;
        private final BaseTier baseTier;

        ConductorTier(double inversek, double inverseC, double insulationInversek, ColourRGBA colour) {
            baseConduction = inversek;
            baseHeatCapacity = inverseC;
            baseConductionInsulation = insulationInversek;

            baseColour = colour;
            baseTier = BaseTier.values()[ordinal()];
        }

        public static ConductorTier get(BaseTier tier) {
            for (ConductorTier transmitter : values()) {
                if (transmitter.getBaseTier() == tier) {
                    return transmitter;
                }
            }

            return BASIC;
        }

        @Override
        public BaseTier getBaseTier() {
            return baseTier;
        }

        public double getInverseConduction() {
            return MekanismConfig.current().general.tiers.get(baseTier).ConductorInverseConduction.val();
        }

        public double getInverseConductionInsulation() {
            return MekanismConfig.current().general.tiers.get(baseTier).ConductorConductionInsulation.val();
        }

        public double getInverseHeatCapacity() {
            return MekanismConfig.current().general.tiers.get(baseTier).ConductorHeatCapacity.val();
        }

        public ColourRGBA getBaseColour() {
            return baseColour;
        }

        public double getBaseConduction() {
            return baseConduction;
        }

        public double getBaseHeatCapacity() {
            return baseHeatCapacity;
        }

        public double getBaseConductionInsulation() {
            return baseConductionInsulation;
        }
    }

    public enum FluidTankTier implements ITier {
        BASIC(14000, 400),
        ADVANCED(28000, 800),
        ELITE(56000, 1600),
        ULTIMATE(112000, 3200),
        CREATIVE(Integer.MAX_VALUE, Integer.MAX_VALUE / 2);

        private final int baseStorage;
        private final int baseOutput;
        private final BaseTier baseTier;

        FluidTankTier(int s, int o) {
            baseStorage = s;
            baseOutput = o;
            baseTier = BaseTier.values()[ordinal()];
        }

        @Override
        public BaseTier getBaseTier() {
            return baseTier;
        }

        public int getStorage() {
            return MekanismConfig.current().general.tiers.get(baseTier).FluidTankStorage.val();
        }

        public int getOutput() {
            return MekanismConfig.current().general.tiers.get(baseTier).FluidTankOutput.val();
        }

        public int getBaseStorage() {
            return baseStorage;
        }

        public int getBaseOutput() {
            return baseOutput;
        }
    }

    public enum GasTankTier implements ITier, IStringSerializable {
        BASIC(64000, 256),
        ADVANCED(128000, 512),
        ELITE(256000, 1028),
        ULTIMATE(512000, 2056),
        CREATIVE(Integer.MAX_VALUE, Integer.MAX_VALUE / 2);

        private final int baseStorage;
        private final int baseOutput;
        private final BaseTier baseTier;

        GasTankTier(int s, int o) {
            baseStorage = s;
            baseOutput = o;
            baseTier = BaseTier.values()[ordinal()];
        }

        @Override
        public BaseTier getBaseTier() {
            return baseTier;
        }

        @Override
        public String getName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public int getStorage() {
            return MekanismConfig.current().general.tiers.get(baseTier).GasTankStorage.val();
        }

        public int getOutput() {
            return MekanismConfig.current().general.tiers.get(baseTier).GasTankOutput.val();
        }

        public int getBaseStorage() {
            return baseStorage;
        }

        public int getBaseOutput() {
            return baseOutput;
        }
    }

    public enum BinTier implements ITier {
        BASIC(4096),
        ADVANCED(8192),
        ELITE(32768),
        ULTIMATE(262144),
        CREATIVE(Integer.MAX_VALUE);

        private final int baseStorage;
        private final BaseTier baseTier;

        BinTier(int s) {
            baseStorage = s;
            baseTier = BaseTier.values()[ordinal()];
        }

        @Override
        public BaseTier getBaseTier() {
            return baseTier;
        }

        public int getStorage() {
            return MekanismConfig.current().general.tiers.get(baseTier).BinStorage.val();
        }

        public int getBaseStorage() {
            return baseStorage;
        }
    }

    public interface ITier {

        BaseTier getBaseTier();
    }
}
