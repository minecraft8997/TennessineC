package ru.deewend.tennessinec.instruction;

import ru.deewend.tennessinec.Helper;
import ru.deewend.tennessinec.Pair;
import ru.deewend.tennessinec.exporter.Exporter;

import java.nio.ByteBuffer;

public class I386Sub implements Instruction {
    private byte opcode = (byte) 0x81;
    private final int modRM;
    private final int constant;

    public I386Sub(Exporter exporter, Pair<Integer, Integer> parameters) {
        this.modRM = parameters.getFirst();
        this.constant = parameters.getSecond();
    }

    protected void setAdd() {
        this.opcode = (byte) 0x01;
    }

    @Override
    public void encode(ByteBuffer buffer) {
        buffer.put(opcode);
        buffer.put((byte) modRM);
        if (constant != Helper.SKIP_PARAMETER) buffer.putInt(constant);
    }
}
