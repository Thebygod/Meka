package mekanism.generators.common.registries;

import mekanism.api.chemical.Chemical;
import mekanism.common.registration.impl.DeferredChemical;
import mekanism.common.registration.impl.ChemicalDeferredRegister;
import mekanism.generators.common.GeneratorsChemicalConstants;
import mekanism.generators.common.MekanismGenerators;

public class GeneratorsChemicals {

    private GeneratorsChemicals() {
    }

    public static final ChemicalDeferredRegister CHEMICALS = new ChemicalDeferredRegister(MekanismGenerators.MODID);

    public static final DeferredChemical<Chemical> DEUTERIUM = CHEMICALS.registerGas(GeneratorsChemicalConstants.DEUTERIUM);
    public static final DeferredChemical<Chemical> TRITIUM = CHEMICALS.registerGas("tritium", 0x64FF70);
    public static final DeferredChemical<Chemical> FUSION_FUEL = CHEMICALS.registerGas("fusion_fuel", 0x7E007D);
}