package ru.deewend.tennessinec;

public class VariableData {
    private final DataType type;
    private int stackOffset;

    private VariableData(DataType type) {
        this.type = type;
    }

    public static VariableData of(DataType type) {
        return new VariableData(type);
    }

    public DataType getType() {
        return type;
    }

    public int getStackOffset() {
        return stackOffset;
    }

    public void setStackOffset(int stackOffset) {
        this.stackOffset = stackOffset;
    }
}
