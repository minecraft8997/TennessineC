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
    public static final Object EMPTY_PARAMETER = new Object();
    public static final int SKIP_PARAMETER = Integer.MAX_VALUE;

    private static final Map<String, Exporter> EXPORTER_CACHE = new HashMap<>();

    private Helper() {
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean validateToken(String token) {
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

    public static List<List<String>> tokenize(InputStream sourceStream) {
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
            tokenizedLines.add(Tokenizer.getInstance().tokenizeLine(sourceLines.get(i), i));
        }

        return tokenizedLines;
    }

    public static void crash(String message) {
        if (message == null || message.isEmpty()) message = "<no further information>";

        System.err.println("Crash: " + message);

        System.exit(-1);
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public static Exporter getExporter(String name) {
        Objects.requireNonNull(name);

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

    public static void putString(ByteBuffer buffer, String str) {
        buffer.put(str.getBytes(StandardCharsets.US_ASCII));
    }

    public static void writeNullUntil(ByteBuffer buffer, int offset) {
        int written = buffer.position();
        for (int i = 0; i < offset - written; i++) buffer.put((byte) 0x00);
    }
}
