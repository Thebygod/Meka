package mekanism.common.item;

import java.util.List;
import mekanism.api.IConfigCardAccess;
import mekanism.api.NBTConstants;
import mekanism.api.security.ISecurityUtils;
import mekanism.api.text.EnumColor;
import mekanism.api.text.TextComponentUtil;
import mekanism.common.MekanismLang;
import mekanism.common.advancements.MekanismCriteriaTriggers;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.util.ItemDataUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.NBTUtils;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ItemConfigurationCard extends Item {

    public ItemConfigurationCard(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Level world, List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(MekanismLang.CONFIG_CARD_HAS_DATA.translateColored(EnumColor.GRAY, EnumColor.INDIGO, getConfigCardName(getData(stack))));
    }

    @NotNull
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction side = context.getClickedFace();
        IConfigCardAccess configCardAccess = WorldUtils.getCapability(world, Capabilities.CONFIG_CARD, pos, side);
        if (configCardAccess != null) {
            //TODO - 1.20.2: Replace with supporting non block entities as wel
            BlockEntity tile = WorldUtils.getTileEntity(world, pos);
            if (!ISecurityUtils.INSTANCE.canAccessOrDisplayError(player, tile)) {
                return InteractionResult.FAIL;
            }
            ItemStack stack = context.getItemInHand();
            if (player.isShiftKeyDown()) {
                if (!world.isClientSide) {
                    String translationKey = configCardAccess.getConfigCardName();
                    CompoundTag data = configCardAccess.getConfigurationData(player);
                    data.putString(NBTConstants.DATA_NAME, translationKey);
                    NBTUtils.writeRegistryEntry(data, NBTConstants.DATA_TYPE, BuiltInRegistries.BLOCK_ENTITY_TYPE, configCardAccess.getConfigurationDataType());
                    ItemDataUtils.setCompound(stack, NBTConstants.DATA, data);
                    player.sendSystemMessage(MekanismUtils.logFormat(MekanismLang.CONFIG_CARD_GOT.translate(EnumColor.INDIGO, TextComponentUtil.translate(translationKey))));
                    MekanismCriteriaTriggers.CONFIGURATION_CARD.trigger((ServerPlayer) player, true);
                }
            } else {
                CompoundTag data = getData(stack);
                BlockEntityType<?> storedType = getStoredTileType(data);
                if (storedType == null) {
                    return InteractionResult.PASS;
                }
                if (!world.isClientSide) {
                    if (configCardAccess.isConfigurationDataCompatible(storedType)) {
                        configCardAccess.setConfigurationData(player, data);
                        configCardAccess.configurationDataSet();
                        player.sendSystemMessage(MekanismUtils.logFormat(EnumColor.DARK_GREEN, MekanismLang.CONFIG_CARD_SET.translate(EnumColor.INDIGO,
                              getConfigCardName(data))));
                        MekanismCriteriaTriggers.CONFIGURATION_CARD.trigger((ServerPlayer) player, false);
                    } else {
                        player.sendSystemMessage(MekanismUtils.logFormat(EnumColor.RED, MekanismLang.CONFIG_CARD_UNEQUAL));
                    }
                }
            }
            return InteractionResult.sidedSuccess(world.isClientSide);
        }
        return InteractionResult.PASS;
    }

    private CompoundTag getData(ItemStack stack) {
        CompoundTag data = ItemDataUtils.getCompound(stack, NBTConstants.DATA);
        return data.isEmpty() ? null : data;
    }

    @Nullable
    @Contract("null -> null")
    private BlockEntityType<?> getStoredTileType(@Nullable CompoundTag data) {
        if (data == null || !data.contains(NBTConstants.DATA_TYPE, Tag.TAG_STRING)) {
            return null;
        }
        ResourceLocation tileRegistryName = ResourceLocation.tryParse(data.getString(NBTConstants.DATA_TYPE));
        return tileRegistryName == null ? null : BuiltInRegistries.BLOCK_ENTITY_TYPE.get(tileRegistryName);
    }

    private Component getConfigCardName(@Nullable CompoundTag data) {
        if (data == null || !data.contains(NBTConstants.DATA_NAME, Tag.TAG_STRING)) {
            return MekanismLang.NONE.translate();
        }
        return TextComponentUtil.translate(data.getString(NBTConstants.DATA_NAME));
    }

    public boolean hasData(ItemStack stack) {
        CompoundTag data = getData(stack);
        return data != null && data.contains(NBTConstants.DATA_NAME, Tag.TAG_STRING);
    }
}