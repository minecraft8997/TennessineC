package ru.deewend.tennessinec.instruction;

import ru.deewend.tennessinec.Pair;
import ru.deewend.tennessinec.exporter.Exporter;

public class I386Add extends I386Sub {
    public I386Add(Exporter exporter, Pair<Integer, Integer> parameters) {
        super(exporter, parameters);

        setAdd();
    }
}
