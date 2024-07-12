package ru.deewend.tennessinec.instruction;

import ru.deewend.tennessinec.exporter.Exporter;

public class I386PushInt extends I386PushWord32 {
    public I386PushInt(Exporter exporter, Integer address) {
        super(exporter, address);
    }
}
