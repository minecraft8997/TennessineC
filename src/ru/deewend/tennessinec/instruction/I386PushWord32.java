package ru.deewend.tennessinec.instruction;

import ru.deewend.tennessinec.exporter.Exporter;

import java.nio.ByteBuffer;

public class I386PushWord32 implements Instruction {
    private final int address;

    public I386PushWord32(Exporter exporter, Integer address) {
        this.address = address;
    }

    @Override
    public void encode(ByteBuffer buffer) {
        buffer.put((byte) 0x68);
        buffer.putInt(address);
    }
}
