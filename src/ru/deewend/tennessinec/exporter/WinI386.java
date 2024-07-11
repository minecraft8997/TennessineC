package ru.deewend.tennessinec.exporter;

import ru.deewend.tennessinec.*;
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
    public static final int MAX_CODE_BYTES = SIZE_OF_HEADERS;
    public static final int IMPORTS_SECTION_START = CODE_SECTION_START + MAX_CODE_BYTES;
    public static final int MAX_IMPORTS_BYTES = SIZE_OF_HEADERS;
    public static final int DATA_SECTION_START = IMPORTS_SECTION_START + MAX_IMPORTS_BYTES;
    public static final int MAX_DATA_BYTES = SIZE_OF_HEADERS;
    public static final int DATA_SECTION_VIRTUAL_ADDRESS = 0x3000;

    private final List<Instruction> instructionList = new ArrayList<>();
    private final List<byte[]> stringList = new LinkedList<>();
    private final Map<String, Integer> externalMethodAddresses = new HashMap<>();
    private Metadata metadata;

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
        buffer.putShort((short) 0); // padding
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.putInt(0x1000); // RVA of entry point
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
        buffer.putInt(0x1000); // VirtualAddress
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

        Set<Pair<LibraryName, Set<ExternalMethod>>> importsSet = metadata.importsSet();
        int methodCount = 0;
        int methodsLength = 0;
        for (Pair<LibraryName, Set<ExternalMethod>> pair : importsSet) {
            Set<ExternalMethod> externalMethods = pair.getSecond();
            methodCount += externalMethods.size();
            for (ExternalMethod method : externalMethods) {
                methodsLength += 2 + method.getName().length() + 1; // check encoding?
            }
        }
        int sectionSize = (methodCount + 1) * 20; // 20 = sizeof(int) * fieldsCount
        int methodPointersLength = methodCount * 8;
        sectionSize += methodPointersLength;
        sectionSize += methodsLength;
        sectionSize += methodPointersLength;
        for (Pair<LibraryName, Set<ExternalMethod>> pair : importsSet) {
            LibraryName name = pair.getFirst();
            sectionSize += name.getName().length() + 1; // null terminator
        }
        ByteBuffer importsBuffer = ByteBuffer.allocate(sectionSize);
        importsBuffer.order(ByteOrder.LITTLE_ENDIAN);

        int currentPointer = sectionSize;
        Map<LibraryName, Integer> libraryNamePointers = new HashMap<>();
        for (Pair<LibraryName, Set<ExternalMethod>> pair : importsSet) {
            LibraryName name = pair.getFirst();
            String uppercaseName = name.getUppercaseName();
            byte[] bytes = (uppercaseName + "\0").getBytes(StandardCharsets.US_ASCII);
            int length = bytes.length;

            currentPointer -= length;
            libraryNamePointers.put(name, currentPointer);

            importsBuffer.position(currentPointer);
            importsBuffer.put(bytes, 0, length);
        }
        currentPointer -= methodPointersLength; // we'll fill it later
        int thePointer0 = currentPointer;

        Map<Pair<LibraryName, ExternalMethod>, Integer> methodNamePointers = new HashMap<>();
        boolean firstTime = true;
        int end = 0;
        int start = 0;
        int methodsWritten = 0;
        int pointerToCurrentPointerToMethodName = currentPointer - methodsLength - 8;
        for (Pair<LibraryName, Set<ExternalMethod>> pair : importsSet) {
            LibraryName libraryName = pair.getFirst();

            for (ExternalMethod method : pair.getSecond()) {
                String name = method.getName();
                //               hint
                byte[] bytes = ("\0\0" + name + "\0").getBytes(StandardCharsets.US_ASCII);
                int length = bytes.length;

                currentPointer -= length;
                int payloadPointer = currentPointer;

                importsBuffer.position(payloadPointer);
                importsBuffer.put(bytes, 0, length);

                int thePointer = pointerToCurrentPointerToMethodName - (8 * methodsWritten);
                importsBuffer.putLong(thePointer, (IMPORTS_VA + payloadPointer));
                if (firstTime) {
                    end = thePointer + 8;

                    firstTime = false;
                }
                start = thePointer;

                methodNamePointers.put(Pair.of(libraryName, method), thePointer);

                methodsWritten++;
            }
        }
        importsBuffer.position(0);

        for (Pair<LibraryName, Set<ExternalMethod>> pair : importsSet) {
            LibraryName libraryName = pair.getFirst();

            for (ExternalMethod method : pair.getSecond()) {
                int pointer1 = IMPORTS_VA + methodNamePointers.get(Pair.of(libraryName, method));
                int pointerToLibraryName = IMPORTS_VA + libraryNamePointers.get(libraryName);
                int pointer2 = pointer1 + methodsLength + methodPointersLength;

                importsBuffer.putInt(pointer1);
                importsBuffer.putInt(0);
                importsBuffer.putInt(0);
                importsBuffer.putInt(pointerToLibraryName);
                importsBuffer.putInt(pointer2);

                // conflicts are possible here, since two different libraries can provide a method with
                // the same name (?). Note that internal map methodNamePointers is already designed against this issue
                externalMethodAddresses.put(method.getName(), (IMAGE_BASE + pointer2));
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

        for (Instruction instruction : instructionList) instruction.encode(buffer);
        checkOverflow(buffer.position() - CODE_SECTION_START);

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
        Instruction instruction;
        try {
            Class<?> clazz = Class.forName("ru.deewend.tennessinec.instruction.I386" + name);
            Constructor<?> constructor = clazz.getConstructor(Exporter.class, parameter.getClass());

            instruction = (Instruction) constructor.newInstance(this, parameter);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        instructionList.add(instruction);
    }

    @Override
    public int getExternalMethodVirtualAddress(String name) {
        Integer virtualAddress = externalMethodAddresses.get(name);
        if (virtualAddress == null) throw new IllegalArgumentException("Unknown method");

        return virtualAddress;
    }

    @Override
    public int imageSize() {
        return SIZE_OF_IMAGE;
    }
}
