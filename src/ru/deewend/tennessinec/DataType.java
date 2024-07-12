package ru.deewend.tennessinec;

import ru.deewend.tennessinec.exporter.Exporter;

public enum DataType {
    CHAR("char", 1), INT("int", 4);

    private final String name;
    private final String nameFirstCharacterUppercased;
    private final int size;

    DataType(String name, int size) {
        this.name = name;
        this.nameFirstCharacterUppercased = Helper.uppercaseFirstCharacter(name);
        this.size = size;
    }

    public static DataType recognizeDataType(String token) {
        for (DataType type : DataType.values()) {
            if (type.getName().equals(token)) return type;
        }

        return null;
    }

    public String getName() {
        return name;
    }

    public int getSize() {
        return size;
    }

    public void push(Exporter exporter, Object value) {
        exporter.putInstruction("Push" + nameFirstCharacterUppercased, value);
    }
}
