package ru.deewend.tennessinec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TMethod {
    private final String name;
    private final List<String> parameterTypes;

    private TMethod(String name, List<String> parameterTypes) {
        this.name = name;
        this.parameterTypes = new ArrayList<>(parameterTypes);
        for (String parameterType : parameterTypes) {
            if (!Helper.validateToken(parameterType, false)) {
                throw new IllegalArgumentException("Invalid parameter type: " + parameterType);
            }
        }
    }

    public static TMethod of(String name, List<String> parameterTypes) {
        return new TMethod(name, parameterTypes);
    }

    public String getName() {
        return name;
    }

    public List<String> getParameterTypes() {
        return Collections.unmodifiableList(parameterTypes);
    }
}
