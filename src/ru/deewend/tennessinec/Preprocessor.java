package ru.deewend.tennessinec;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.*;

public class Preprocessor {
    private static final Preprocessor INSTANCE = new Preprocessor();
    private final Map<Integer, List<Integer>> futureAddresses = new HashMap<>();
    private final Map<String, Object> context = new HashMap<>();
    // private final Map<String, String> pragmaOptions = new HashMap<>();
    private Set<String> defines;
    private TennessineC compiler;
    private TokenizedCode tokenizedCode;
    private String nextLineRequestedBy;

    private Preprocessor() {
    }

    public static Preprocessor getInstance() {
        return INSTANCE;
    }

    public void linkCompiler(TennessineC compiler) {
        this.compiler = compiler;
        tokenizedCode = compiler.tokenizedLines;
        futureAddresses.clear();
        defines = new HashSet<>(compiler.defines);
    }

    public boolean hasFutureAddresses() {
        return futureAddresses.containsKey(compiler.idx);
    }

    public List<Integer> listFutureAddresses() {
        return Collections.unmodifiableList(futureAddresses.get(compiler.idx));
    }

    /**
     * @return true if the compiler should start analyzing source file again.
     */
    public boolean handleLine() throws Exception {
        Method method = null;
        tokenizedCode.switchToLine(compiler.idx);

        if (nextLineRequestedBy != null) {
            method = getClass().getDeclaredMethod(nextLineRequestedBy);
            nextLineRequestedBy = null;
        } else {
            context.clear();

            String directive;
            if ((directive = scanDirective(true)) != null) {
                String newDirective = Helper.uppercaseFirstCharacter(directive);

                method = getClass().getDeclaredMethod("do" + newDirective);
            }
        }
        if (method != null) {
            method.setAccessible(true);

            return (Boolean) method.invoke(this);
        }

        return false;
    }

    // note that this method may cause TokenizedCode's cursor to move
    private boolean isDirective() {
        return tokenizedCode.hasMoreTokens() && tokenizedCode.nextToken().equals("#");
    }

    private String scanDirective(boolean strict) {
        if (isDirective()) {
            boolean hasIssue = false;
            if (!tokenizedCode.hasMoreTokens() || tokenizedCode.getNextTokenType() != TokenizedCode.TokenType.SYMBOL) {
                if (strict) tokenizedCode.issue("bad directive");
                else        hasIssue = true;
            }

            if (!hasIssue) return tokenizedCode.nextToken();
        }

        return null;
    }

