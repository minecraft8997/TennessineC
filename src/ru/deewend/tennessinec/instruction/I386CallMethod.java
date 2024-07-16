package ru.deewend.tennessinec.instruction;

import ru.deewend.tennessinec.exporter.Exporter;

import java.nio.ByteBuffer;

public class I386CallMethod implements Instruction {
    private final Exporter exporter;
    private final String methodName;

    public I386CallMethod(Exporter exporter, String methodName) {
        this.exporter = exporter;
        this.methodName = methodName;
    }

    @Override
    public void encode(ByteBuffer buffer) {
        buffer.putShort((short) 0x15FF);
        buffer.putInt(exporter.getMethodVirtualAddress(methodName));
    }
}
