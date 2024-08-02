package ru.deewend.tennessinec.instruction;

import ru.deewend.tennessinec.Helper;
import ru.deewend.tennessinec.Pair;
import ru.deewend.tennessinec.exporter.Exporter;

import java.nio.ByteBuffer;

public class I386Sub implements Instruction {
    private boolean add;
    private final int modRM;
    private final int constant;

    public I386Sub(Exporter exporter, Pair<Integer, Integer> parameters) {
        this.modRM = parameters.getFirst();
        this.constant = parameters.getSecond();
    }

    protected void setAdd() {
        add = true;
    }

    @Override
    public void encode(ByteBuffer buffer) {
        boolean hasImmediateValue = (constant != Helper.SKIP_PARAMETER);
        if (add) {
            if (hasImmediateValue) {
                throw new RuntimeException("Instruction Add with an " +
                        "immediate value is unsupported in this TennessineC version");
            }
            buffer.put((byte) 0x01);
            buffer.put((byte) modRM);
        } else { // sub
            if (hasImmediateValue) {
                buffer.put((byte) 0x81);
            } else {
                buffer.put((byte) 0x29);
            }
            buffer.put((byte) modRM);
            if (hasImmediateValue) {
                buffer.putInt(constant);
            }
        }
    }
}
