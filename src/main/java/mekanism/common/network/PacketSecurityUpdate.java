package mekanism.common.network;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import mekanism.client.MekanismClient;
import mekanism.common.Mekanism;
import mekanism.common.PacketHandler;
import mekanism.common.frequency.Frequency;
import mekanism.common.network.PacketSecurityUpdate.SecurityUpdateMessage;
import mekanism.common.security.SecurityData;
import mekanism.common.security.SecurityFrequency;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketSecurityUpdate implements IMessageHandler<SecurityUpdateMessage, IMessage>
{
	@Override
	public IMessage onMessage(SecurityUpdateMessage message, MessageContext context) 
	{
		if(message.packetType == SecurityPacket.UPDATE)
		{
			MekanismClient.clientSecurityMap.put(message.playerUUID, message.securityData);
		}
		
		return null;
	}
	
	public static class SecurityUpdateMessage implements IMessage
	{
		public SecurityPacket packetType;
		
		public UUID playerUUID;
		public SecurityData securityData;
		
		public SecurityUpdateMessage() {}
	
		public SecurityUpdateMessage(SecurityPacket type, UUID username, SecurityData data)
		{
			packetType = type;
			
			if(packetType == SecurityPacket.UPDATE)
			{
				playerUUID = username;
				securityData = data;
			}
		}
	
		@Override
		public void toBytes(ByteBuf dataStream)
		{
			dataStream.writeInt(packetType.ordinal());
			
			if(packetType == SecurityPacket.UPDATE)
			{
				PacketHandler.writeString(dataStream, playerUUID.toString());
				securityData.write(dataStream);
			}
			else if(packetType == SecurityPacket.FULL)
			{
				List<SecurityFrequency> frequencies = new ArrayList<SecurityFrequency>();
				
				for(Frequency frequency : Mekanism.securityFrequencies.getFrequencies())
				{
					if(frequency instanceof SecurityFrequency)
					{
						frequencies.add((SecurityFrequency)frequency);
					}
				}
				
				dataStream.writeInt(frequencies.size());
				
				for(SecurityFrequency frequency : frequencies)
				{
					PacketHandler.writeString(dataStream, frequency.owner.toString());
					new SecurityData(frequency).write(dataStream);
				}
			}
		}
	
		@Override
		public void fromBytes(ByteBuf dataStream)
		{
			packetType = SecurityPacket.values()[dataStream.readInt()];
			
			if(packetType == SecurityPacket.UPDATE)
			{
				playerUUID = UUID.fromString(PacketHandler.readString(dataStream));
				securityData = SecurityData.read(dataStream);
			}
			else if(packetType == SecurityPacket.FULL)
			{
				MekanismClient.clientSecurityMap.clear();
				
				int amount = dataStream.readInt();
				
				for(int i = 0; i < amount; i++)
				{
					UUID owner = UUID.fromString(PacketHandler.readString(dataStream));
					SecurityData data = SecurityData.read(dataStream);
					
					MekanismClient.clientSecurityMap.put(owner, data);
				}
			}
		}
	}
	
	public static enum SecurityPacket
	{
		UPDATE,
		FULL;
	}
}
