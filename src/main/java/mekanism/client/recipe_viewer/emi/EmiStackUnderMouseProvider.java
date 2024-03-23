package mekanism.client.recipe_viewer.emi;

import dev.emi.emi.api.EmiStackProvider;
import dev.emi.emi.api.neoforge.NeoForgeEmiStack;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;
import mekanism.api.chemical.ChemicalStack;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.recipe_viewer.GuiElementHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

public class EmiStackUnderMouseProvider implements EmiStackProvider<Screen> {

    @Override
    public EmiStackInteraction getStackAt(Screen screen, int x, int y) {
        if (screen instanceof GuiMekanism<?> gui) {
            return GuiElementHandler.getClickableIngredientUnderMouse(gui, x, y, (helper, ingredient) -> {
                EmiStack emiStack;
                if (ingredient instanceof ItemStack stack) {
                    emiStack = EmiStack.of(stack);
                } else if (ingredient instanceof FluidStack stack) {
                    emiStack = NeoForgeEmiStack.of(stack);
                } else if (ingredient instanceof ChemicalStack<?> stack) {
                    emiStack = ChemicalEmiStack.create(stack);
                } else {
                    return null;
                }
                return new EmiStackInteraction(emiStack);
            }).orElse(EmiStackInteraction.EMPTY);
        }
        return EmiStackInteraction.EMPTY;
    }
}