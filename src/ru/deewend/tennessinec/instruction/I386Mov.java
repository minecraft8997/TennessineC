package ru.deewend.tennessinec.instruction;

import ru.deewend.tennessinec.Helper;
import ru.deewend.tennessinec.Triple;
import ru.deewend.tennessinec.exporter.Exporter;

import java.nio.ByteBuffer;

public class I386Mov implements Instruction {
    private final Helper.MovType movType;
    private final int modRM;
    private final int constant;

    public I386Mov(Exporter exporter, Triple<Helper.MovType, Integer, Integer> parameters) {
        this.movType = parameters.getFirst();
        this.modRM = parameters.getSecond();
        this.constant = parameters.getThird();
    }

    @Override
    public void encode(ByteBuffer buffer) {
        byte opcode = (byte) (movType == Helper.MovType.REG_TO_REG_OR_REG_TO_MEM ? 0x89 : 0x8B);
        buffer.put(opcode);
        buffer.put((byte) modRM);
        if (constant != Helper.SKIP_PARAMETER) buffer.put((byte) constant);
    }
}
