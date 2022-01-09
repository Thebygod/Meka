package mekanism.common.integration.crafttweaker.recipe.handler;

import com.blamejared.crafttweaker.api.fluid.MCFluidStack;
import com.blamejared.crafttweaker.api.ingredient.IIngredient;
import com.blamejared.crafttweaker.api.recipe.handler.IRecipeHandler;
import com.blamejared.crafttweaker.api.recipe.manager.base.IRecipeManager;
import com.blamejared.crafttweaker.api.tag.MCTag;
import com.blamejared.crafttweaker.api.tag.manager.TagManagerFluid;
import com.blamejared.crafttweaker.api.tag.manager.TagManagerItem;
import com.blamejared.crafttweaker.api.util.ItemStackUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import javax.annotation.Nullable;
import mekanism.api.JsonConstants;
import mekanism.api.SerializerHelper;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.ChemicalType;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.infuse.InfusionStack;
import mekanism.api.chemical.merged.BoxedChemicalStack;
import mekanism.api.chemical.pigment.PigmentStack;
import mekanism.api.chemical.slurry.SlurryStack;
import mekanism.api.math.FloatingLong;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.api.recipes.inputs.FluidStackIngredient;
import mekanism.api.recipes.inputs.InputIngredient;
import mekanism.api.recipes.inputs.ItemStackIngredient;
import mekanism.api.recipes.inputs.chemical.ChemicalIngredientDeserializer;
import mekanism.api.recipes.inputs.chemical.ChemicalStackIngredient;
import mekanism.api.recipes.inputs.chemical.GasStackIngredient;
import mekanism.api.recipes.inputs.chemical.InfusionStackIngredient;
import mekanism.api.recipes.inputs.chemical.PigmentStackIngredient;
import mekanism.api.recipes.inputs.chemical.SlurryStackIngredient;
import mekanism.common.integration.crafttweaker.CrTConstants;
import mekanism.common.integration.crafttweaker.chemical.CrTChemicalStack.CrTGasStack;
import mekanism.common.integration.crafttweaker.chemical.CrTChemicalStack.CrTInfusionStack;
import mekanism.common.integration.crafttweaker.chemical.CrTChemicalStack.CrTPigmentStack;
import mekanism.common.integration.crafttweaker.chemical.CrTChemicalStack.CrTSlurryStack;
import mekanism.common.integration.crafttweaker.tag.CrTChemicalTagManager;
import mekanism.common.integration.crafttweaker.tag.CrTGasTagManager;
import mekanism.common.integration.crafttweaker.tag.CrTInfuseTypeTagManager;
import mekanism.common.integration.crafttweaker.tag.CrTPigmentTagManager;
import mekanism.common.integration.crafttweaker.tag.CrTSlurryTagManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.NBTIngredient;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

//TODO: Eventually we might want to try and support replacing but for now it isn't worth it
public abstract class MekanismRecipeHandler<RECIPE extends MekanismRecipe> implements IRecipeHandler<RECIPE> {

    protected static final Object SKIP_OPTIONAL_PARAM = new Object();

    @Override
    public abstract <U extends Recipe<?>> boolean doesConflict(final IRecipeManager manager, final RECIPE recipe, final U other);

    protected <TYPE, INGREDIENT extends InputIngredient<TYPE>> boolean ingredientConflicts(INGREDIENT a, INGREDIENT b) {
        return a.getRepresentations().stream().anyMatch(b::testType);
    }

    @SuppressWarnings("unchecked")
    protected <CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>> boolean chemicalIngredientConflicts(ChemicalStackIngredient<CHEMICAL, STACK> a,
          ChemicalStackIngredient<?, ?> b) {
        //If types of inputs match then check if they conflict
        return ChemicalType.getTypeFor(a) == ChemicalType.getTypeFor(b) && ingredientConflicts(a, (ChemicalStackIngredient<CHEMICAL, STACK>) b);
    }

    protected String buildCommandString(IRecipeManager manager, RECIPE recipe, Object... params) {
        return buildCommandString(manager, "addRecipe", recipe, params);
    }

    protected String buildCommandString(IRecipeManager manager, String method, RECIPE recipe, Object... params) {
        StringBuilder commandString = new StringBuilder(manager.getCommandString())
              .append('.')
              .append(method)
              .append("(\"")
              //Note: Uses path rather than entire location as we only allow adding recipes to the CrT namespace
              .append(recipe.getId().getPath())
              .append('"');
        for (Object param : params) {
            if (param != SKIP_OPTIONAL_PARAM) {
                commandString.append(", ")
                      .append(convertParam(param));
            }
        }
        return commandString.append(");").toString();
    }

