package ru.deewend.tennessinec.instruction;

import ru.deewend.tennessinec.exporter.Exporter;

@Alias
public class I386PushPointer extends I386PushWord32 {
    public I386PushPointer(Exporter exporter, Integer address) {
        super(exporter, address);
    }
}
