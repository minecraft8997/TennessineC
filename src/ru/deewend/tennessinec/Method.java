package ru.deewend.tennessinec;

import java.util.ArrayList;
import java.util.List;

public class Method {
    private final String name;
    private final List<String> parameterTypes;

    protected Method(String name, List<String> parameterTypes) {
        this.name = name;
        this.parameterTypes = new ArrayList<>(parameterTypes);
    }


}
