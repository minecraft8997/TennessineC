package ru.deewend.tennessinec.instruction;

import ru.deewend.tennessinec.exporter.Exporter;

import java.nio.ByteBuffer;

@NotARealMachineInstruction
public class I386FinishMethod implements Instruction {
    private final Exporter exporter;

    public I386FinishMethod(Exporter exporter, Object nothing) {
        this.exporter = exporter;
    }

    @Override
    public void encode(ByteBuffer buffer) {
        // do we really need it? TCC puts it for some reason
        exporter.putInstruction("Jmp", 0); // no offset
        exporter.encodeLastInstruction();
        buffer.put((byte) 0xC9); // LEAVE
        buffer.put((byte) 0xC3); // RET
    }
}
