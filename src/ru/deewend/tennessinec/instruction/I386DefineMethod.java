package ru.deewend.tennessinec.instruction;

import ru.deewend.tennessinec.ModRM;
import ru.deewend.tennessinec.Pair;
import ru.deewend.tennessinec.exporter.Exporter;

import java.nio.ByteBuffer;

public class I386DefineMethod implements Instruction {
    private final Exporter exporter;
    private final int stackSize;

    public I386DefineMethod(Exporter exporter, Integer stackSize) {
        this.exporter = exporter;
        this.stackSize = stackSize;
    }

    @Override
    public void encode(ByteBuffer buffer) {
        exporter.putInstruction("I386PushEBP", null);
        exporter.putInstruction("I386Mov", ModRM.builder()
                .setMod(ModRM.MOD_REGISTER_TO_REGISTER)
                .setReg(ModRM.REG_EBP)
                .setRm(ModRM.REG_ESP)
                .value()); // MOV EBP,ESP
        exporter.putInstruction("I386Sub", Pair.of(ModRM.builder()
                .setMod(ModRM.MOD_REGISTER_TO_REGISTER)
                .setReg(ModRM.REG_ESP)
                .setRm(ModRM.RM_CONSTANT)
                .value(), stackSize)); // SUB ESP,<stackSize>
    }
}
