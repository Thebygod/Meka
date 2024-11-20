package mekanism.api.chemical.attribute;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import mekanism.api.chemical.Chemical;
import mekanism.api.math.MathUtils;
import mekanism.api.providers.IChemicalProvider;
import mekanism.api.radiation.IRadiationManager;
import mekanism.api.text.APILang;
import mekanism.api.text.EnumColor;
import mekanism.api.text.ITooltipHelper;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Range;

/**
 * @since 10.7.0 Previously was GasAttributes
 */
public class ChemicalAttributes {

    private ChemicalAttributes() {
    }

    /**
     * This defines the radioactivity of a certain chemical. This attribute <i>requires validation</i>, meaning chemical containers won't be able to accept chemicals with
     * this attribute by default. Radioactivity is measured in Sv/h.
     *
     * @author aidancbrady
     */
    public static class Radiation extends ChemicalAttribute {

        private final double radioactivity;

        /**
         * @param radioactivity Radioactivity of the chemical measured in Sv/h, must be greater than zero.
         */
        public Radiation(double radioactivity) {
            if (radioactivity <= 0) {
                throw new IllegalArgumentException("Radiation attribute should only be used when there actually is radiation! Radioactivity: " + radioactivity);
            }
            this.radioactivity = radioactivity;
        }

        /**
         * Gets the radioactivity of this chemical in Sv/h. Each mB of this chemical released into the environment will cause a radiation source of the given
         * radioactivity.
         *
         * @return the radioactivity of this chemical
         */
        public double getRadioactivity() {
            return radioactivity;
        }

        @Override
        public boolean needsValidation() {
            //This attribute only actually needs validation if radiation is enabled
            return IRadiationManager.INSTANCE.isRadiationEnabled();
        }

        @Override
        @Deprecated(since = "10.7.4", forRemoval = true)
        public List<Component> addTooltipText(List<Component> list) {
            collectTooltips(list::add);
            return list;
        }

        @Override
        public void collectTooltips(Consumer<Component> adder) {
            if (needsValidation()) {
                //Only show the radioactive tooltip information if radiation is actually enabled
                ITooltipHelper tooltipHelper = ITooltipHelper.INSTANCE;
                adder.accept(APILang.CHEMICAL_ATTRIBUTE_RADIATION.translateColored(EnumColor.GRAY, EnumColor.INDIGO, tooltipHelper.getRadioactivityDisplayShort(getRadioactivity())));
            }
        }
    }

    /**
     * Defines the root data of a coolant, for use in Fission Reactors. Coolants have two primary properties: 'thermal enthalpy' and 'conductivity'. Thermal Enthalpy
     * defines how much energy one mB of the chemical can store; as such, lower values will cause reactors to require more of the chemical to stay cool. 'Conductivity'
     * defines the proportion of a reactor's available heat that can be used at an instant to convert this coolant's cool variant to its heated variant.
     *
     * @author aidancbrady
     */
    public abstract static sealed class Coolant extends ChemicalAttribute permits CooledCoolant, HeatedCoolant {

        private final double thermalEnthalpy;
        private final double conductivity;

        /**
         * @param thermalEnthalpy Defines how much energy one mB of the chemical can store; lower values will cause reactors to require more of the chemical to stay cool.
         *                        Must be greater than zero.
         * @param conductivity    Defines the proportion of a reactor's available heat that can be used at an instant to convert this coolant's cool variant to its heated
         *                        variant. This value should be greater than zero, and at most one.
         */
        private Coolant(double thermalEnthalpy, double conductivity) {
            if (thermalEnthalpy <= 0) {
                throw new IllegalArgumentException("Coolant attributes must have a thermal enthalpy greater than zero! Thermal Enthalpy: " + thermalEnthalpy);
            }
            if (conductivity <= 0 || conductivity > 1) {
                throw new IllegalArgumentException("Coolant attributes must have a conductivity greater than zero and at most one! Conductivity: " + conductivity);
            }
            this.thermalEnthalpy = thermalEnthalpy;
            this.conductivity = conductivity;
        }

        /**
         * Gets the thermal enthalpy of this coolant. Thermal Enthalpy defines how much energy one mB of the chemical can store.
         */
        public double getThermalEnthalpy() {
            return thermalEnthalpy;
        }

        /**
         * Gets the conductivity of this coolant. 'Conductivity' defines the proportion of a reactor's available heat that can be used at an instant to convert this
         * coolant's cool variant to its heated variant.
         */
        public double getConductivity() {
            return conductivity;
        }

        @Override
        @Deprecated(since = "10.7.4", forRemoval = true)
        public List<Component> addTooltipText(List<Component> list) {
            collectTooltips(list::add);
            return list;
        }

        @Override
        public void collectTooltips(Consumer<Component> adder) {
            ITooltipHelper tooltipHelper = ITooltipHelper.INSTANCE;
            adder.accept(APILang.CHEMICAL_ATTRIBUTE_COOLANT_EFFICIENCY.translateColored(EnumColor.GRAY, EnumColor.INDIGO, tooltipHelper.getPercent(conductivity)));
            adder.accept(APILang.CHEMICAL_ATTRIBUTE_COOLANT_ENTHALPY.translateColored(EnumColor.GRAY, EnumColor.INDIGO,
                  tooltipHelper.getEnergyPerMBDisplayShort(MathUtils.clampToLong(thermalEnthalpy))));
        }
    }

