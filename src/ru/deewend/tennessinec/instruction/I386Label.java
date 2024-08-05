package ru.deewend.tennessinec.instruction;

import ru.deewend.tennessinec.exporter.Exporter;

import java.nio.ByteBuffer;

@NotARealMachineInstruction
public class I386Label implements Instruction {
    private final String name;
    private int virtualAddress;

    public I386Label(Exporter exporter, String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int getVirtualAddress() {
        return virtualAddress;
    }

    public void setVirtualAddress(int virtualAddress) {
        this.virtualAddress = virtualAddress;
    }

    @Override
    public void encode(ByteBuffer buffer) {
        throw new UnsupportedOperationException("Attempted to encode a label");
    }
}
