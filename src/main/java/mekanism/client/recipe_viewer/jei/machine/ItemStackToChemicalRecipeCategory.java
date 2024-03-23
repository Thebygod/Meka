package mekanism.client.recipe_viewer.jei.machine;

import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.chemical.ItemStackToChemicalRecipe;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiChemicalGauge;
import mekanism.client.gui.element.gauge.GuiGauge;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.slot.GuiSlot;
import mekanism.client.gui.element.slot.SlotType;
import mekanism.client.recipe_viewer.jei.HolderRecipeCategory;
import mekanism.client.recipe_viewer.type.IRecipeViewerRecipeType;
import mekanism.common.tile.component.config.DataType;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.NotNull;

public abstract class ItemStackToChemicalRecipeCategory<CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>,
      RECIPE extends ItemStackToChemicalRecipe<CHEMICAL, STACK>> extends HolderRecipeCategory<RECIPE> {

    protected static final String CHEMICAL_INPUT = "chemicalInput";

    private final IIngredientType<STACK> ingredientType;
    protected final GuiProgress progressBar;
    private final GuiGauge<?> output;
    private final GuiSlot input;

    protected ItemStackToChemicalRecipeCategory(IGuiHelper helper, IRecipeViewerRecipeType<RECIPE> recipeType, IIngredientType<STACK> ingredientType, boolean isConversion) {
        super(helper, recipeType);
        this.ingredientType = ingredientType;
        output = addElement(getGauge(GaugeType.STANDARD.with(DataType.OUTPUT), 131, 13));
        input = addSlot(SlotType.INPUT, 26, 36);
        progressBar = addElement(new GuiProgress(isConversion ? () -> 1 : getSimpleProgressTimer(), ProgressType.LARGE_RIGHT, this, 64, 40));
    }

    protected abstract GuiChemicalGauge<CHEMICAL, STACK, ?> getGauge(GaugeType type, int x, int y);

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, RecipeHolder<RECIPE> recipeHolder, @NotNull IFocusGroup focusGroup) {
        RECIPE recipe = recipeHolder.value();
        initItem(builder, RecipeIngredientRole.INPUT, input, recipe.getInput().getRepresentations());
        initChemical(builder, ingredientType, RecipeIngredientRole.OUTPUT, output, recipe.getOutputDefinition())
              .setSlotName(CHEMICAL_INPUT);
    }
}