    /**
     * Defines the 'cooled' variant of a coolant - the heated variant must be supplied in this class.
     *
     * @author aidancbrady
     */
    public static non-sealed class CooledCoolant extends Coolant {

        private final IChemicalProvider heatedChemical;

        /**
         * @param heatedChemical  Chemical provider for the heated variant of this chemical.
         * @param thermalEnthalpy Defines how much energy one mB of the chemical can store; lower values will cause reactors to require more of the chemical to stay cool.
         *                        Must be greater than zero.
         * @param conductivity    Defines the proportion of a reactor's available heat that can be used at an instant to convert this coolant's cool variant to its heated
         *                        variant. This value should be greater than zero, and at most one.
         */
        public CooledCoolant(IChemicalProvider heatedChemical, double thermalEnthalpy, double conductivity) {
            super(thermalEnthalpy, conductivity);
            this.heatedChemical = heatedChemical;
        }

        /**
         * Gets the heated version of this coolant.
         */
        public Chemical getHeatedChemical() {
            return heatedChemical.getChemical();
        }
    }

    /**
     * Defines the 'heated' variant of a coolant - the cooled variant must be supplied in this class.
     *
     * @author aidancbrady
     */
    public static non-sealed class HeatedCoolant extends Coolant {

        private final IChemicalProvider cooledChemical;

        /**
         * @param cooledChemical  Chemical provider for the cooled variant of this chemical.
         * @param thermalEnthalpy Defines how much energy one mB of the chemical can store; lower values will cause reactors to require more of the chemical to stay cool.
         *                        Must be greater than zero.
         * @param conductivity    Defines the proportion of a reactor's available heat that can be used at an instant to convert this coolant's cool variant to its heated
         *                        variant. This value should be greater than zero, and at most one.
         */
        public HeatedCoolant(IChemicalProvider cooledChemical, double thermalEnthalpy, double conductivity) {
            super(thermalEnthalpy, conductivity);
            this.cooledChemical = cooledChemical;
        }

        /**
         * Gets the cooled version of this coolant.
         */
        public Chemical getCooledChemical() {
            return cooledChemical.getChemical();
        }
    }

    /**
     * Defines a fuel which can be processed by a Gas-Burning Generator to produce energy.
     * Fuels have two primary values:
     * - 'max burn per tick', defining how many mB per tick can be burnt (max amount burned when tank is full)
     * - 'energy density', defining how much energy is stored in one mB of fuel.
     *
     * @author aidancbrady
     */
    public static class Fuel extends ChemicalAttribute {

        private final IntSupplier maxBurnPerTick;
        private final LongSupplier energyDensity;

        /**
         * @param maxBurnPerTick how many mB per tick can be burnt; must be greater than zero.
         * @param energyDensity  The energy density in one mB of fuel; must be greater than zero.
         *
         * @since 10.7.8
         */
        public Fuel(int maxBurnPerTick, long energyDensity) {
            if (maxBurnPerTick <= 0) {
                throw new IllegalArgumentException("Fuel attributes must be able to burn at least 1 per tick. maxBurnPerTick: " + maxBurnPerTick);
            } else if (energyDensity <= 0) {
                throw new IllegalArgumentException("Fuel attributes must have an energy density greater than zero!");
            }
            this.maxBurnPerTick = () -> maxBurnPerTick;
            this.energyDensity = () -> energyDensity;
        }

        /**
         * @param maxBurnPerTick Supplier for how many mB per tick can be burnt. The supplier should return values greater than zero.
         * @param energyDensity  Supplier for the energy density of one mB of fuel. The supplier should return values be greater than zero.
         */
        public Fuel(IntSupplier maxBurnPerTick, LongSupplier energyDensity) {
            this.maxBurnPerTick = maxBurnPerTick;
            this.energyDensity = energyDensity;
        }

        /**
         * Gets the number of ticks this fuel burns for.
         */
        @Range(from = 1, to = Integer.MAX_VALUE)
        public int getMaxBurnPerTick() {
            return Math.max(maxBurnPerTick.getAsInt(), 1);
        }

        /**
         * Gets the amount of energy produced per tick of this fuel.
         */
        @Range(from = 1, to = Integer.MAX_VALUE)
        public long getEnergyDensity() {
            return Math.max(energyDensity.getAsLong(), 1);
        }

        @Override
        @Deprecated(since = "10.7.4", forRemoval = true)
        public List<Component> addTooltipText(List<Component> list) {
            collectTooltips(list::add);
            return list;
        }

        @Override
        public void collectTooltips(Consumer<Component> adder) {
            ITooltipHelper tooltipHelper = ITooltipHelper.INSTANCE;
            adder.accept(APILang.CHEMICAL_ATTRIBUTE_FUEL_MAX_BURN.translateColored(EnumColor.GRAY, EnumColor.INDIGO, tooltipHelper.getFluidDisplay(getMaxBurnPerTick(), true)));
            adder.accept(APILang.CHEMICAL_ATTRIBUTE_FUEL_ENERGY_DENSITY.translateColored(EnumColor.GRAY, EnumColor.INDIGO,
                  tooltipHelper.getEnergyPerMBDisplayShort(energyDensity.getAsLong())));
            adder.accept(APILang.CHEMICAL_ATTRIBUTE_FUEL_ENERGY_MAX_TOTAL.translateColored(EnumColor.GRAY, EnumColor.INDIGO,
                  tooltipHelper.getEnergyDisplay(getMaxJoulesPerTick(), true)));
        }

        public long getMaxJoulesPerTick() {
            return getMaxBurnPerTick() * energyDensity.getAsLong();
        }
    }
}
