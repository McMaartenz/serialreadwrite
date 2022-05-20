package com.kbs2.serialrw.serial;

import com.kbs2.serialrw.exception.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Packet
{
	private byte packetId;
	private SerialCommType packetType;
	private byte[] packetData;
	private PacketState packetState;

	private long timeSent;
	private long timeAckReceived;
	private long timeResponseReceived;

	private Packet responsePacket;

	/**
	 * Create new packet
	 * @param packetId ID of packet
	 * @param packetType Type of packet: what command is issued
	 * @param packetData Data of packet: command may add additional data
	 * @param packetState State of packet: is an acknowledgement received yet? timeout?
	 */
	public Packet(byte packetId, SerialCommType packetType, byte[] packetData, PacketState packetState)
	{
		this.packetId = packetId;
		this.packetType = packetType;
		this.packetData = packetData;
		this.packetState = packetState;

		responsePacket = null;
	}

	/**
	 * Create packet byte buffer received from COMM
	 * @param packetBuffer The byte buffer
	 */
	public Packet(byte[] packetBuffer)
	{
		int packetLength = packetBuffer[0];
		this.packetId = packetBuffer[1];
		this.packetType = SerialCommType.values()[packetBuffer[2]];

		if (packetLength < 4)
		{
			this.packetData = new byte[0];
		}
		else
		{
			this.packetData = Arrays.copyOfRange(packetBuffer, 3, packetLength);
		}

		responsePacket = null;
	}

	/**
	 * Receive acknowledgement
	 */
	public void receiveAcknowledgement()
	{
		timeAckReceived = System.currentTimeMillis();
		packetState = PacketState.AWAITING_RESPONSE_PACKET;
	}

	/**
	 * Receive a response
	 * @param responsePacket Packet received from serial that is responding to this
	 */
	public void receiveResponse(Packet responsePacket)
	{
		this.responsePacket = responsePacket;
		this.packetState = PacketState.RESPONSE_PACKET_RECEIVED;
		setTimeResponseReceived(System.currentTimeMillis());
	}

	/**
	 * Set time sent
	 * @param unixTime time in Unix Epoch
	 * @see <a href="https://en.wikipedia.org/wiki/Unix_time">Unix Time (Wikipedia)</a>
	 */
	public void setTimeSent(long unixTime)
	{
		if (timeSent == 0)
		{
			timeSent = unixTime;
		}
		else
		{
			throw new TimeAlreadySetException();
		}
	}

	/**
	 * Set time acknowledgement is received
	 * @param unixTime time in Unix Epoch
	 * @see <a href="https://en.wikipedia.org/wiki/Unix_time">Unix Time (Wikipedia)</a>
	 */
	public void setTimeAckReceived(long unixTime)
	{
		if (timeAckReceived == 0)
		{
			timeAckReceived = unixTime;
		}
		else
		{
			throw new TimeAlreadySetException();
		}
	}

	/**
	 * Set time response is received
	 * @param unixTime time in Unix Epoch
	 * @see <a href="https://en.wikipedia.org/wiki/Unix_time">Unix Time (Wikipedia)</a>
	 */
	public void setTimeResponseReceived(long unixTime)
	{
		if (timeResponseReceived == 0)
		{
			timeResponseReceived = unixTime;
		}
		else
		{
			throw new TimeAlreadySetException();
		}
	}

	public boolean isAcknowledged()
	{
		return timeAckReceived > 0;
	}

	public boolean receivedResponse()
	{
		return responsePacket != null;
	}

	public long getTimeSent()
	{
		return timeSent;
	}

	public long getTimeAckReceived()
	{
		return timeAckReceived;
	}

	public long getTimeResponseReceived()
	{
		return timeResponseReceived;
	}

	public byte getPacketId()
	{
		return packetId;
	}

	public SerialCommType getPacketType()
	{
		return packetType;
	}

	public byte[] getPacketData()
	{
		return packetData;
	}

	/**
	 * Get packet data
	 * @return packet data, converted from byte[] to string
	 */
	public String getPacketDataAsString()
	{
		return new String(packetData, StandardCharsets.UTF_8);
	}

	/**
	 * Convert the packet to bytes, to be sent through COMM
	 * @return The packet, in shape: [packet size][packet id][packet type][packet data]
	 */
	public byte[] convertToBytes()
	{
		byte packetLength = (byte)(packetData.length + 3);
		ByteBuffer packetBuffer = ByteBuffer.wrap(new byte[packetLength]);
		packetBuffer.put(packetLength);
		packetBuffer.put(packetId);
		packetBuffer.put(SerialCommType.toByte(packetType));
		packetBuffer.put(packetData);

		return packetBuffer.array();
	}

	/**
	 * Detect a timeout
	 * @return true if timeout detected
	 */
	public boolean detectTimeout()
	{
		if (timeResponseReceived != 0)
		{
			return false;
		}

		long currentTime = System.currentTimeMillis();
		boolean timeout = (currentTime - lastActiveTime()) > 5000;
		if (timeout)
		{
			packetState = PacketState.PACKET_REACHED_A_TIMEOUT;
		}

		return timeout;
	}

	/**
	 * Is the packet safe to override?
	 * @return true if packet reached a timeout or has received a response more than 2 seconds ago
	 */
	public boolean safeToOverride()
	{
		if (detectTimeout())
		{
			return true;
		}

		long time = System.currentTimeMillis();
		return ((time - timeResponseReceived) > 2000);
	}

	/**
	 * Last active time
	 * @return last time this packet was active (e.g. received ack, or data)
	 */
	public long lastActiveTime()
	{
		if (timeResponseReceived != 0)
		{
			return timeResponseReceived;
		}
		if (timeAckReceived != 0)
		{
			return timeAckReceived;
		}
		return timeSent;
	}

	@Override
	public String toString() // JSON
	{
		return String.format("{\"packetId\":\"%d\",\"packetType\":\"%s\",\"packetState\":\"%s\",\"packetData\":\"%s\",\"responsePacket\":%s}",
			packetId,
			packetType,
			packetState,
			getPacketDataAsString(),
			receivedResponse() ? responsePacket: "\"\""
		);
	}
}
