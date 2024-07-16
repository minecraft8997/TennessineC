package ru.deewend.tennessinec.instruction;

import ru.deewend.tennessinec.Helper;
import ru.deewend.tennessinec.Pair;
import ru.deewend.tennessinec.exporter.Exporter;

import java.nio.ByteBuffer;

public class I386Sub implements Instruction {
    private final int modRM;
    private final int constant;

    public I386Sub(Exporter exporter, Pair<Integer, Integer> parameters) {
        this.modRM = parameters.getFirst();
        this.constant = parameters.getSecond();
    }

    @Override
    public void encode(ByteBuffer buffer) {
        buffer.put((byte) 0x81);
        buffer.put((byte) modRM);
        if (constant != Helper.SKIP_PARAMETER) buffer.put((byte) constant);
    }
}
