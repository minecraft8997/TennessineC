package ru.deewend.tennessinec;

import ru.deewend.tennessinec.exporter.WinI386;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;

public class Test {
    public static void main(String[] args) throws IOException {
        WinI386 exporter = new WinI386();
        File file = new File("test.exe");
        file.createNewFile();
        try (DataOutputStream stream = new DataOutputStream(Files.newOutputStream(file.toPath()))) {
            ByteBuffer buffer = ByteBuffer.allocate(65536);
            exporter.load(new Metadata());
            exporter.export(buffer);
            int position = buffer.position();
            buffer.flip();
            stream.write(buffer.array(), 0, position);
            for (; position < 16384; position++) stream.write(0);
        }
    }
}
