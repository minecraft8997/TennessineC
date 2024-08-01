package ru.deewend.tennessinec.instruction;

import ru.deewend.tennessinec.ModRM;
import ru.deewend.tennessinec.TMethod;
import ru.deewend.tennessinec.exporter.Exporter;

import java.nio.ByteBuffer;

public class I386CallMethod implements Instruction {
    private final Exporter exporter;
    private final TMethod method;

    public I386CallMethod(Exporter exporter, TMethod method) {
        this.exporter = exporter;
        this.method = method;
    }

    @Override
    public void encode(ByteBuffer buffer) {
        int virtualAddress = method.getVirtualAddress();
        if (virtualAddress == TMethod.UNINITIALIZED_VIRTUAL_ADDRESS) {
            throw new RuntimeException("Virtual address of method \"" + method.toStringExtended() + "\" is " +
                    "unknown");
        }
        if (method.isExternal()) {
            buffer.putShort((short) 0x15FF);
            buffer.putInt(virtualAddress);
        } else {
            int currentVirtualAddress = exporter.currentVirtualAddress();
            int offset = -(currentVirtualAddress - virtualAddress + 4 + 1);

            buffer.put((byte) 0xE8);
            buffer.putInt(offset);

            // ADD ESP,0x4 // TODO do we really need it at all? It seems to break things
            buffer.put((byte) 0x83);
            buffer.put(ModRM.builder().setMod(ModRM.MOD_REGISTER_TO_REGISTER).setReg(0b000).setRm(0b100).byteValue());
            buffer.put((byte) 0x4);
            // TODO re-implement when Add instruction with an immediate value will be supported
        }
    }
}
