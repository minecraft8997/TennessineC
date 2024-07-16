package ru.deewend.tennessinec;

public class ModRM {
    public static final int MOD_NO_DISPLACEMENT = 0b00;
    public static final int MOD_1_BYTE_DISPLACEMENT = 0b01;
    public static final int MOD_4_BYTE_DISPLACEMENT = 0b10;
    public static final int MOD_REGISTER_TO_REGISTER = 0b11;

    public static final int REG_EAX = 0b000;
    public static final int REG_EBX = 0b011;
    public static final int REG_ECX = 0b001;
    public static final int REG_EDX = 0b010;
    public static final int REG_EBP = 0b100;
    public static final int REG_ESP = 0b101;

    public static final int RM_CONSTANT = 0b100;

    private int mod;
    private int reg;
    private int rm;

    private ModRM() {
    }

    public static ModRM builder() {
        return new ModRM();
    }

    public ModRM setMod(int mod) {
        this.mod = mod;

        return this;
    }

    public ModRM setReg(int reg) {
        this.reg = reg;

        return this;
    }

    public ModRM setRm(int rm) {
        this.rm = rm;

        return this;
    }

    public int value() {
        int result = 0;
        result |= (mod << 6);
        result |= (reg << 3);
        result |= rm;

        return result;
    }

    public byte byteValue() {
        return (byte) value();
    }
}