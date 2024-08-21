package mekanism.common.integration.framedblocks;

import mekanism.api.chemical.Chemical;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.api.camo.CamoContainer;
import xfacthd.framedblocks.api.camo.CamoContainerFactory;

final class ChemicalCamoContainer extends CamoContainer<ChemicalCamoContent, ChemicalCamoContainer> {

    ChemicalCamoContainer(Chemical chemical) {
        super(new ChemicalCamoContent(chemical));
    }

    Chemical getChemical() {
        return content.getChemical();
    }

    @Override
    public boolean canRotateCamo() {
        return false;
    }

    @Override
    @Nullable
    public ChemicalCamoContainer rotateCamo() {
        return null;
    }

    @Override
    public int hashCode() {
        return content.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != ChemicalCamoContainer.class) return false;
        return content.equals(((ChemicalCamoContainer) obj).content);
    }

    @Override
    public String toString() {
        return "ChemicalCamoContainer{" + content + "}";
    }

    @Override
    public CamoContainerFactory<ChemicalCamoContainer> getFactory() {
        return FramedBlocksIntegration.CHEMICAL_FACTORY.get();
    }
}
