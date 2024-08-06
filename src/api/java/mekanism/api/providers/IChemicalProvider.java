package mekanism.api.providers;

import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

@MethodsReturnNonnullByDefault
public interface IChemicalProvider extends IBaseProvider {

    /**
     * Gets the chemical this provider represents.
     */
    Chemical getChemical();

    /**
     * Creates a chemical stack of the given size using the chemical this provider represents.
     *
     * @param size Size of the stack.
     */
    default ChemicalStack getStack(long size) {
        return new ChemicalStack(getChemical(), size);
    }

    @Override
    default ResourceLocation getRegistryName() {
        return getChemical().getRegistryName();
    }

    @Override
    default Component getTextComponent() {
        return getChemical().getTextComponent();
    }

    @Override
    default String getTranslationKey() {
        return getChemical().getTranslationKey();
    }
}