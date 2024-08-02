package ru.deewend.tennessinec;

import java.util.ArrayList;
import java.util.List;

public class Tokenizer {
    private static final Tokenizer INSTANCE = new Tokenizer();

    private String line;
    private String token;
    private String sourceFilename;
    private int i;
    private int lineNumber;
    boolean noticedMultilineComment;

    private Tokenizer() {
    }

    public static Tokenizer getInstance() {
        return INSTANCE;
    }

    public static boolean isLetter(char symbol) {
        return (symbol >= 'a' && symbol <= 'z') || (symbol >= 'A' && symbol <= 'Z');
    }

    public static boolean isDigit(char symbol) {
        return (symbol >= '0' && symbol <= '9');
    }

    private static boolean isLetterOrDigit(char symbol) {
        return isLetter(symbol) || isDigit(symbol);
    }

    public List<String> tokenizeLine(String originalLine, int lineNumber, String sourceFilename) {
        this.line = originalLine;
        this.lineNumber = lineNumber;
        this.sourceFilename = sourceFilename;

        List<String> result = new ArrayList<>();
        while (!line.isEmpty()) {
            // removing blank symbols in the start
            line = line.replaceFirst("^\\s*", "");
            if (line.isEmpty()) continue;

            token = null;
            char firstSymbol = line.charAt(0);

            if (line.startsWith("/*") || noticedMultilineComment) {
                i = (noticedMultilineComment ? 1 : 3);
                noticedMultilineComment = true;

                for (; i < line.length(); i++) {
                    char first = line.charAt(i - 1);
                    char second = line.charAt(i);
                    if (first == '*' && second == '/') {
                        noticedMultilineComment = false;

                        break;
                    }
                }
                if (noticedMultilineComment) break;
                token();

                continue; // not adding that token
            } else if (line.startsWith("//")) {
                break;
            } else if (isLetterOrDigit(firstSymbol)) {
                for (i = 1; i < line.length(); i++) {
                    char currentChar = line.charAt(i);
                    if (!isLetterOrDigit(line.charAt(i)) && currentChar != '_') break;
                }
                i--;
                token();
            } else if (firstSymbol == '"') {
                for (i = 1; i < line.length(); i++) {
                    char currentChar = line.charAt(i);
                    // boolean firstCondition =
                    // (firstSymbol == '"' && currentChar == '"') || (firstSymbol == '<' && currentChar == '>');
                    // but it's tricky since < > are also comparison operators
                    if (currentChar == '"' && (i == 1 || line.charAt(i - 1) != '\\')) {
                        token();

                        break;
                    }
                }
                if (token == null) issue("could not found the end of a string");
            } else if (firstSymbol == '\'') {
                String badEnding = "could not find the end of a character";
                if (line.length() <= 2) issue(badEnding);
                i = 1;
                char nextSymbol = line.charAt(i++);
                if (nextSymbol == '\\') {
                    if (line.length() == 3) issue(badEnding);
                    i++;
                }
                char closingSymbol = line.charAt(i);
                if (closingSymbol != '\'') issue(badEnding);

                token();
            } else if (firstSymbol == '.' && line.length() >= 3 && line.charAt(1) == '.' && line.charAt(2) == '.') {
                // varargs
                i = 2;
                token();
            } else if ("#(){}+-/*.,;=\\".indexOf(firstSymbol) != -1) {
                // TODO implement support of <> (#include <something>)
                i = 0;
                token(); // instead of token = String.valueOf(firstSymbol); line = line.substring(1);
            } else {
                issue("Found a token that starts with '" + firstSymbol + "': could not parse it");
            }
            result.add(token);
        }
        token = null;

        return result;
    }

    private void token() {
        token = line.substring(0, i + 1);
        line = line.substring(i + 1);
    }

    void issue(String message) {
        throw new IllegalArgumentException("[Tokenizer] " + sourceFilename + " at line " + lineNumber + ": " + message);
    }
}
