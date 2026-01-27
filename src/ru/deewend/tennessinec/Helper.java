package ru.deewend.tennessinec;

import ru.deewend.tennessinec.exporter.Exporter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Helper {
    public enum MovType {
        REG_TO_REG_OR_REG_TO_MEM,
        MEM_TO_REG_OR_MEM_TO_MEM
    }

    public static final Object NOTHING = new Object();
    public static final int UNINITIALIZED_VIRTUAL_ADDRESS = 0;
    public static final int SKIP_PARAMETER = Integer.MAX_VALUE;

    private static final Map<String, Exporter> EXPORTER_CACHE = new HashMap<>();

    private Helper() {
    }

    public static void validateToken(String token) {
        validateToken(token, true);
    }

    public static boolean validateToken(String token, boolean throwException) {
        if (token.isEmpty()) {
            return false;
        }
        for (int i = 0; i < token.length(); i++) {
            char current = token.charAt(i);
            if (current == '_') {
                continue;
            }
            if (current >= '0' && current <= '9') {
                continue;
            }
            if ((current >= 'a' && current <= 'z') || (current >= 'A' && current <= 'Z')) {
                continue;
            }

            if (throwException) throw new IllegalArgumentException("Bad token: " + token);

            return false;
        }

        return true;
    }

    public static String uppercaseFirstCharacter(String str) {
        if (str.isEmpty()) return str;

        char firstCharUppercase = Character.toUpperCase(str.charAt(0));

        return (firstCharUppercase + str.substring(1));
    }

    public static String stringTokenToString(String token) {
        token = token.substring(1, token.length() - 1);
        token = token
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\0", "\0")
                .replace("\\b", "\b")
                .replace("\\n", "\n")
                .replace("\\t", "\t");

        return token;
    }

    public static <T> List<T> uniformList(int size, T element) {
        List<T> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) result.add(element);

        return result;
    }

    public static List<List<String>> tokenize(InputStream sourceStream, String sourceFilename) {
        //noinspection ExtractMethodRecommender
        List<String> sourceLines = new ArrayList<>();
        try {
            // assuming sourceStream will be closed by the caller
            // both BufferedStream and InputStreamReader themselves don't hold any native resources, not closing them
            BufferedReader reader = new BufferedReader(new InputStreamReader(sourceStream));
            String line;
            while ((line = reader.readLine()) != null) sourceLines.add(line);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<List<String>> tokenizedLines = new ArrayList<>();
        for (int i = 0; i < sourceLines.size(); i++) {
            tokenizedLines.add(Tokenizer.getInstance().tokenizeLine(sourceLines.get(i), i, sourceFilename));
        }
        if (Tokenizer.getInstance().noticedMultilineComment) {
            Tokenizer.getInstance().issue("unterminated multiline comment");
        }

        return tokenizedLines;
    }

    public static void crash(String message) {
        if (message == null || message.isEmpty()) message = "<no further information>";

        System.err.println("Crash: " + message);

        System.exit(-1);
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public static Exporter getExporter(String name, boolean shouldValidateName) {
        Objects.requireNonNull(name);

        if (shouldValidateName) Helper.validateToken(name);

        if (EXPORTER_CACHE.containsKey(name)) {
            return EXPORTER_CACHE.get(name);
        }

        Exporter instance;
        try {
            Class<?> clazz = Class.forName("ru.deewend.tennessinec.exporter." + name);
            instance = (Exporter) clazz.newInstance();
        } catch (ClassNotFoundException e) {
            System.err.println("Unknown exporter");

            return null;
        } catch (Exception e) {
            System.err.println("Could not instantiate an Exporter:");
            e.printStackTrace();

            return null;
        }
        EXPORTER_CACHE.put(name, instance);

        return instance;
    }

    private static void moveVariable(Exporter exporter, MovType mode, int register, VariableData data) {
        int stackOffset = data.getStackOffset();

        exporter.putInstruction("Mov", Triple.of(mode, ModRM.builder()
                .setMod(stackOffset < 0 ? ModRM.MOD_4_BYTE_DISPLACEMENT : ModRM.MOD_1_BYTE_DISPLACEMENT)
                .setReg(register)
                .setRm(0b101) // EBP + disp8/32 (?)
                .value(), stackOffset));
    }

    public static void moveFromRegToMem(Exporter exporter, int register, VariableData data) {
        // MOV dword ptr [EBP + stackOffset],EAX
        moveVariable(exporter, MovType.REG_TO_REG_OR_REG_TO_MEM, register, data);
    }

    public static void moveFromMemToReg(Exporter exporter, int register, VariableData data) {
        // MOV EAX,dword ptr [EBP + stackOffset]
        moveVariable(exporter, MovType.MEM_TO_REG_OR_MEM_TO_MEM, register, data);
    }

    public static void putString(ByteBuffer buffer, String str) {
        buffer.put(str.getBytes(StandardCharsets.US_ASCII));
    }

    public static void writeNullUntil(ByteBuffer buffer, int offset) {
        int written = buffer.position();
        for (int i = 0; i < offset - written; i++) buffer.put((byte) 0x00);
    }
}