    /**
     * Super simplified/watered down version of BaseCrTExampleProvider#getConversionRepresentations
     */
    private String convertParam(Object param) {
        if (param instanceof ItemStack stack) {
            return ItemStackUtil.getCommandString(stack);
        } else if (param instanceof FluidStack stack) {
            return new MCFluidStack(stack).getCommandString();
        } else if (param instanceof GasStack stack) {
            return new CrTGasStack(stack).getCommandString();
        } else if (param instanceof InfusionStack stack) {
            return new CrTInfusionStack(stack).getCommandString();
        } else if (param instanceof PigmentStack stack) {
            return new CrTPigmentStack(stack).getCommandString();
        } else if (param instanceof SlurryStack stack) {
            return new CrTSlurryStack(stack).getCommandString();
        } else if (param instanceof BoxedChemicalStack stack) {
            return convertParam(stack.getChemicalStack());
        } else if (param instanceof FloatingLong fl) {
            //Note: Handled via implicit casts
            if (fl.getDecimal() == 0) {
                //No decimal, don't bother printing it
                return fl.toString(0);
            }
            //Trim any trailing zeros rather than printing them out
            return fl.toString().replaceAll("0*$", "");
        } else if (param instanceof Number || param instanceof Boolean) {//Handle integers and the like
            return param.toString();
        } else if (param instanceof ItemStackIngredient ingredient) {
            return convertIngredient(ingredient);
        } else if (param instanceof FluidStackIngredient ingredient) {
            return convertIngredient(ingredient);
        } else if (param instanceof GasStackIngredient ingredient) {
            return convertIngredient(CrTConstants.CLASS_GAS_STACK_INGREDIENT, CrTGasTagManager.INSTANCE, ChemicalIngredientDeserializer.GAS, ingredient);
        } else if (param instanceof InfusionStackIngredient ingredient) {
            return convertIngredient(CrTConstants.CLASS_INFUSION_STACK_INGREDIENT, CrTInfuseTypeTagManager.INSTANCE, ChemicalIngredientDeserializer.INFUSION, ingredient);
        } else if (param instanceof PigmentStackIngredient ingredient) {
            return convertIngredient(CrTConstants.CLASS_PIGMENT_STACK_INGREDIENT, CrTPigmentTagManager.INSTANCE, ChemicalIngredientDeserializer.PIGMENT, ingredient);
        } else if (param instanceof SlurryStackIngredient ingredient) {
            return convertIngredient(CrTConstants.CLASS_SLURRY_STACK_INGREDIENT, CrTSlurryTagManager.INSTANCE, ChemicalIngredientDeserializer.SLURRY, ingredient);
        } else if (param instanceof List<?> list) {
            if (list.isEmpty()) {
                //Shouldn't happen
                return "Invalid (output) list, no outputs";
            }
            //Outputs sometimes are as lists, try wrapping them into a single element
            // eventually we may want to try listing them all somehow?
            return convertParam(list.get(0));
        }
        //Shouldn't happen
        return "Unimplemented: " + param;
    }

