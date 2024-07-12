package ru.deewend.tennessinec.instruction;

import ru.deewend.tennessinec.exporter.Exporter;

public class I386PushChar extends I386PushWord8 {
    public I386PushChar(Exporter exporter, Integer value) {
        super(exporter, value);
    }
}
