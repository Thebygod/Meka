package mekanism.common.network.to_client;

import mekanism.common.lib.radiation.RadiationManager;
import mekanism.common.lib.radiation.RadiationManager.LevelAndMaxMagnitude;
import mekanism.common.network.IMekanismPacket;
import mekanism.common.registries.MekanismAttachmentTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.NetworkEvent;

public class PacketRadiationData implements IMekanismPacket {

    private final RadiationPacketType type;
    private final double radiation;
    private final double maxMagnitude;

    private PacketRadiationData(RadiationPacketType type, double radiation, double maxMagnitude) {
        this.type = type;
        this.radiation = radiation;
        this.maxMagnitude = maxMagnitude;
    }

    public static PacketRadiationData createEnvironmental(LevelAndMaxMagnitude levelAndMaxMagnitude) {
        return new PacketRadiationData(RadiationPacketType.ENVIRONMENTAL, levelAndMaxMagnitude.level(), levelAndMaxMagnitude.maxMagnitude());
    }

    public static PacketRadiationData createPlayer(Player player) {
        return createPlayer(player.getData(MekanismAttachmentTypes.RADIATION));
    }

    public static PacketRadiationData createPlayer(double radiation) {
        return new PacketRadiationData(RadiationPacketType.PLAYER, radiation, 0);
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        if (type == RadiationPacketType.ENVIRONMENTAL) {
            RadiationManager.get().setClientEnvironmentalRadiation(radiation, maxMagnitude);
        } else if (type == RadiationPacketType.PLAYER) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.setData(MekanismAttachmentTypes.RADIATION, radiation);
            }
        }
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(type);
        buffer.writeDouble(radiation);
        if (type.tracksMaxMagnitude) {
            buffer.writeDouble(maxMagnitude);
        }
    }

    public static PacketRadiationData decode(FriendlyByteBuf buffer) {
        RadiationPacketType type = buffer.readEnum(RadiationPacketType.class);
        return new PacketRadiationData(type, buffer.readDouble(), type.tracksMaxMagnitude ? buffer.readDouble() : 0);
    }

    public enum RadiationPacketType {
        ENVIRONMENTAL(true),
        PLAYER(false);

        private final boolean tracksMaxMagnitude;

        RadiationPacketType(boolean tracksMaxMagnitude) {
            this.tracksMaxMagnitude = tracksMaxMagnitude;
        }
    }
}
