package ru.deewend.tennessinec;

public enum DataType {
    VOID("void", 0, false),
    CHAR("char", 1),
    INT("int", 4);

    private final String name;
    // private final String nameFirstCharacterUppercased;
    private final int size;
    private final boolean canBeUsedForVariableDefinition;

    DataType(String name, int size) {
        this(name, size, true);
    }

    DataType(String name, int size, boolean canBeUsedForVariableDefinition) {
        this.name = name;
        // this.nameFirstCharacterUppercased = Helper.uppercaseFirstCharacter(name);
        this.size = size;
        this.canBeUsedForVariableDefinition = canBeUsedForVariableDefinition;
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

    public boolean canBeUsedForVariableDefinition() {
        return canBeUsedForVariableDefinition;
    }
}
