package mekanism.common.item;

import java.util.List;
import mekanism.api.security.IItemSecurityUtils;
import mekanism.common.capabilities.security.item.ItemStackOwnerObject;
import mekanism.common.config.MekanismConfig;
import mekanism.common.item.interfaces.IGuiItem;
import mekanism.common.lib.frequency.FrequencyType;
import mekanism.common.lib.frequency.IFrequencyItem;
import mekanism.common.lib.security.ItemSecurityUtils;
import mekanism.common.registration.impl.ContainerTypeRegistryObject;
import mekanism.common.registries.MekanismContainerTypes;
import mekanism.common.util.MekanismUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.NotNull;

public class ItemPortableTeleporter extends ItemEnergized implements IFrequencyItem, IGuiItem {

    public ItemPortableTeleporter(Properties properties) {
        super(MekanismConfig.gear.portableTeleporterChargeRate, MekanismConfig.gear.portableTeleporterMaxEnergy, properties.rarity(Rarity.RARE));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Level world, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        IItemSecurityUtils.INSTANCE.addSecurityTooltip(stack, tooltip);
        MekanismUtils.addFrequencyItemTooltip(stack, tooltip);
        super.appendHoverText(stack, world, tooltip, flag);
    }

    @Override
    public FrequencyType<?> getFrequencyType() {
        return FrequencyType.TELEPORTER;
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level world, @NotNull Player player, @NotNull InteractionHand hand) {
        return ItemSecurityUtils.get().claimOrOpenGui(world, player, hand, getContainerType()::tryOpenGui);
    }

    @Override
    public ContainerTypeRegistryObject<?> getContainerType() {
        return MekanismContainerTypes.PORTABLE_TELEPORTER;
    }

    @Override
    public void attachCapabilities(RegisterCapabilitiesEvent event) {
        super.attachCapabilities(event);
        event.registerItem(IItemSecurityUtils.INSTANCE.ownerCapability(), (stack, ctx) -> new ItemStackOwnerObject(stack), this);
    }
}