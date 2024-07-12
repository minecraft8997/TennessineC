package ru.deewend.tennessinec;

public class VariableData {
    public static final int UNINITIALIZED = -1;

    private final DataType type;
    private int stackOffset = UNINITIALIZED;

    private VariableData(DataType type) {
        this.type = type;
    }

    public static VariableData of(DataType type) {
        return new VariableData(type);
    }

    public void calculateStackOffset(TennessineC compiler) {
        int stackOffset = 0;
        for (VariableData variableData : compiler.variableMap.values()) {
            if (variableData.stackOffset == UNINITIALIZED) continue;

            stackOffset += variableData.type.getSize();
        }

        this.stackOffset = stackOffset;
    }

    public int getStackOffset() {
        return stackOffset;
    }

    public DataType getType() {
        return type;
    }
}
