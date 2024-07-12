package ru.deewend.tennessinec;

import ru.deewend.tennessinec.exporter.Exporter;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

public class TennessineC {
    public static final int CONTEXT_EXPECTING_SOURCE_FILE = 1;
    public static final int CONTEXT_EXPECTING_DEFINES = 2;
    public static final int CONTEXT_EXPECTING_EXPORTER = 3;
    public static final int CONTEXT_EXPECTING_BUFFER_CAPACITY = 4;
    public static final int CONTEXT_EXPECTING_OUTPUT_FILE = 5;

    final Set<String> defines;
    private final InputStream sourceStream;
    private final String sourceFilename;
    final String parentDirectory;
    TokenizedCode tokenizedLines;
    Metadata metadata;
    int idx;
    final Map<String, VariableData> variableMap;
    private final boolean debugPreprocessingResult;
    private final Exporter exporter;

    public TennessineC(
            Set<String> defines,
            InputStream sourceStream,
            String sourceFilename,
            String parentDirectory,
            boolean debugPreprocessingResult,
            Exporter exporter
    ) {
        this.defines = new HashSet<>(defines);
        this.sourceStream = sourceStream;
        this.sourceFilename = sourceFilename;
        this.parentDirectory = parentDirectory;
        this.variableMap = new HashMap<>();
        this.debugPreprocessingResult = debugPreprocessingResult;
        this.exporter = exporter;
    }

