package ru.deewend.tennessinec.instruction;

import ru.deewend.tennessinec.exporter.Exporter;

import java.nio.ByteBuffer;

public class I386Jmp implements Instruction {
    private final int offset;

    public I386Jmp(Exporter exporter, Integer offset) {
        this.offset = offset;
    }

    @Override
    public void encode(ByteBuffer buffer) {
        buffer.put((byte) 0xE9);
        buffer.putInt(offset);
    }
}