    @SuppressWarnings({"IOStreamConstructor", "unused"})
    public boolean doInclude() {
        if (tokenizedCode.getNextTokenType() != TokenizedCode.TokenType.LITERAL_STRING) {
            tokenizedCode.issue("expected the next token (path to the file to be included) to be a string literal");
        }
        String fileName = Helper.stringTokenToString(tokenizedCode.nextToken());
        tokenizedCode.removeLine(compiler.idx);

        List<List<String>> tokenizedLines;
        File file = new File(compiler.parentDirectory, fileName);
        try (InputStream stream = new FileInputStream(file)) {
            tokenizedLines = Helper.tokenize(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (int i = tokenizedLines.size() - 1; i >= 0; i--) {
            tokenizedCode.insertFirstLine(TokenizedLine.of(file.getPath(), i, tokenizedLines.get(i)));
        }

        return true;
    }

    @SuppressWarnings("unused")
    public boolean doPragma() {
        if (tokenizedCode.getNextTokenType() != TokenizedCode.TokenType.SYMBOL) {
            tokenizedCode.issue("expected the next token (after #pragma) to be a symbol");
        }
        String firstOption = tokenizedCode.nextToken();
        if (!firstOption.equals("tenc")) {
            tokenizedCode.issue("only \"#pragma tenc ...\" options are currently supported");
        }
        if (tokenizedCode.getNextTokenType() != TokenizedCode.TokenType.SYMBOL) {
            tokenizedCode.issue("expected the next token (after #pragma tenc) to be a symbol");
        }
        String secondOption = tokenizedCode.nextToken();
        if (secondOption.equals("bundleFile")) {
            Helper.crash("\"bundleFile\" will be implemented in future versions of TennessineC");
        } else if (secondOption.equals("import")) {
            if (!tokenizedCode.nextToken().equals("(")) tokenizedCode.issue("bad import syntax");
            if (tokenizedCode.getNextTokenType() != TokenizedCode.TokenType.LITERAL_STRING) {
                tokenizedCode.issue("expected a string literal (library name)");
            }
            String libraryName = Helper.stringTokenToString(tokenizedCode.nextToken());
            if (tokenizedCode.getNextTokenTypeOmitComma(true) != TokenizedCode.TokenType.LITERAL_STRING) {
                tokenizedCode.issue("expected a string literal (method name)");
            }
            String methodName = Helper.stringTokenToString(tokenizedCode.nextToken());
            List<String> types = new ArrayList<>();

            while (tokenizedCode.getNextTokenTypeOmitComma(false) == TokenizedCode.TokenType.SYMBOL) {
                types.add(tokenizedCode.nextToken());
            }
            if (!tokenizedCode.currentToken().equals(")")) tokenizedCode.issue("bad import syntax");

            tokenizedCode.removeLine(compiler.idx);

            compiler.metadata.addImport(libraryName, methodName, types);

            return true;
        }
        boolean shouldConvert = (tokenizedCode.getNextTokenType() == TokenizedCode.TokenType.LITERAL_STRING);
        String value = tokenizedCode.nextToken();
        if (shouldConvert) value = Helper.stringTokenToString(value);

        tokenizedCode.removeLine(compiler.idx);

        Metadata metadata = compiler.metadata;
        switch (secondOption) {
            case "subsystem": {
                if (!value.equals("gui")) tokenizedCode.issue("Subsystems other than \"gui\" are currently unsupported");

                metadata.setSubsystem(2);

                break;
            }
            case "minNTVersion": {
                metadata.setMinNTVersion(Integer.parseInt(value));

                break;
            }
            default: {
                System.err.println("Unrecognized #pragma tenc option: " + secondOption + ", ignoring");

                break;
            }
        }

        return true;
    }

    @SuppressWarnings({"unchecked", "unused"})
    public boolean doDefine() {
        String name;
        List<String> value;
        if (!context.isEmpty()) {
            name = (String) context.get("name");
            value = null;

            String directive;
            if ((directive = scanDirective(false)) != null && directive.startsWith("if")) {
                nextLineRequestedBy = "doDefine";

                return false;
            }
            while (tokenizedCode.hasMoreTokens()) {
                String nextToken = tokenizedCode.nextToken();
                if (nextToken.equals(name)) {
                    if (value == null) {
                        value = (List<String>) context.get("value");
                    }
                    tokenizedCode.patchToken(value.get(0));
                    for (int j = 1; j < value.size(); j++) tokenizedCode.insertToken(value.get(j));
                }
            }
            if (isLastLine()) {
                tokenizedCode.removeLine((int) context.get("idx"));

                return true;
            }
            nextLineRequestedBy = "doDefine";

            return false;
        }
        if (tokenizedCode.getNextTokenType() != TokenizedCode.TokenType.SYMBOL) tokenizedCode.issue("bad #define name");
        name = tokenizedCode.nextToken();
        value = collectRemainingTokens(true);
        context.put("name", name);
        context.put("value", value);
        context.put("idx", compiler.idx);

        defines.add(name);

        nextLineRequestedBy = "doDefine";

        return false;
    }

    @SuppressWarnings("unused")
    public boolean doIfdef() {
        return doIfdef(false);
    }

    public boolean doIfdef(boolean not) {
        String mode;
        if (!context.isEmpty()) {
            String token;
            if ((token = scanDirective(false)) != null) {
                int stack = (int) context.get("stack");
                if (token.startsWith("if")) {
                    context.put("stack", (stack + 1));
                } else if (token.equals("endif")) {
                    if (stack == 0) {
                        mode = (String) context.get("mode");
                        int startingWith = (int) context.get("startingWith");
                        //noinspection StringEquality
                        if (mode == "clearIfAndEndIf") { // a small optimization; we don't need equals() here
                            tokenizedCode.removeLine(compiler.idx);
                            tokenizedCode.removeLine(startingWith);
                        } else { // == "clearEverything"
                            for (int i = compiler.idx; i >= startingWith; i--) {
                                tokenizedCode.removeLine(i);
                            }
                        }

                        return true;
                    } else {
                        context.put("stack", (stack - 1));
                        nextLineRequestedBy = "doIfdef";

                        return false;
                    }
                }
            }
            nextLineRequestedBy = "doIfdef";

            return false;
        }
        if (tokenizedCode.getNextTokenType() != TokenizedCode.TokenType.SYMBOL) {
            tokenizedCode.issue("it is not possible a #define with such name exists");
        }
        String name = tokenizedCode.nextToken();
        boolean defined = defines.contains(name);

        if ((not && !defined) || (!not && defined)) {
            mode = "clearIfAndEndIf";
        } else {
            mode = "clearEverything";
        }
        context.put("mode", mode);
        context.put("startingWith", compiler.idx);
        context.put("stack", 0);

        nextLineRequestedBy = "doIfdef";

        return false;
    }

    @SuppressWarnings("unused")
    public boolean doIfndef() {
        return doIfdef(true);
    }

    @SuppressWarnings("unused")
    public boolean doError() {
        Helper.crash(String.join(" ", collectRemainingTokens(false)));

        return false;
    }

    private boolean isLastLine() {
        return compiler.idx == tokenizedCode.linesCount() - 1;
    }

    private List<String> collectRemainingTokens(boolean isDefine) {
        List<String> value = new ArrayList<>();
        while (tokenizedCode.hasMoreTokens()) {
            String nextToken = tokenizedCode.nextToken();
            if (isDefine && !tokenizedCode.hasMoreTokens() && nextToken.equals("\\")) {
                tokenizedCode.issue("multi-line #defines are not supported");
            }
            value.add(nextToken);
        }
        if (value.isEmpty()) value.add("");

        return value;
    }
}
