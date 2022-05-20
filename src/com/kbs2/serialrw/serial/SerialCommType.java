package com.kbs2.serialrw.serial;

public enum SerialCommType
{
	ACKNOWLEDGEMENT,
	RESPONSE_PACKET,
	REQUEST_POSITION;

	static byte toByte(SerialCommType serialCommType)
	{
		return (byte)serialCommType.ordinal();
	}
}
