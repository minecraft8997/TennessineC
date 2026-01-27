package ru.deewend.tennessinec.instruction;

import ru.deewend.tennessinec.Pair;
import ru.deewend.tennessinec.exporter.Exporter;

public class I386Jz extends I386Jmp {
    public I386Jz(Exporter exporter, Integer offset) {
        super(exporter, offset);

        setJz();
    }

    public I386Jz(Exporter exporter, Pair<String, Boolean> labelData) {
        super(exporter, labelData);

        setJz();
    }
}
