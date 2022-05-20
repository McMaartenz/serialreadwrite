package com.kbs2.serialrw;

import com.kbs2.serialrw.mockArduino.MockSerial;
import com.kbs2.serialrw.serial.*;

import java.nio.charset.StandardCharsets;

public class Main
{
    public static void main(String[] args)
    {
        MockSerial mockSerial = new MockSerial();
        try (SerialComm serialComm = new SerialComm(mockSerial))
        {
            byte packetId1 = serialComm.sendPacket(SerialCommType.REQUEST_POSITION, "hello world1");
            byte packetId2 = serialComm.sendPacket(SerialCommType.REQUEST_POSITION, "hello world2");
            byte packetId3 = serialComm.sendPacket(SerialCommType.REQUEST_POSITION, "hello world3");
            Packet packet1 = serialComm.getPacketInfo(packetId1);
            Packet packet2 = serialComm.getPacketInfo(packetId2);
            Packet packet3 = serialComm.getPacketInfo(packetId3);

            Packet A = new Packet((byte)0, SerialCommType.ACKNOWLEDGEMENT, "hello1".getBytes(StandardCharsets.UTF_8), PacketState.AWAITING_ACKNOWLEDGEMENT);
            Packet B = new Packet((byte)0, SerialCommType.RESPONSE_PACKET, "hello2".getBytes(StandardCharsets.UTF_8), PacketState.AWAITING_ACKNOWLEDGEMENT);
            Packet C = new Packet((byte)1, SerialCommType.ACKNOWLEDGEMENT, "hello3".getBytes(StandardCharsets.UTF_8), PacketState.AWAITING_ACKNOWLEDGEMENT);
            Packet D = new Packet((byte)1, SerialCommType.RESPONSE_PACKET, "hello4".getBytes(StandardCharsets.UTF_8), PacketState.AWAITING_ACKNOWLEDGEMENT);

            byte[] bA = A.convertToBytes();
            byte[] bB = B.convertToBytes();
            byte[] bC = C.convertToBytes();
            byte[] bD = D.convertToBytes();
            byte[][] bytes = new byte[4][];
            bytes[0] = bA;
            bytes[1] = bB;
            bytes[2] = bC;
            bytes[3] = bD;

            mockSerial.mockPacket(bytes);
            long time = System.currentTimeMillis();
            time += 1000;
            while (System.currentTimeMillis() < time)
            {
                Thread.onSpinWait();
            }

            System.out.println(packet1);
            System.out.println(packet2);
            System.out.println(packet3);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
