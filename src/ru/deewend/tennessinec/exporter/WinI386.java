package ru.deewend.tennessinec.exporter;

import ru.deewend.tennessinec.*;
import ru.deewend.tennessinec.instruction.I386Label;
import ru.deewend.tennessinec.instruction.Instruction;

import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class WinI386 implements Exporter {
    public static final int IMAGE_BASE = 0x400000;
    public static final int SIZE_OF_IMAGE = 0x4000;
    public static final int SIZE_OF_HEADERS = 0x800;
    public static final int IMPORTS_VA = 0x2000;
    public static final int CODE_SECTION_START = 0x200;
    public static final int CODE_SECTION_VIRTUAL_ADDRESS = 0x1000;
    public static final int MAX_CODE_BYTES = SIZE_OF_HEADERS;
    public static final int IMPORTS_SECTION_START = CODE_SECTION_START + MAX_CODE_BYTES;
    public static final int MAX_IMPORTS_BYTES = SIZE_OF_HEADERS;
    public static final int DATA_SECTION_START = IMPORTS_SECTION_START + MAX_IMPORTS_BYTES;
    public static final int MAX_DATA_BYTES = SIZE_OF_HEADERS;
    public static final int DATA_SECTION_VIRTUAL_ADDRESS = 0x3000;

    public static final int ENCODING_PHASE_CALCULATING_LABEL_ADDRESSES = 1;
    public static final int ENCODING_PHASE_FINAL = 2;

    private static final String INSTRUCTION_CLASS_PREFIX = "ru.deewend.tennessinec.instruction.I386";

    private List<Instruction> instructionList = new ArrayList<>();
    private final List<byte[]> stringList = new LinkedList<>();
    private Metadata metadata;
    private int encodingPhase;
    private ByteBuffer buffer;
    private int currentInstructionIdx;

    @Override
    public void load(Metadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public void export(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // DOS header
        Helper.putString(buffer, "MZ");
        Helper.writeNullUntil(buffer, 0x30);
        buffer.putInt(0); // padding
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.putInt(0x40); // offset of PE header

        Helper.writeNullUntil(buffer, 0x40);

        // PE header
        // PE, 0, 0
        Helper.putString(buffer, "PE");
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        buffer.putShort((short) 0x14C); // Intel 386
        buffer.putShort((short) 3); // 3 sections in total
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.putShort((short) 0xE0);
        buffer.putShort((short) 0x102); // 32bit EXE

        Helper.writeNullUntil(buffer, 0x58);

        // Optional header
        buffer.putShort((short) 0x10B); // 32 bit
        buffer.putShort((short) 0);
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.putInt(0x1000); // RVA of entry point (to be modified later)
        buffer.putInt(0); // padding
        buffer.putInt(0);
        buffer.putInt(IMAGE_BASE); // ImageBase
        buffer.putInt(0x1000); // offset where sections should start
        buffer.putInt(0x200); // offset where sections start in the file
        buffer.putInt(0); // padding
        buffer.putInt(0);
        buffer.putShort((short) metadata.getMinNTVersion());
        buffer.putShort((short) 0); // padding
        buffer.putInt(0);
        buffer.putInt(SIZE_OF_IMAGE); // size of image
        buffer.putInt(SIZE_OF_HEADERS); // size of headers
        buffer.putInt(0);
        buffer.putShort((short) metadata.getSubsystem());
        buffer.putShort((short) 0); // padding
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.putInt(16); // count of data directories

        // Data directories
        buffer.putInt(0); // padding
        buffer.putInt(0);
        buffer.putInt(IMPORTS_VA); // ImportsVA
        buffer.putInt(0); // padding
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.putInt(0);

        Helper.writeNullUntil(buffer, 0x138);

        Helper.putString(buffer, ".text");
        buffer.put((byte) 0); // padding, to make the string contain 8 bytes in total
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        buffer.putInt(0x1000); // VirtualSize
        buffer.putInt(CODE_SECTION_VIRTUAL_ADDRESS); // VirtualAddress
        buffer.putInt(MAX_CODE_BYTES); // size of raw data
        buffer.putInt(CODE_SECTION_START); // pointer to raw data
        buffer.putInt(0); // padding
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.putInt(0x60000020); // CODE_EXECUTE_READ

        Helper.putString(buffer, ".rdata");
        buffer.put((byte) 0); // padding
        buffer.put((byte) 0);
        buffer.putInt(0x1000); // VirtualSize
        buffer.putInt(0x2000); // VirtualAddress
        buffer.putInt(MAX_IMPORTS_BYTES); // size of raw data
        buffer.putInt(IMPORTS_SECTION_START); // pointer to raw data
        buffer.putInt(0); // padding
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.putInt(0x40000040); // INITIALIZED_READ

        Helper.putString(buffer, ".data");
        buffer.put((byte) 0); // padding, to make the string contain 8 bytes in total
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        buffer.putInt(0x1000); // VirtualSize
        buffer.putInt(0x3000); // VirtualAddress
        buffer.putInt(MAX_DATA_BYTES); // size of raw data
        buffer.putInt(DATA_SECTION_START); // pointer to raw data
        buffer.putInt(0); // padding
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.putInt(0xC0000040); // DATA_READ_WRITE

        // imports

        Set<Pair<LibraryName, Set<TFunction>>> importsSet = metadata.importsSet();
        int functionCount = 0;
        int functionsLength = 0;
        for (Pair<LibraryName, Set<TFunction>> pair : importsSet) {
            Set<TFunction> externalFunctions = pair.getSecond();
            functionCount += externalFunctions.size();
            for (TFunction function : externalFunctions) {
                functionsLength += 2 + function.getName().length() + 1; // check encoding?
            }
        }
        int sectionSize = (functionCount + 1) * 20; // 20 = sizeof(int) * fieldsCount
        int functionPointersLength = functionCount * 8;
        sectionSize += functionPointersLength;
        sectionSize += functionsLength;
        sectionSize += functionPointersLength;
        for (Pair<LibraryName, Set<TFunction>> pair : importsSet) {
            LibraryName name = pair.getFirst();
            sectionSize += name.getName().length() + 1; // null terminator
        }
        ByteBuffer importsBuffer = ByteBuffer.allocate(sectionSize);
        importsBuffer.order(ByteOrder.LITTLE_ENDIAN);

        int currentPointer = sectionSize;
        Map<LibraryName, Integer> libraryNamePointers = new HashMap<>();
        for (Pair<LibraryName, Set<TFunction>> pair : importsSet) {
            LibraryName name = pair.getFirst();
            String uppercaseName = name.defaultCase();
            byte[] bytes = (uppercaseName + "\0").getBytes(StandardCharsets.US_ASCII);
            int length = bytes.length;

            currentPointer -= length;
            libraryNamePointers.put(name, currentPointer);

            importsBuffer.position(currentPointer);
            importsBuffer.put(bytes, 0, length);
        }
        currentPointer -= functionPointersLength; // we'll fill it later
        int thePointer0 = currentPointer;

        Map<Pair<LibraryName, TFunction>, Integer> functionNamePointers = new HashMap<>();
        boolean firstTime = true;
        int end = 0;
        int start = 0;
        int functionsWritten = 0;
        int pointerToCurrentPointerToFunctionName = currentPointer - functionsLength - 8;
        for (Pair<LibraryName, Set<TFunction>> pair : importsSet) {
            LibraryName libraryName = pair.getFirst();

            for (TFunction function : pair.getSecond()) {
                String name = function.getName();
                //               hint
                byte[] bytes = ("\0\0" + name + "\0").getBytes(StandardCharsets.US_ASCII);
                int length = bytes.length;

                currentPointer -= length;
                int payloadPointer = currentPointer;

                importsBuffer.position(payloadPointer);
                importsBuffer.put(bytes, 0, length);

                int thePointer = pointerToCurrentPointerToFunctionName - (8 * functionsWritten);
                importsBuffer.putLong(thePointer, (IMPORTS_VA + payloadPointer));
                if (firstTime) {
                    end = thePointer + 8;

                    firstTime = false;
                }
                start = thePointer;

                functionNamePointers.put(Pair.of(libraryName, function), thePointer);

                functionsWritten++;
            }
        }
        importsBuffer.position(0);

        for (Pair<LibraryName, Set<TFunction>> pair : importsSet) {
            LibraryName libraryName = pair.getFirst();

            for (TFunction function : pair.getSecond()) {
                int pointer1 = IMPORTS_VA + functionNamePointers.get(Pair.of(libraryName, function));
                int pointerToLibraryName = IMPORTS_VA + libraryNamePointers.get(libraryName);
                int pointer2 = pointer1 + functionsLength + functionPointersLength;

                importsBuffer.putInt(pointer1);
                importsBuffer.putInt(0);
                importsBuffer.putInt(0);
                importsBuffer.putInt(pointerToLibraryName);
                importsBuffer.putInt(pointer2);

                function.setVirtualAddress(IMAGE_BASE + pointer2);
            }
        }
        importsBuffer.putInt(0);
        importsBuffer.putInt(0);
        importsBuffer.putInt(0);
        importsBuffer.putInt(0);
        importsBuffer.putInt(0);

        importsBuffer.flip();
        byte[] wholeSection = importsBuffer.array();
        System.arraycopy(wholeSection, start, wholeSection, thePointer0, (end - start));

        Helper.writeNullUntil(buffer, CODE_SECTION_START);

        // FIXME (eventually). Encoding instructions twice just because of labels isn't a really nice solution
        List<Instruction> instructionListReference = instructionList;
        this.buffer = ByteBuffer.allocate(SIZE_OF_HEADERS);
        // we don't really need to specify endianness for this buffer
        instructionList = new ArrayList<>(instructionList);
        encodingPhase = ENCODING_PHASE_CALCULATING_LABEL_ADDRESSES;

        encodeInstructions();

        this.buffer = buffer;
        instructionList = instructionListReference;
        encodingPhase = ENCODING_PHASE_FINAL;

        encodeInstructions();

        // TODO likely is a subject for removal. If an overflow didn't occur with a temporary Buffer,
        //  it won't occur here
        checkOverflow(buffer.position() - CODE_SECTION_START);

        TFunction entryFunction = TFunction.lookupEntryFunction();
        int entryVirtualAddress = entryFunction.getVirtualAddress();
        if (entryVirtualAddress == Helper.UNINITIALIZED_VIRTUAL_ADDRESS) {
            throw new AssertionError("Virtual address of the entry function is uninitialized. " +
                    "Most likely a compiler bug");
        }
        buffer.putInt(0x68 /* 0x58 + 16 bytes */, entryVirtualAddress - IMAGE_BASE);

        Helper.writeNullUntil(buffer, IMPORTS_SECTION_START);

        checkOverflow(wholeSection.length);
        buffer.put(wholeSection);

        // strings
        Helper.writeNullUntil(buffer, DATA_SECTION_START);

        for (byte[] string : stringList) buffer.put(string);
        checkOverflow(buffer.position() - DATA_SECTION_START);
    }

    private void checkOverflow(int written) {
        if (written > SIZE_OF_HEADERS) {
            throw new RuntimeException("An overflow occurred when mounting a section, " +
                    "current size limit for all headers is hardcoded at 0x" + Integer.toHexString(SIZE_OF_HEADERS));
        }
    }

    private void encodeInstructions() {
        // don't replace with for each loop, ConcurrentModificationException might be thrown
        for (currentInstructionIdx = 0; currentInstructionIdx < instructionList.size(); currentInstructionIdx++) {
            Instruction instruction = instructionList.get(currentInstructionIdx);
            if (instruction instanceof I386Label) {
                if (encodingPhase == ENCODING_PHASE_CALCULATING_LABEL_ADDRESSES) {
                    ((I386Label) instruction).setVirtualAddress(currentVirtualAddress());
                }

                continue;
            }
            instruction.encode(buffer);
        }
    }

    @Override
    public int mountString(String str) {
        int address = IMAGE_BASE + DATA_SECTION_VIRTUAL_ADDRESS;
        for (byte[] string : stringList) {
            address += string.length;
        }
        stringList.add((str + "\0").getBytes(StandardCharsets.US_ASCII));

        return address;
    }

    @Override
    public void putInstruction(String name, Object parameter) {
        Helper.validateToken(name);

        Instruction instruction;
        try {
            Class<?> clazz = Class.forName(INSTRUCTION_CLASS_PREFIX + name);
            Constructor<?> constructor = clazz.getConstructor(Exporter.class, parameter.getClass());

            instruction = (Instruction) constructor.newInstance(this, parameter);
        } catch (Exception e) {
            throw new RuntimeException("Adding " + name + " instruction with parameter " + parameter, e);
        }

        instructionList.add(instruction);
    }

    @Override
    public void encodeLastInstruction() {
        checkBuffer();

        int idx = instructionList.size() - 1;
        Instruction instruction = instructionList.get(idx);
        instruction.encode(buffer);
        instructionList.remove(idx);
    }

    @Override
    public int searchLabelAndGetAddress(String name, boolean rightToLeft) {
        if (encodingPhase == ENCODING_PHASE_CALCULATING_LABEL_ADDRESSES && !rightToLeft) {
            throw new IllegalStateException("Left-to-right searching " +
                    "mode is unsupported during CALCULATING_LABEL_ADDRESSES phase");
        }
        I386Label result = null;
        if (rightToLeft) {
            for (int i = currentInstructionIdx - 1; i >= 0; i--) {
                result = checkLabel(instructionList.get(i), name);
                if (result != null) break;
            }
        } else {
            for (int i = currentInstructionIdx + 1; i < instructionList.size(); i++) {
                result = checkLabel(instructionList.get(i), name); // fixme don't duplicate code
                if (result != null) break;
            }
        }
        if (result == null) {
            throw new RuntimeException("Label \"" + name + "\" was not found " +
                    "in " + (rightToLeft ? "right-to-left" : "left-to-right") + " mode");
        }

        return result.getVirtualAddress();
    }

    private I386Label checkLabel(Instruction instruction, String labelName) {
        if (!(instruction instanceof I386Label)) return null;

        I386Label label = (I386Label) instruction;
        if (!label.getName().equals(labelName)) return null;

        return label;
    }

    @Override
    public int currentVirtualAddress() {
        checkBuffer();

        int offset = buffer.position() - (encodingPhase == ENCODING_PHASE_FINAL ? CODE_SECTION_START : 0);

        return IMAGE_BASE + CODE_SECTION_VIRTUAL_ADDRESS + offset;
    }

    @Override
    public int currentEncodingPhase() {
        return encodingPhase;
    }

    private void checkBuffer() {
        if (buffer == null) {
            throw new IllegalStateException("Instruction encoding hasn't started or already finished");
        }
    }

    @Override
    public int imageSize() {
        return SIZE_OF_IMAGE;
    }
}
