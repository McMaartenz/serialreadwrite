package com.kbs2.serialrw.serial;

import com.kbs2.serialrw.exception.*;
import com.kbs2.serialrw.mockArduino.MockSerial;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SerialComm implements AutoCloseable
{
	private byte nextPacketId;
	private MockSerial serialPort;
	private ConcurrentHashMap<Byte, Packet> packets;
	private Queue<Packet> receivedPackets;

	private volatile boolean threadsEnabled;
	private Thread packetListenerWorker;
	private Thread packetHandlerWorker;

	public SerialComm(MockSerial serialPort)
	{
		packets = new ConcurrentHashMap<>();
		this.serialPort = serialPort;
		threadsEnabled = true;

		receivedPackets = new LinkedList<>();

		packetListenerWorker = new Thread(this::packetListener);
		packetListenerWorker.start();
		packetHandlerWorker = new Thread(this::packetHandler);
		packetHandlerWorker.start();
	}

	public void stopThreads()
	{
		threadsEnabled = false;
	}

	/**
	 * Run on thread listening for packets
	 */
	private void packetListener()
	{
		Thread.currentThread().setName("Packet listener thread");
		while (threadsEnabled)
		{
			byte[] serialBuffer = serialPort.read();
			if (serialBuffer == null || serialBuffer.length == 0)
			{
				Thread.onSpinWait();
			}
			else
			{
				decodePackets(serialBuffer);
				break; // Stop listening to stop spam
			}
		}
	}

	/**
	 * Run on thread handling incoming packets
	 */
	private void packetHandler()
	{
		Thread.currentThread().setName("Packet handler thread");
		while (threadsEnabled)
		{
			if (receivedPackets.isEmpty())
			{
				for(byte packetId : packets.keySet())
				{
					packets.get(packetId).detectTimeout();
				}
				Thread.onSpinWait();
			}
			else
			{
				while (!receivedPackets.isEmpty())
				{
					Packet packet = receivedPackets.poll();
					byte packetId = packet.getPacketId();
					SerialCommType packetType = packet.getPacketType();
					if (packetType == SerialCommType.RESPONSE_PACKET)
					{
						if (packets.containsKey(packetId))
						{
							packets.get(packetId).receiveResponse(packet);
						}
						else
						{
							System.out.println("Response packet does no longer have a matching sent packet");
						}
					}
					else if (packetType == SerialCommType.ACKNOWLEDGEMENT)
					{
						if (packets.containsKey(packetId))
						{
							packets.get(packetId).receiveAcknowledgement();
						}
						else
						{
							System.out.println("Acknowledgement packet does no longer have a matching sent packet");
						}
					}
					else
					{
						switch (packetType)
						{
							default:
							{
								System.out.println("Unsupported packet:\n" + packet);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Decode packets and place into received packets array
	 * @param packets byte buffer from serial
	 */
	private void decodePackets(byte[] packets)
	{
		int i = 0;
		while (i < packets.length)
		{
			int size = packets[i];
			Packet packet = new Packet(Arrays.copyOfRange(packets, i, i + size));
			receivedPackets.add(packet);
			i += size;
			System.out.println("Receiving a packet: " + packet);
		}
	}

	public byte sendPacket(SerialCommType packetType, String data)
	{
		byte   packetId = nextPacketId++;
		byte[] packetData = data.getBytes(StandardCharsets.UTF_8);

		Packet packet = new Packet(packetId, packetType, packetData, PacketState.AWAITING_ACKNOWLEDGEMENT);
		if (packets.containsKey(packetId))
		{
			Packet oldPacket = packets.get(packetId);
			if (!oldPacket.safeToOverride())
			{
				throw new PacketBufferOverflowException();
			}
		}
		packets.put(packetId, packet);

		serialPort.write(packet.convertToBytes());
		serialPort.flush();

		return packetId;
	}

	public Packet getPacketInfo(byte packetId)
	{
		if (!packets.containsKey(packetId))
		{
			throw new InvalidPacketIdException();
		}

		return packets.get(packetId);
	}

	@Override
	public void close() throws Exception
	{
		stopThreads();
	}
}