    @Nullable
    public static String basicImplicitIngredient(Ingredient vanillaIngredient, int amount, JsonElement serialized) {
        if (serialized.isJsonObject()) {
            JsonObject serializedIngredient = serialized.getAsJsonObject();
            if (vanillaIngredient.isVanilla()) {
                if (serializedIngredient.has(JsonConstants.ITEM)) {
                    Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(serializedIngredient.get(JsonConstants.ITEM).getAsString()));
                    return ItemStackUtil.getCommandString(new ItemStack(item, amount));
                } else if (serializedIngredient.has(JsonConstants.TAG)) {
                    return TagManagerItem.INSTANCE.getTag(serializedIngredient.get(JsonConstants.TAG).getAsString()).withAmount(amount).getCommandString();
                }
            } else if (vanillaIngredient instanceof NBTIngredient) {
                ItemStack stack = CraftingHelper.getItemStack(serializedIngredient, true);
                stack.setCount(amount);
                return ItemStackUtil.getCommandString(stack);
            }
        }
        return null;
    }

    private String convertIngredient(ItemStackIngredient ingredient) {
        if (ingredient instanceof ItemStackIngredient.Single single) {
            JsonObject serialized = ingredient.serialize().getAsJsonObject();
            Ingredient vanillaIngredient = single.getInputRaw();
            int amount = GsonHelper.getAsInt(serialized, JsonConstants.AMOUNT, 1);
            String rep = basicImplicitIngredient(vanillaIngredient, amount, serialized.get(JsonConstants.INGREDIENT));
            if (rep == null) {
                rep = IIngredient.fromIngredient(vanillaIngredient).getCommandString();
                if (amount > 1) {
                    return CrTConstants.CLASS_ITEM_STACK_INGREDIENT + ".from(" + rep + ", " + amount + ")";
                }
            }
            //Note: Handled via implicit casts
            return rep;
        } else if (ingredient instanceof ItemStackIngredient.Multi multiIngredient) {
            StringBuilder builder = new StringBuilder(CrTConstants.CLASS_ITEM_STACK_INGREDIENT + ".createMulti(");
            multiIngredient.forEachIngredient(i -> {
                builder.append(convertIngredient(i)).append(", ");
                return false;
            });
            //Remove trailing comma and space
            builder.setLength(builder.length() - 2);
            builder.append(")");
            return builder.toString();
        }
        //Shouldn't happen
        return "Unimplemented itemstack ingredient: " + ingredient;
    }

    private String convertIngredient(FluidStackIngredient ingredient) {
        if (ingredient instanceof FluidStackIngredient.Single) {
            JsonObject serialized = ingredient.serialize().getAsJsonObject();
            //Note: Handled via implicit casts
            return new MCFluidStack(SerializerHelper.deserializeFluid(serialized)).getCommandString();
        } else if (ingredient instanceof FluidStackIngredient.Tagged) {
            JsonObject serialized = ingredient.serialize().getAsJsonObject();
            //Note: Handled via implicit casts
            return TagManagerFluid.INSTANCE.getTag(serialized.get(JsonConstants.TAG).getAsString())
                  .withAmount(serialized.getAsJsonPrimitive(JsonConstants.AMOUNT).getAsInt()).getCommandString();
        } else if (ingredient instanceof FluidStackIngredient.Multi multiIngredient) {
            StringBuilder builder = new StringBuilder(CrTConstants.CLASS_FLUID_STACK_INGREDIENT + ".createMulti(");
            multiIngredient.forEachIngredient(i -> {
                builder.append(convertIngredient(i)).append(", ");
                return false;
            });
            //Remove trailing comma and space
            builder.setLength(builder.length() - 2);
            builder.append(")");
            return builder.toString();
        }
        return "Unimplemented fluidstack ingredient: " + ingredient;
    }

    private <CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>> String convertIngredient(String crtClass,
          CrTChemicalTagManager<CHEMICAL> tagManager, ChemicalIngredientDeserializer<CHEMICAL, STACK, ?> deserializer,
          ChemicalStackIngredient<CHEMICAL, STACK> ingredient) {
        if (ingredient instanceof ChemicalStackIngredient.SingleIngredient) {
            //Serialize and deserialize to get easy access to the amount
            JsonObject serialized = ingredient.serialize().getAsJsonObject();
            //Note: Handled via implicit casts
            return convertParam(deserializer.deserializeStack(serialized));
        } else if (ingredient instanceof ChemicalStackIngredient.TaggedIngredient) {
            JsonObject serialized = ingredient.serialize().getAsJsonObject();
            MCTag<CHEMICAL> tag = tagManager.getTag(serialized.get(JsonConstants.TAG).getAsString());
            long amount = serialized.getAsJsonPrimitive(JsonConstants.AMOUNT).getAsLong();
            if (amount > 0 && amount <= Integer.MAX_VALUE) {
                //Note: Handled via implicit casts
                return tag.withAmount((int) amount).getCommandString();
            }
            //Tag with amount can only handle up to max int, so we have to do it explicitly if we have more
            return crtClass + ".from(" + tag.getCommandString() + ", " + amount + ")";
        } else if (ingredient instanceof ChemicalStackIngredient.MultiIngredient<CHEMICAL, STACK, ?> multiIngredient) {
            StringBuilder builder = new StringBuilder(crtClass + ".createMulti(");
            multiIngredient.forEachIngredient(i -> {
                builder.append(convertIngredient(crtClass, tagManager, deserializer, i)).append(", ");
                return false;
            });
            //Remove trailing comma and space
            builder.setLength(builder.length() - 2);
            builder.append(")");
            return builder.toString();
        }
        //Shouldn't happen
        return "Unimplemented chemical stack ingredient: " + ingredient;
    }
}