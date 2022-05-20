package com.kbs2.serialrw.mockArduino;
import java.nio.ByteBuffer;

public class MockSerial
{
	private byte[] buffer;

	public void flush() { }
	public void write(byte[] packet) { }
	public byte[] read()
	{
		return buffer;
	}

	public void mockPacket(byte[][] packetsData)
	{
		int size = 0;
		for (byte[] barr : packetsData)
		{
			size += barr[0];
		}

		if (size == 0)
		{
			return;
		}

		ByteBuffer b = ByteBuffer.wrap(new byte[size]);
		for (byte[] packet : packetsData)
		{
			b.put(packet);
		}

		buffer = b.array();
	}
}
