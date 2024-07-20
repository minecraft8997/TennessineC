package ru.deewend.tennessinec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TMethod {
    /*
    public interface Encoder {
        int encode(Exporter exporter);
    }

    public enum Type {
        BYTE,
        POINTER;

        private final Encoder size;
        private final Encoder pushEncoder;

        Type(Encoder pushEncoder) {
            this.pushEncoder = pushEncoder;
        }

        public int getPushInstruction(Exporter exporter) {
            return pushEncoder.encode(exporter);
        }
    }
     */

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
