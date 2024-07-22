package ru.deewend.tennessinec.instruction;

import ru.deewend.tennessinec.exporter.Exporter;

import java.nio.ByteBuffer;

public class I386MovEAX implements Instruction {
    private final int value;

    public I386MovEAX(Exporter exporter, Integer value) {
        this.value = value;
    }

    @Override
    public void encode(ByteBuffer buffer) {
        buffer.put((byte) 0xB8);
        buffer.putInt(value);
    }
}
