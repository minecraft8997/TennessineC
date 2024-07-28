package ru.deewend.tennessinec.exporter;

import ru.deewend.tennessinec.Metadata;

import java.nio.ByteBuffer;

public interface Exporter {
    void load(Metadata instructions);
    void export(ByteBuffer stream);
    int mountString(String str);
    void putInstruction(String name, Object parameter);
    /*
     * Note that calling this method will result the instruction
     * being removed from the queue. (We don't want to encode it twice.)
     */
    void encodeLastInstruction();
    int imageSize();
}
