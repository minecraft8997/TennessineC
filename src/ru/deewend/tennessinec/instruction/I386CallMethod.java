package ru.deewend.tennessinec.instruction;

import ru.deewend.tennessinec.TMethod;
import ru.deewend.tennessinec.exporter.Exporter;

import java.nio.ByteBuffer;

public class I386CallMethod implements Instruction {
    private final TMethod method;

    public I386CallMethod(Exporter exporter, TMethod method) {
        this.method = method;
    }

    @Override
    public void encode(ByteBuffer buffer) {
        buffer.putShort((short) 0x15FF);
        int virtualAddress = method.getVirtualAddress();
        if (virtualAddress == TMethod.UNINITIALIZED_VIRTUAL_ADDRESS) {
            throw new RuntimeException("Virtual address of method \"" + method.toStringExtended() + "\" is " +
                    "unknown");
        }
        buffer.putInt(virtualAddress);
    }
}
