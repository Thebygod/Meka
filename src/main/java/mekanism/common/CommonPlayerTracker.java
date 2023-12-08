package mekanism.common;

import mekanism.api.text.EnumColor;
import mekanism.common.advancements.MekanismCriteriaTriggers;
import mekanism.common.block.BlockBounding;
import mekanism.common.block.BlockCardboardBox;
import mekanism.common.block.BlockMekanism;
import mekanism.common.lib.radiation.RadiationManager;
import mekanism.common.network.to_client.PacketPlayerData;
import mekanism.common.network.to_client.PacketRadiationData;
import mekanism.common.network.to_client.PacketResetPlayerClient;
import mekanism.common.network.to_client.PacketSecurityUpdate;
import mekanism.common.registries.MekanismItems;
import mekanism.common.tags.MekanismTags.Items;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;

public class CommonPlayerTracker {

    private static final Component ALPHA_WARNING = MekanismLang.LOG_FORMAT.translateColored(EnumColor.RED, MekanismLang.MEKANISM, EnumColor.GRAY,
          MekanismLang.ALPHA_WARNING.translate(EnumColor.INDIGO, ChatFormatting.UNDERLINE, new ClickEvent(Action.OPEN_URL,
                "https://github.com/mekanism/Mekanism#alpha-status"), MekanismLang.ALPHA_WARNING_HERE));

    public CommonPlayerTracker() {
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onPlayerLoginEvent(PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide) {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            Mekanism.packetHandler().sendTo(new PacketSecurityUpdate(), serverPlayer);
            //serverPlayer.sendSystemMessage(ALPHA_WARNING);
            MekanismCriteriaTriggers.LOGGED_IN.trigger(serverPlayer);
        }
    }

    @SubscribeEvent
    public void onPlayerLogoutEvent(PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        Mekanism.playerState.clearPlayer(player.getUUID(), false);
        Mekanism.playerState.clearPlayerServerSideOnly(player.getUUID());
    }

    @SubscribeEvent
    public void onPlayerDimChangedEvent(PlayerChangedDimensionEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        Mekanism.playerState.clearPlayer(player.getUUID(), false);
        Mekanism.playerState.reapplyServerSideOnly(player);
        Mekanism.packetHandler().sendTo(PacketRadiationData.createPlayer(player), player);
        RadiationManager.get().updateClientRadiation(player);
    }

    @SubscribeEvent
    public void onPlayerStartTrackingEvent(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof Player player && event.getEntity() instanceof ServerPlayer serverPlayer) {
            Mekanism.packetHandler().sendTo(new PacketPlayerData(player.getUUID()), serverPlayer);
        }
    }

    @SubscribeEvent
    public void respawnEvent(PlayerEvent.PlayerRespawnEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        Mekanism.packetHandler().sendTo(PacketRadiationData.createPlayer(player), player);
        RadiationManager.get().updateClientRadiation(player);
        Mekanism.packetHandler().sendToAll(new PacketResetPlayerClient(player.getUUID()));
    }

    /**
     * If the player is sneaking and the dest block is a cardboard box, ensure onBlockActivated is called, and that the item use is not.
     */
    @SubscribeEvent
    public void rightClickEvent(RightClickBlock event) {
        ItemStack itemInHand = event.getEntity().getItemInHand(event.getHand());
        if (itemInHand.is(Items.CONFIGURATORS) && !itemInHand.is(MekanismItems.CONFIGURATOR.asItem())) {
            //it's a wrench, see if it's our block. Not the configurator, as it handles bypass correctly
            Block block = event.getLevel().getBlockState(event.getPos()).getBlock();
            if (block instanceof BlockMekanism || block instanceof BlockBounding) {
                event.setUseBlock(Event.Result.ALLOW);//force it to use the item on the block
            }
        } else if (event.getEntity().isShiftKeyDown() && event.getLevel().getBlockState(event.getPos()).getBlock() instanceof BlockCardboardBox) {
            event.setUseBlock(Event.Result.ALLOW);
            event.setUseItem(Event.Result.DENY);
        }
    }
}