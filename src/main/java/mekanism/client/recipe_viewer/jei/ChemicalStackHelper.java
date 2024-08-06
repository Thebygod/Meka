package mekanism.client.recipe_viewer.jei;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.text.TextComponentUtil;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.util.ChemicalUtil;
import mezz.jei.api.helpers.IColorHelper;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.core.HolderSet.Named;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;

public class ChemicalStackHelper implements IIngredientHelper<ChemicalStack> {

    @Nullable
    private IColorHelper colorHelper;

    void setColorHelper(IColorHelper colorHelper) {
        this.colorHelper = colorHelper;
    }

    @Override
    public String getDisplayName(ChemicalStack ingredient) {
        return TextComponentUtil.build(ingredient).getString();
    }

    @Override
    public String getUniqueId(ChemicalStack ingredient, UidContext context) {
        return "chemical:" + ingredient.getTypeRegistryName();
    }

    @Override
    public ResourceLocation getResourceLocation(ChemicalStack ingredient) {
        return ingredient.getTypeRegistryName();
    }

    @Override
    public ItemStack getCheatItemStack(ChemicalStack ingredient) {
        return ChemicalUtil.getFilledVariant(MekanismBlocks.CREATIVE_CHEMICAL_TANK, ingredient.getChemical());
    }

    @Override
    public ChemicalStack normalizeIngredient(ChemicalStack ingredient) {
        return ingredient.copyWithAmount(FluidType.BUCKET_VOLUME);
    }

    @Override
    public boolean isValidIngredient(ChemicalStack ingredient) {
        return !ingredient.isEmpty();
    }

    @Override
    public Iterable<Integer> getColors(ChemicalStack ingredient) {
        if (colorHelper == null) {
            return IIngredientHelper.super.getColors(ingredient);
        }
        Chemical chemical = ingredient.getChemical();
        return colorHelper.getColors(MekanismRenderer.getChemicalTexture(chemical), chemical.getTint(), 1);
    }

    @Override
    public IIngredientType<ChemicalStack> getIngredientType() {
        return MekanismJEI.TYPE_CHEMICAL;
    }

    @Override
    public ChemicalStack copyIngredient(ChemicalStack ingredient) {
        return ingredient.copy();
    }

    @Override
    public Stream<ResourceLocation> getTagStream(ChemicalStack ingredient) {
        return ingredient.getChemical().getTags().map(TagKey::location);
    }

    @Override
    public Optional<ResourceLocation> getTagEquivalent(Collection<ChemicalStack> stacks) {
        if (stacks.size() < 2) {
            return Optional.empty();
        }
        Set<Chemical> values = stacks.stream()
              .map(ChemicalStack::getChemical)
              .collect(Collectors.toSet());
        int expected = values.size();
        if (expected != stacks.size()) {
            //One of the chemicals is there more than once, definitely not a tag
            return Optional.empty();
        }
        return MekanismAPI.CHEMICAL_REGISTRY.getTags()
              .filter(pair -> {
                  Named<Chemical> tag = pair.getSecond();
                  return tag.size() == expected && tag.stream().allMatch(holder -> values.contains(holder.value()));
              }).map(pair -> pair.getFirst().location())
              .findFirst();
    }

    @Override
    public String getErrorInfo(ChemicalStack ingredient) {
        if (ingredient == null) {
            ingredient = ChemicalStack.EMPTY;
        }
        ToStringHelper toStringHelper = MoreObjects.toStringHelper(ChemicalStack.class);
        Chemical chemical = ingredient.getChemical();
        toStringHelper.add("Chemical", chemical.isEmptyType() ? "none" : TextComponentUtil.build(chemical).getString());
        if (!ingredient.isEmpty()) {
            toStringHelper.add("Amount", ingredient.getAmount());
        }
        return toStringHelper.toString();
    }
}