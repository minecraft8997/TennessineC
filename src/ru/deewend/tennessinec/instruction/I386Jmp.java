package ru.deewend.tennessinec.instruction;

import ru.deewend.tennessinec.Pair;
import ru.deewend.tennessinec.exporter.Exporter;
import ru.deewend.tennessinec.exporter.WinI386;

import java.nio.ByteBuffer;

public class I386Jmp implements JInstruction {
    private final Exporter exporter;
    private boolean jz;
    private int offset;
    private String label;
    private boolean searchRightToLeft;

    public I386Jmp(Exporter exporter, Integer offset) {
        this.exporter = exporter;
        this.offset = offset;
    }

    public I386Jmp(Exporter exporter, Pair<String, Boolean> labelData) {
        this.exporter = exporter;
        label = labelData.getFirst();
        searchRightToLeft = labelData.getSecond();
    }

    public void setJz() {
        this.jz = true;
    }

    @Override
    public void setOffset(int offset) {
        this.offset = offset;
    }

    @Override
    public void encode(ByteBuffer buffer) {
        if (jz) {
            buffer.putShort((short) 0x840F);
        } else {
            buffer.put((byte) 0xE9);
        }
        if (label != null && exporter.currentEncodingPhase() == WinI386.ENCODING_PHASE_FINAL) {
            int virtualAddress = exporter.searchLabelAndGetAddress(label, searchRightToLeft);
            int currentVirtualAddress = exporter.currentVirtualAddress() + 4;
            offset = virtualAddress - currentVirtualAddress;
        }
        buffer.putInt(offset);
    }
}
