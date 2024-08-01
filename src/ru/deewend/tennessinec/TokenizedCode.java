package ru.deewend.tennessinec;

import java.util.ArrayList;
import java.util.List;

public class TokenizedCode {
    public interface Detector {
        default boolean detect(String token) {
            if (token.isEmpty()) throw new IllegalArgumentException("Empty token");

            return detect0(token, token.charAt(0));
        }

        boolean detect0(String token, char firstChar);
    }

    public enum TokenType {
        SYMBOL((token, firstChar) -> !Tokenizer.isDigit(firstChar) && Tokenizer.isLetter(firstChar)),
        LITERAL_INTEGER((token, firstChar) -> firstChar >= '0' && firstChar <= '9'),
        LITERAL_STRING((token, firstChar) -> firstChar == '"'),
        LITERAL_CHARACTER((token, firstChar) -> firstChar == '\''),
        OPERATOR_MATH(((token, firstChar) -> "+-/*".indexOf(firstChar) != -1)),
        OTHER((token, firstChar) -> "#(){}.,=\\".indexOf(firstChar) != -1),
        STATEMENT_END((token, firstChar) -> firstChar == ';');

        private final Detector detector;

        TokenType(Detector detector) {
            this.detector = detector;
        }

        boolean detect(String token) {
            return detector.detect(token);
        }
    }

    private final List<TokenizedLine> tokenizedLines;
    private int lineIdx;
    private int nextTokenIdx;

    private TokenizedCode(List<List<String>> tokenizedLines, String sourceFilename) {
        this.tokenizedLines = new ArrayList<>(tokenizedLines.size());
        for (int i = 0; i < tokenizedLines.size(); i++) {
            List<String> line = tokenizedLines.get(i);
            this.tokenizedLines.add(TokenizedLine.of(sourceFilename, i, line));
        }
    }

    public static TokenizedCode of(List<List<String>> tokenizedLines, String sourceFilename) {
        return new TokenizedCode(tokenizedLines, sourceFilename);
    }

    public void switchToLine(int lineIdx) {
        if (lineIdx < 0 || lineIdx >= tokenizedLines.size()) issue("lineIdx out of bounds");

        this.lineIdx = lineIdx;
        nextTokenIdx = 0;
    }

    public List<String> getLine() {
        return tokenizedLines.get(lineIdx).getLineUnmodifiable();
    }

    public void insertFirstLine(TokenizedLine tokenizedLine) {
        tokenizedLines.add(0, tokenizedLine);
        lineIdx++;
    }

    public boolean hasMoreTokens() {
        return nextTokenIdx < tokenizedLines.get(lineIdx).tokenCount();
    }

    // note that this moves the cursor
    public TokenType getNextTokenTypeOmitComma(boolean strict) {
        String nextToken;
        if (!(nextToken = nextToken()).equals(",")) {
            if (!strict) return detectTokenType(nextToken);

            issue("expected a comma, found: " + nextToken);
        }

        return getNextTokenType();
    }

    public TokenType getNextTokenType() {
        String nextToken = nextToken(false);

        return detectTokenType(nextToken);
    }

    private TokenType detectTokenType(String token) {
        for (TokenType possibleType : TokenType.values()) {
            if (possibleType.detect(token)) return possibleType;
        }
        issue("could not determine the next token type"); // could probably return OTHER instead?

        return null; // unreachable statement
    }

    public String currentToken() {
        if (nextTokenIdx == 0) issue("nextToken(true) has not been called");

        List<String> tokens = tokenizedLines.get(lineIdx).getLineUnmodifiable();

        return tokens.get(nextTokenIdx - 1);
    }

    public String nextToken() {
        return nextToken(true);
    }

    public String nextToken(boolean moveCursor) {
        List<String> tokens = tokenizedLines.get(lineIdx).getLineUnmodifiable();
        if (nextTokenIdx >= tokens.size()) issue("too few tokens");

        String result = tokens.get(nextTokenIdx);
        if (moveCursor) nextTokenIdx++;

        return result;
    }

    public void patchToken(String newValue) {
        if (nextTokenIdx == 0) issue("nextToken has never been called");

        List<String> tokens = tokenizedLines.get(lineIdx).getLine();
        if (!newValue.isEmpty()) {
            tokens.set(nextTokenIdx - 1, newValue);
        } else {
            tokens.remove(nextTokenIdx - 1);
            nextTokenIdx--;
        }
    }

    public void insertToken(String newValue) {
        List<String> tokens = tokenizedLines.get(lineIdx).getLine();
        if (nextTokenIdx < 0 || nextTokenIdx > tokens.size() /* not >= */) issue("nextTokenIdx: " + nextTokenIdx);

        tokens.add(nextTokenIdx++, newValue);
    }

    public void removeLine(int lineIdx) {
        if (lineIdx < 0 || lineIdx >= tokenizedLines.size()) issue("lineIdx out of bounds: " + lineIdx);

        tokenizedLines.remove(lineIdx);
        if (lineIdx == this.lineIdx) {
            this.lineIdx = 0;
            nextTokenIdx = 0;
        } else if (lineIdx < this.lineIdx) {
            this.lineIdx--;
        }
    }

    public int linesCount() {
        return tokenizedLines.size();
    }

    public void warning(String message) {
        issue(message, false, true);
    }

    public void issue(String message) {
        issue(message, false, false);
    }

    public void issue(String message, boolean fromUncaughtExceptionHandler) {
        issue(message, fromUncaughtExceptionHandler, false);
    }

    public void issue(String message, boolean fromUncaughtExceptionHandler, boolean warning) {
        TokenizedLine line = tokenizedLines.get(lineIdx);
        String errorMessage = line.getSourceFilename() + " at line " + line.getOriginalLineNumber() + ": " + message;

        if (fromUncaughtExceptionHandler || warning) {
            if (warning) System.err.print("[Warning] ");
            System.err.println(errorMessage);

            return;
        }

        throw new IllegalArgumentException(errorMessage);
    }
}
