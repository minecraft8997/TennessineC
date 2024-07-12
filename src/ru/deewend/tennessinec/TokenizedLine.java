package ru.deewend.tennessinec;

import java.util.Collections;
import java.util.List;

public class TokenizedLine {
    private final String sourceFilename;
    private final int originalLineNumber;
    private final List<String> tokenizedLine;

    private TokenizedLine(String sourceFilename, int lineIdx, List<String> tokenizedLine) {
        this.sourceFilename = sourceFilename;
        this.originalLineNumber = lineIdx + 1;
        this.tokenizedLine = tokenizedLine;
    }

    public static TokenizedLine of(String sourceFilename, int lineIdx, List<String> tokenizedLine) {
        return new TokenizedLine(sourceFilename, lineIdx, tokenizedLine);
    }

    public String getSourceFilename() {
        return sourceFilename;
    }

    public int getOriginalLineNumber() {
        return originalLineNumber;
    }

    public int tokenCount() {
        return tokenizedLine.size();
    }

    List<String> getLine() {
        return tokenizedLine;
    }

    public List<String> getLineUnmodifiable() {
        return Collections.unmodifiableList(tokenizedLine);
    }
}
