package ru.deewend.tennessinec.instruction;

import ru.deewend.tennessinec.exporter.Exporter;

import java.nio.ByteBuffer;

public class I386PushEBP implements Instruction {
    public I386PushEBP(Exporter exporter, Object nothing) {
    }

    @Override
    public void encode(ByteBuffer buffer) {
        buffer.put((byte) 0x55);
    }
}
