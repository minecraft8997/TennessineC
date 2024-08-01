package ru.deewend.tennessinec;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Scope {
    public static final int MAX_METHOD_STACK_SIZE = 256;
    public static final int INITIAL_PARAMETER_OFFSET = 8;

    public static final int NOT_A_METHOD_SCOPE = -1;

    private static final Scope ROOT_SCOPE = new Scope(null, true, false);

    private static Scope currentScope = ROOT_SCOPE;
    private static int sizeOfCurrentMethodParameters;
    private static int sizeOfCurrentMethodLocalVariables;

    private final Scope parent;
    private final boolean rootScope;
    private final boolean methodScope;
    private final Map<String, VariableData> variables;

    private Scope(Scope parent, boolean rootScope, boolean methodScope) {
        this.parent = parent;
        this.rootScope = rootScope;
        this.methodScope = methodScope;
        this.variables = new HashMap<>();

        if (methodScope) {
            sizeOfCurrentMethodParameters = 0;
            sizeOfCurrentMethodLocalVariables = 0;
        }
    }

    public static void pushScope() {
        currentScope = new Scope(currentScope, false, (currentScope == ROOT_SCOPE));
    }

    public static int popScope() {
        if (currentScope == ROOT_SCOPE) return NOT_A_METHOD_SCOPE;

        boolean wasMethodScope = currentScope.methodScope;
        currentScope = currentScope.parent;

        if (wasMethodScope) return sizeOfCurrentMethodLocalVariables;

        return NOT_A_METHOD_SCOPE;
    }

    public static void addVariable(String name, VariableData data, boolean parameter) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(data);

        if (currentScope == ROOT_SCOPE) {
            throw new IllegalStateException("Global variables are not supported in this TennessineC version");
        }

        if (findVariable(currentScope, name) != null) {
            throw new IllegalArgumentException("Variable \"" + name + "\" is already defined in the current scope");
        }
        currentScope.variables.put(name, data);
        int size = data.getType().getSize();
        int stackOffset;
        if (parameter) {
            stackOffset = INITIAL_PARAMETER_OFFSET /* 8 */ + sizeOfCurrentMethodParameters;
            sizeOfCurrentMethodParameters += size;
        } else {
            sizeOfCurrentMethodLocalVariables += size;
            stackOffset = MAX_METHOD_STACK_SIZE - sizeOfCurrentMethodLocalVariables; // different order is intended
        }
        int sum = sizeOfCurrentMethodParameters + sizeOfCurrentMethodLocalVariables;
        if (sum > MAX_METHOD_STACK_SIZE - INITIAL_PARAMETER_OFFSET) { // if (sum > 248)
            throw new IllegalStateException("Cannot store variable \"" + name + "\" in the stack. In " +
                    "this TennessineC version the stack size is limited to " + MAX_METHOD_STACK_SIZE + " bytes " +
                    "(the first " + INITIAL_PARAMETER_OFFSET + " bytes are reserved)");
        }
        data.setStackOffset(stackOffset);
    }

    private static VariableData findVariable(Scope scope, String name) {
        Objects.requireNonNull(scope);
        Objects.requireNonNull(name);

        for (String variableName : scope.variables.keySet()) {
            if (variableName.equals(name)) {
                return scope.variables.get(variableName);
            }
        }
        if (scope == ROOT_SCOPE) return null;

        return findVariable(scope.parent, name);
    }

    public static VariableData findVariable(String name) {
        VariableData result = findVariable(currentScope, name);
        if (result == null) {
            throw new IllegalArgumentException("Failed to find variable \"" + name + "\" in the current scope");
        }

        return result;
    }

    public static boolean isRootScope() {
        return currentScope.rootScope;
    }

    public static boolean isMethodScope() {
        return currentScope.methodScope;
    }
}
