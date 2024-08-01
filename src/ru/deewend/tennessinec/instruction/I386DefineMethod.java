package ru.deewend.tennessinec.instruction;

import ru.deewend.tennessinec.*;
import ru.deewend.tennessinec.exporter.Exporter;
import ru.deewend.tennessinec.exporter.WinI386;

import java.nio.ByteBuffer;

@NotARealMachineInstruction
public class I386DefineMethod implements Instruction {
    private final Exporter exporter;
    private final TMethod method;

    public I386DefineMethod(Exporter exporter, TMethod method) {
        this.exporter = exporter;
        this.method = method;
    }

    @Override
    public void encode(ByteBuffer buffer) {
        int virtualAddress = exporter.currentVirtualAddress();

        exporter.putInstruction("PushEBP", Helper.NOTHING);
        exporter.encodeLastInstruction();
        exporter.putInstruction("Mov", Triple.of(Helper.MovType.REG_TO_REG_OR_REG_TO_MEM, ModRM.builder()
                .setMod(ModRM.MOD_REGISTER_TO_REGISTER)
                .setReg(ModRM.REG_EBP)
                .setRm(ModRM.REG_ESP)
                .value(), Helper.SKIP_PARAMETER)); // MOV EBP,ESP
        exporter.encodeLastInstruction();
        exporter.putInstruction("Sub", Pair.of(ModRM.builder()
                .setMod(ModRM.MOD_REGISTER_TO_REGISTER)
                .setReg(ModRM.REG_ESP)
                .setRm(ModRM.RM_CONSTANT)
                .value(), method.getStackSize())); // SUB ESP,<stackSize>
        exporter.encodeLastInstruction();

        method.setVirtualAddress(virtualAddress);
    }
}
