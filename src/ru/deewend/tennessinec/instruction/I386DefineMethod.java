package ru.deewend.tennessinec.instruction;

import ru.deewend.tennessinec.Helper;
import ru.deewend.tennessinec.ModRM;
import ru.deewend.tennessinec.Pair;
import ru.deewend.tennessinec.Triple;
import ru.deewend.tennessinec.exporter.Exporter;

import java.nio.ByteBuffer;

@NotARealMachineInstruction
public class I386DefineMethod implements Instruction {
    private final Exporter exporter;
    private final int stackSize;

    public I386DefineMethod(Exporter exporter, Integer stackSize) {
        this.exporter = exporter;
        this.stackSize = stackSize;
    }

    @Override
    public void encode(ByteBuffer buffer) {
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
                .value(), stackSize)); // SUB ESP,<stackSize>
        exporter.encodeLastInstruction();
    }
}
