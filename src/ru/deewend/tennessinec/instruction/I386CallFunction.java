package ru.deewend.tennessinec.instruction;

import ru.deewend.tennessinec.TFunction;
import ru.deewend.tennessinec.exporter.Exporter;

import java.nio.ByteBuffer;

public class I386CallFunction implements Instruction {
    private final Exporter exporter;
    private final TFunction function;

    public I386CallFunction(Exporter exporter, TFunction function) {
        this.exporter = exporter;
        this.function = function;
    }

    @Override
    public void encode(ByteBuffer buffer) {
        int virtualAddress = function.getVirtualAddress();
        if (virtualAddress == TFunction.UNINITIALIZED_VIRTUAL_ADDRESS) {
            throw new RuntimeException("Virtual address of function \"" + function.toStringExtended() + "\" is " +
                    "unknown");
        }
        if (function.isExternal()) {
            buffer.putShort((short) 0x15FF);
            buffer.putInt(virtualAddress);
        } else {
            int currentVirtualAddress = exporter.currentVirtualAddress();
            int offset = -(currentVirtualAddress - virtualAddress + 4 + 1);

            buffer.put((byte) 0xE8);
            buffer.putInt(offset);

            // ADD ESP,<from.stackSize>
            // TODO do we really need it at all? It seems to break things
            // TODO re-implement when Add instruction with an immediate value will be supported
            // buffer.put((byte) 0x83);
            // buffer.put(ModRM.builder().setMod(ModRM.MOD_REGISTER_TO_REGISTER).setReg(0b000).setRm(0b100)
            //         .byteValue());
            // buffer.put((byte) from.getStackSize());
        }
    }
}
