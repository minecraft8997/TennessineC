package ru.deewend.tennessinec.instruction;

import java.nio.ByteBuffer;

public interface Instruction {
    void encode(ByteBuffer buffer);
}
