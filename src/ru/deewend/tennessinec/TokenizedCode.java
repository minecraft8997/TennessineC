package ru.deewend.tennessinec;

import java.util.Collections;
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
        SYMBOL((token, firstChar) -> !Character.isDigit(firstChar) && Character.isLetter(firstChar)),
        LITERAL_INTEGER((token, firstChar) -> Character.isDigit(firstChar)),
        LITERAL_STRING((token, firstChar) -> firstChar == '"'),
        LITERAL_CHARACTER((token, firstChar) -> firstChar == '\''),
        OPERATOR_MATH(((token, firstChar) -> "+-/*".indexOf(firstChar) != -1)),
        OTHER((token, firstChar) -> "#(){},=\\".indexOf(firstChar) != -1),
        STATEMENT_END((token, firstChar) -> firstChar == ';');

        private final Detector detector;

        TokenType(Detector detector) {
            this.detector = detector;
        }

        boolean detect(String token) {
            return detector.detect(token);
        }
    }

    /*
     * TODO Probably worth doing something like List<Triple<String, Integer, List<String>>> where a Triple contains:
     *  1) String - the source file from where this line was taken;
     *  2) Integer - the original line number;
     *  3) List<String> - the line itself (= list of tokens).
     *  This will allow us not to report an "Internal line" in case of an error, which is literally not helpful at all.
     */
    private final List<List<String>> tokenizedLines;
    private int lineIdx;
    private int nextTokenIdx;
    private boolean linesCountModified;

    private TokenizedCode(List<List<String>> tokenizedLines) {
        this.tokenizedLines = tokenizedLines;
    }

    public static TokenizedCode of(List<List<String>> tokenizedLines) {
        return new TokenizedCode(tokenizedLines);
    }

    public void switchToLine(int lineIdx) {
        if (lineIdx < 0 || lineIdx >= tokenizedLines.size()) issue("lineIdx out of bounds");

        this.lineIdx = lineIdx;
        nextTokenIdx = 0;
    }

    public List<String> getLine() {
        return Collections.unmodifiableList(tokenizedLines.get(lineIdx));
    }

    public void insertFirstLine(List<String> tokenizedLine) {
        tokenizedLines.add(0, tokenizedLine);
        linesCountModified = true;
        lineIdx++;
    }

    public boolean hasMoreTokens() {
        return nextTokenIdx < tokenizedLines.get(lineIdx).size();
    }

    // note that this moves the cursor
    public TokenType getNextTokenTypeOmitComma(boolean strict) {
        String nextToken;
        if (!(nextToken = nextToken()).equals(",")) {
            if (!strict) return detectTokenType(nextToken);

            issue("expected a comma");
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

        List<String> tokens = tokenizedLines.get(lineIdx);

        return tokens.get(nextTokenIdx - 1);
    }

    public String nextToken() {
        return nextToken(true);
    }

    public String nextToken(boolean moveCursor) {
        List<String> tokens = tokenizedLines.get(lineIdx);
        if (nextTokenIdx >= tokens.size()) issue("too few tokens");

        String result = tokens.get(nextTokenIdx);
        if (moveCursor) nextTokenIdx++;

        return result;
    }

    public void patchToken(String newValue) {
        if (nextTokenIdx == 0) issue("nextToken has never been called");

        List<String> tokens = tokenizedLines.get(lineIdx);
        if (!newValue.isEmpty()) {
            tokens.set(nextTokenIdx - 1, newValue);
        } else {
            tokens.remove(nextTokenIdx - 1);
            nextTokenIdx--;
        }
    }

    public void insertToken(String newValue) {
        List<String> tokens = tokenizedLines.get(lineIdx);
        if (nextTokenIdx < 0 || nextTokenIdx > tokens.size() /* not >= */) issue("nextTokenIdx: " + nextTokenIdx);

        tokens.add(nextTokenIdx++, newValue);
    }

    public void removeLine(int lineIdx) {
        if (lineIdx < 0 || lineIdx >= tokenizedLines.size()) issue("lineIdx out of bounds: " + lineIdx);

        tokenizedLines.remove(lineIdx);
        linesCountModified = true;
        if (lineIdx == this.lineIdx) {
            this.lineIdx = 0;
            nextTokenIdx = 0;
        } else if (lineIdx < this.lineIdx) {
            this.lineIdx--;
        }
    }

    public boolean isLinesCountModified() {
        return linesCountModified;
    }

    public int linesCount() {
        return tokenizedLines.size();
    }

    public void issue(String message) {
        if (linesCountModified) {
            System.err.println("Printing current internal line");
            List<String> line = getLine();

            System.err.println(String.join(" ", line));
        }

        throw new IllegalArgumentException((linesCountModified ? "Internal " : "") +
                "Line " + (lineIdx + 1) + ": " + message);
    }
}
