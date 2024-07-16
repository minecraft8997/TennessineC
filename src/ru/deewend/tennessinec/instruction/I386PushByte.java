package ru.deewend.tennessinec.instruction;

import ru.deewend.tennessinec.exporter.Exporter;

@Alias
public class I386PushByte extends I386PushWord8 {
    public I386PushByte(Exporter exporter, Integer value) {
        super(exporter, value);
    }
}
