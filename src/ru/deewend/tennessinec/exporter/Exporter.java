package ru.deewend.tennessinec.exporter;

import ru.deewend.tennessinec.Metadata;

import java.nio.ByteBuffer;

public interface Exporter {
    void load(Metadata instructions);
    void export(ByteBuffer stream);
    int mountString(String str);
    void putInstruction(String name, Object parameter);
    int getMethodVirtualAddress(String name);
    int imageSize();
}
