package ru.deewend.tennessinec.instruction;

import ru.deewend.tennessinec.exporter.Exporter;

import java.nio.ByteBuffer;

public class I386Test implements Instruction {
    private final int modRM;

    public I386Test(Exporter exporter, Integer modRM) {
        this.modRM = modRM;
    }

    @Override
    public void encode(ByteBuffer buffer) {
        buffer.put((byte) 0x85);
        buffer.put((byte) modRM);
    }
}

