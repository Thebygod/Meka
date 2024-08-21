package mekanism.common.integration.framedblocks;

import com.mojang.serialization.MapCodec;
import mekanism.api.Action;
import mekanism.api.MekanismAPI;
import mekanism.api.MekanismAPITags;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.text.TextComponentUtil;
import mekanism.common.MekanismLang;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.api.camo.CamoContainerFactory;
import xfacthd.framedblocks.api.camo.TriggerRegistrar;
import xfacthd.framedblocks.api.util.CamoMessageVerbosity;
import xfacthd.framedblocks.api.util.ConfigView;

final class ChemicalCamoContainerFactory extends CamoContainerFactory<ChemicalCamoContainer> {

    private static final MapCodec<ChemicalCamoContainer> CODEC = Chemical.CODEC
            .xmap(ChemicalCamoContainer::new, ChemicalCamoContainer::getChemical)
            .fieldOf("chemical");
    private static final StreamCodec<RegistryFriendlyByteBuf, ChemicalCamoContainer> STREAM_CODEC =
            Chemical.STREAM_CODEC.map(ChemicalCamoContainer::new, ChemicalCamoContainer::getChemical);
    private static final Component MSG_HAS_SPECIAL_HANDLING = TextComponentUtil.translate(
            MekanismLang.FRAMEDBLOCKS_CAMO_HAS_SPECIAL_HANDLING.getTranslationKey()
    );

    @Override
    protected void writeToNetwork(CompoundTag tag, ChemicalCamoContainer camo) {
        Chemical chemical = camo.getChemical();
        tag.putInt("chemical", MekanismAPI.CHEMICAL_REGISTRY.getId(chemical));
    }

    @Override
    protected ChemicalCamoContainer readFromNetwork(CompoundTag tag) {
        Chemical chemical = MekanismAPI.CHEMICAL_REGISTRY.byId(tag.getInt("chemical"));
        return new ChemicalCamoContainer(chemical);
    }

    @Override
    @Nullable
    public ChemicalCamoContainer applyCamo(Level level, BlockPos pos, Player player, ItemStack stack) {
        IChemicalHandler handler = Capabilities.CHEMICAL.getCapability(stack);
        if (handler == null || handler.getChemicalTanks() <= 0) {
             return null;
        }

        for (int tank = 0; tank < handler.getChemicalTanks(); tank++) {
            ChemicalStack chemical = handler.getChemicalInTank(tank);
            if (!isValidChemical(chemical.getChemical(), player)) {
                continue;
            }

            if (!player.isCreative() && ConfigView.Server.INSTANCE.shouldConsumeCamoItem()) {
                ChemicalStack extracted = handler.extractChemical(tank, FramedBlocksIntegration.Constants.CHEMICAL_AMOUNT, Action.SIMULATE);
                if (extracted.getAmount() != FramedBlocksIntegration.Constants.CHEMICAL_AMOUNT) {
                    continue;
                }

                if (!level.isClientSide()) {
                    handler.extractChemical(tank, FramedBlocksIntegration.Constants.CHEMICAL_AMOUNT, Action.EXECUTE);
                }
            }

            return new ChemicalCamoContainer(chemical.getChemical());
        }
        return null;
    }

    @Override
    public boolean removeCamo(Level level, BlockPos pos, Player player, ItemStack stack, ChemicalCamoContainer camo) {
        if (stack.isEmpty()) {
            return false;
        }

        IChemicalHandler handler = Capabilities.CHEMICAL.getCapability(stack);
        if (handler == null || handler.getChemicalTanks() <= 0) {
            return false;
        }

        ChemicalStack chemical = camo.getChemical().getStack(FramedBlocksIntegration.Constants.CHEMICAL_AMOUNT);
        if (!isValidForHandler(handler, chemical)) {
            return false;
        }
        if (!player.isCreative() && ConfigView.Server.INSTANCE.shouldConsumeCamoItem()) {
            if (!handler.insertChemical(chemical, Action.SIMULATE).isEmpty()) {
                return false;
            }
            if (!level.isClientSide()) {
                handler.insertChemical(chemical, Action.EXECUTE);
            }
        }
        return true;
    }

    private static boolean isValidForHandler(IChemicalHandler handler, ChemicalStack chemical) {
        for (int tank = 0; tank < handler.getChemicalTanks(); tank++) {
            if (!handler.isValid(tank, chemical)) {
                continue;
            }
            ChemicalStack inTank = handler.getChemicalInTank(tank);
            if (inTank.isEmpty() || inTank.is(chemical.getChemical())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canTriviallyConvertToItemStack() {
        return false;
    }

    @Override
    public ItemStack dropCamo(ChemicalCamoContainer camo) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean validateCamo(ChemicalCamoContainer camo) {
        return isValidChemical(camo.getChemical(), null);
    }

    private static boolean isValidChemical(Chemical chemical, @Nullable Player player) {
        if (chemical.isEmptyType()) {
            return false;
        }
        if (chemical.hasAttributesWithValidation()) {
            displayValidationMessage(player, MSG_HAS_SPECIAL_HANDLING, CamoMessageVerbosity.DEFAULT);
            return false;
        }
        if (chemical.is(MekanismAPITags.Chemicals.FRAMEDBLOCKS_BLACKLISTED)) {
            displayValidationMessage(player, MSG_BLACKLISTED, CamoMessageVerbosity.DEFAULT);
            return false;
        }
        return true;
    }

    @Override
    public MapCodec<ChemicalCamoContainer> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, ChemicalCamoContainer> streamCodec() {
        return STREAM_CODEC;
    }

    @Override
    public void registerTriggerItems(TriggerRegistrar registrar) {
        registrar.registerApplicationPredicate(Capabilities.CHEMICAL::hasCapability);
        registrar.registerRemovalPredicate(Capabilities.CHEMICAL::hasCapability);
    }
}