    @SuppressWarnings("IOStreamConstructor")
    public static void main(String[] args) {
        String sourceFile = null;
        Set<String> defines = new HashSet<>();
        boolean debugPreprocessor = false;
        String exporter = "WinI386";
        int bufferCapacity = 65536;
        String outputFile = null;
        int context = 0;
        for (int i = 0; i < args.length; i++) {
            String argument = args[i];
            boolean shouldClearContextLater = (context != 0 && context != CONTEXT_EXPECTING_DEFINES);
            switch (context) {
                case CONTEXT_EXPECTING_SOURCE_FILE: {
                    sourceFile = argument;

                    break;
                }
                case CONTEXT_EXPECTING_DEFINES: {
                    if (argument.startsWith("-")) {
                        context = 0;
                        i--;

                        break;
                    }
                    defines.add(argument);

                    break;
                }
                case CONTEXT_EXPECTING_EXPORTER: {
                    exporter = argument;

                    break;
                }
                case CONTEXT_EXPECTING_BUFFER_CAPACITY: {
                    bufferCapacity = Integer.parseInt(argument);

                    break;
                }
                case CONTEXT_EXPECTING_OUTPUT_FILE: {
                    outputFile = argument;

                    break;
                }
                default: {
                    if (argument.equalsIgnoreCase("-s")) {
                        context = CONTEXT_EXPECTING_SOURCE_FILE;
                    } else if (argument.equalsIgnoreCase("-d")) {
                        context = CONTEXT_EXPECTING_DEFINES;
                    } else if (argument.equalsIgnoreCase("-dp")) {
                        debugPreprocessor = true;
                    } else if (argument.equalsIgnoreCase("-e")) {
                        context = CONTEXT_EXPECTING_EXPORTER;
                    } else if (argument.equalsIgnoreCase("-bc")) {
                        context = CONTEXT_EXPECTING_BUFFER_CAPACITY;
                    } else if (argument.equalsIgnoreCase("-o")) {
                        context = CONTEXT_EXPECTING_OUTPUT_FILE;
                    } else {
                        System.err.println("Unrecognized option (at i=" + i + "): " + argument);
                    }

                    break;
                }
            }
            if (shouldClearContextLater) context = 0;
        }
        boolean hasIssues = false;
        if (sourceFile == null) {
            System.err.println("Source file is not specified");

            hasIssues = true;
        }
        if (!Helper.validateToken(exporter)) {
            System.err.println("Bad exporter name");

            hasIssues = true;
        }
        if (outputFile == null) {
            System.err.println("Output file is not specified");

            hasIssues = true;
        }
        if (hasIssues) {
            System.err.println("Usage: java -jar TennessineC.jar " +
                    "-s <sourceFile> [-d [define1] [define2] ...] [-dp] [-e <exporter>] [-bc <bufferCapacity>] -o <outputFile>");

            System.exit(-1);
        }
        System.out.println("Source file: " + sourceFile);
        System.out.println("Defines (might be listed in a different order): " + String.join(" ", defines));
        System.out.println("Debug preprocessor: " + debugPreprocessor);
        System.out.println("Exporter: " + exporter);
        System.out.println("Buffer capacity: " + bufferCapacity + " bytes");
        System.out.println("Output file: " + outputFile);
        System.out.println();

        Exporter exporterObj = Helper.getExporter(exporter);
        if (exporterObj == null) System.exit(-1);

        TennessineC compiler;
        File file = new File(sourceFile);
        try (InputStream stream = new FileInputStream(file)) {
            compiler = new TennessineC(defines, stream, sourceFile,
                    file.getParent(), true, exporterObj);
            compiler.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        compiler.preprocess();

        compiler.compile();
        Metadata metadata = compiler.getCompiledCode();
        exporterObj.load(metadata);

        ByteBuffer buffer = ByteBuffer.allocate(bufferCapacity);
        exporterObj.export(buffer);

        int position = buffer.position();
        try (OutputStream stream = new FileOutputStream(outputFile)) {
            buffer.flip();
            stream.write(buffer.array(), 0, position);
            for (; position < exporterObj.imageSize(); position++) stream.write(0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void load() {
        // assuming sourceStream will be closed by the caller
        // both BufferedStream and InputStreamReader themselves don't hold any native resources, not closing them
        List<List<String>> tokenizedLines = Helper.tokenize(sourceStream);

        this.tokenizedLines = TokenizedCode.of(tokenizedLines, sourceFilename);
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public void preprocess() {
        /*
         * Instantiating in advance to be able to add imports specified in #pragma tenc import.
         */
        metadata = new Metadata();

        Preprocessor.getInstance().linkCompiler(this);

        while (idx != tokenizedLines.linesCount()) {
            boolean shouldReset = false;
            try {
                shouldReset = Preprocessor.getInstance().handleLine();
            } catch (Exception e) {
                System.err.println("Preprocessor has thrown an Exception:");
                e.printStackTrace();

                Helper.crash(e.getMessage());
            }
            idx = (shouldReset ? 0 : idx + 1);
        }
        metadata.addImport("kernel32.dll", "ExitProcess", Collections.singletonList("byte"));
        metadata.finishConstructingImports();

        if (debugPreprocessingResult) {
            for (int i = 0; i < tokenizedLines.linesCount(); i++) {
                tokenizedLines.switchToLine(i);
                List<String> line = tokenizedLines.getLine();

                System.out.println(String.join(" ", line));
                /*
                if (Preprocessor.getInstance().hasFutureAddresses()) {
                    // assuming the list is sorted in ascending order
                    List<Integer> futureAddresses = Preprocessor.getInstance().listFutureAddresses();
                    for (int i = 0; i < line.length(); i++) {
                        char c;
                        if (futureAddresses.contains(i)) {
                            c = '^';
                        } else {
                            c = ' ';
                        }
                        System.out.print(c);
                    }
                    System.out.println();
                    int firstIdx = futureAddresses.get(0);
                    for (int i = 0; i < firstIdx; i++) System.out.println(' ');

                    System.out.println("TennessineC will put an address here");
                }
                 */
            }
        }
    }

    public void compile() {
        if (tokenizedLines.linesCount() == 0) {
            addExitProcess();

            return;
        }
        mountStrings();

        idx = 0;
        tokenizedLines.switchToLine(0);
        int methodCount = 0;
        boolean insideAMethod = false;
        while (hasMoreTokens()) {
            String nextToken;
            if (!insideAMethod) {
                nextToken = nextToken();
                if (!nextToken.equals("int") && !nextToken.equals("void")) {
                    tokenizedLines.issue("unexpected token: " + nextToken);
                }

                String name = nextToken();
                String action = nextToken();
                if (action.equals("=")) tokenizedLines.issue("global variables are currently unsupported");
                if (!action.equals("(")) tokenizedLines.issue("expected \"(\", found: " + action);
                if (!name.equals("main")) tokenizedLines.issue("methods other than \"main\" are currently unsupported");
                if (!nextToken().equals(")")) tokenizedLines.issue("method parameters are currently unsupported");
                if (!nextToken().equals("{")) tokenizedLines.issue("expected an opening curly brace");
                if (methodCount > 0) tokenizedLines.issue("defining multiple methods is currently unsupported");

                insideAMethod = true;
            }
            boolean symbol = nextTokenIs(TokenizedCode.TokenType.SYMBOL);
            nextToken = nextToken();
            if (nextToken.equals("}")) {
                insideAMethod = false;
                methodCount++;

                continue;
            }
            DataType type = DataType.recognizeDataType(nextToken);

            if (type != null) {
                handleVariableDefinition(type);
            } else {
                if (!nextToken().equals("(")) {
                    tokenizedLines.issue("expected either a variable declaration or a method call " +
                            "(any other statements are currently unsupported)");
                }
                if (!symbol) {
                    tokenizedLines.issue("unexpected token: " + nextToken);
                }

                handleMethodCall(nextToken);
            }

            if (!(nextToken = nextToken()).equals(";")) tokenizedLines.issue("expected \";\", found: " + nextToken);
        }

        addExitProcess();
    }

    private void handleVariableDefinition(DataType recognizedType) {
        if (getNextTokenType() != TokenizedCode.TokenType.SYMBOL) {
            tokenizedLines.issue("expected the next token to be a symbol (variable name)");
        }
        String variableName = nextToken();
        VariableData data;
        variableMap.put(variableName, (data = VariableData.of(recognizedType)));

        if (nextTokenIs(TokenizedCode.TokenType.STATEMENT_END)) return;

        if (!nextToken().equals("=")) {
            tokenizedLines.issue("expected an assignment");
        }
        if (getNextTokenType() != TokenizedCode.TokenType.LITERAL_INTEGER) {
            tokenizedLines.issue("expected the next token to be an integer (variable value)");
        }
        int value = Integer.parseInt(nextToken());

        data.calculateStackOffset(this);
        recognizedType.push(exporter, value);
    }

    private void handleMethodCall(String nextToken) {
        ExternalMethod method = lookupExternalMethod(nextToken);
        List<String> parameterTypes = method.getParameterTypes();

        //if (!parameterTypes.isEmpty()) {
        int i = 0;
        List<Pair<String, Integer>> toPush = new ArrayList<>();
        while (getNextTokenType() == TokenizedCode.TokenType.LITERAL_INTEGER) {
            if (i == parameterTypes.size()) tokenizedLines.issue("too many arguments");

            int value = Integer.parseInt(nextToken());
            String type = Helper.uppercaseFirstCharacter(parameterTypes.get(i++));
            toPush.add(Pair.of(type, value));

            if (i < parameterTypes.size() && !(nextToken = nextToken()).equals(",")) {
                tokenizedLines.issue("expected a comma, found: " + nextToken);
            }
        }
        if (i != parameterTypes.size()) tokenizedLines.issue("too few arguments");
        if (!nextToken().equals(")")) {
            tokenizedLines.issue("expected a closing brace");
        }
        i--;
        for (; i >= 0; i--) {
            Pair<String, Integer> pair = toPush.get(i);
            String type = pair.getFirst();
            int value = pair.getSecond();

            exporter.putInstruction("Push" + type, value);
        }
        //}

        exporter.putInstruction("CallExternalMethod", method.getName());
    }

    private ExternalMethod lookupExternalMethod(String name) {
        Set<Pair<LibraryName, Set<ExternalMethod>>> importsSet = metadata.importsSet();

        ExternalMethod method = null;
        for (Pair<LibraryName, Set<ExternalMethod>> pair : importsSet) {
            Set<ExternalMethod> externalMethods = pair.getSecond();

            for (ExternalMethod externalMethod : externalMethods) {
                if (externalMethod.getName().equals(name)) {
                    if (method != null) {
                        tokenizedLines.issue("While searching for definition of external method \"" + name + "\" " +
                                "TennessineC has faced with two or more candidates. Method overloading is currently unsupported");
                    }
                    method = externalMethod;
                }
            }
        }
        if (method == null) tokenizedLines.issue("Unable to find method \"" + name + "\"");

        return method;
    }

    private void addExitProcess() {
        exporter.putInstruction("PushByte", 0);
        exporter.putInstruction("CallExternalMethod", "ExitProcess");
    }

    private boolean nextTokenIs(TokenizedCode.TokenType type) {
        return hasMoreTokens() && getNextTokenType() == type;
    }

    private boolean hasMoreTokens() {
        boolean hasMoreTokens = tokenizedLines.hasMoreTokens();
        if (!hasMoreTokens) {
            while (++idx < tokenizedLines.linesCount()) {
                tokenizedLines.switchToLine(idx);
                hasMoreTokens = tokenizedLines.hasMoreTokens();
                if (hasMoreTokens) break;
            }
        }

        return hasMoreTokens;
    }

    private TokenizedCode.TokenType getNextTokenType() {
        hasMoreTokens();

        return tokenizedLines.getNextTokenType();
    }

    private String nextToken() {
        return nextToken(false);
    }

    private String nextToken(boolean tt) {
        if (!tt) hasMoreTokens();

        return tokenizedLines.nextToken();
    }

    private void mountStrings() {
        for (int i = 0; i < tokenizedLines.linesCount(); i++) {
            tokenizedLines.switchToLine(i);

            while (tokenizedLines.hasMoreTokens()) {
                boolean string = (tokenizedLines.getNextTokenType() == TokenizedCode.TokenType.LITERAL_STRING);
                String token = tokenizedLines.nextToken();
                if (string) {
                    token = Helper.stringTokenToString(token);
                    int address = exporter.mountString(token);
                    tokenizedLines.patchToken(String.valueOf(address));
                }
            }
        }
    }

    public Metadata getCompiledCode() {
        return metadata;
    }
}
