package ru.deewend.tennessinec;

import java.util.ArrayList;
import java.util.List;

public class Tokenizer {
    private static final Tokenizer INSTANCE = new Tokenizer();

    private String line;
    private String token;
    private int i;
    private int lineNumber;

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

    public List<String> tokenizeLine(String originalLine, int lineNumber) {
        this.line = originalLine;
        this.lineNumber = lineNumber;

        List<String> result = new ArrayList<>();
        while (!line.isEmpty()) {
            // removing blank symbols in the start
            line = line.replaceFirst("^\\s*", "");
            if (line.isEmpty()) continue;

            token = null;
            char firstSymbol = line.charAt(0);
            if (isLetterOrDigit(firstSymbol)) {
                for (i = 1; i < line.length(); i++) {
                    char currentChar = line.charAt(i);
                    if (!isLetterOrDigit(line.charAt(i)) && currentChar != '_') break;
                }
                i--;
                token();
            } else if (line.startsWith("//")) {
                break;
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
                // todo implement support of <> (#include <something>)
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

    private void issue(String message) {
        throw new IllegalArgumentException("Line " + lineNumber + ": " + message);
    }
}
