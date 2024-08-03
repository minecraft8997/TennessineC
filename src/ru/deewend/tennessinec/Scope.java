package ru.deewend.tennessinec;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Scope {
    public static final int MAX_FUNCTION_STACK_SIZE = 256;
    public static final int INITIAL_PARAMETER_OFFSET = 8;

    public static final int NOT_A_FUNCTION_SCOPE = -1;

    private static final Scope ROOT_SCOPE = new Scope(null, true, false);

    private static Scope currentScope = ROOT_SCOPE;
    private static int sizeOfCurrentFunctionParameters;
    private static int sizeOfCurrentFunctionLocalVariables;

    private final Scope parent;
    private final boolean rootScope;
    private final boolean functionScope;
    private final Map<String, VariableData> variables;

    private Scope(Scope parent, boolean rootScope, boolean functionScope) {
        this.parent = parent;
        this.rootScope = rootScope;
        this.functionScope = functionScope;
        this.variables = new HashMap<>();

        if (functionScope) {
            sizeOfCurrentFunctionParameters = 0;
            sizeOfCurrentFunctionLocalVariables = 0;
        }
    }

    public static void pushScope() {
        currentScope = new Scope(currentScope, false, (currentScope == ROOT_SCOPE));
    }

    public static int popScope() {
        if (currentScope == ROOT_SCOPE) return NOT_A_FUNCTION_SCOPE;

        boolean wasFunctionScope = currentScope.functionScope;
        currentScope = currentScope.parent;

        if (wasFunctionScope) return sizeOfCurrentFunctionLocalVariables;

        return NOT_A_FUNCTION_SCOPE;
    }

    public static void addVariable(String name, VariableData data, boolean parameter) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(data);

        if (currentScope == ROOT_SCOPE) {
            throw new IllegalStateException("Global variables are unsupported in this TennessineC version");
        }

        if (findVariable(currentScope, name) != null) {
            throw new IllegalArgumentException("Variable \"" + name + "\" is already defined in the current scope");
        }
        currentScope.variables.put(name, data);
        int size = data.getType().getSize();
        int stackOffset;
        if (parameter) {
            stackOffset = INITIAL_PARAMETER_OFFSET /* 8 */ + sizeOfCurrentFunctionParameters;
            sizeOfCurrentFunctionParameters += size;
        } else {
            sizeOfCurrentFunctionLocalVariables += size;
            stackOffset = MAX_FUNCTION_STACK_SIZE - sizeOfCurrentFunctionLocalVariables; // different order is intended
        }
        int sum = sizeOfCurrentFunctionParameters + sizeOfCurrentFunctionLocalVariables;
        if (sum > MAX_FUNCTION_STACK_SIZE - INITIAL_PARAMETER_OFFSET) { // if (sum > 248)
            throw new IllegalStateException("Cannot store variable \"" + name + "\" in the stack. In " +
                    "this TennessineC version the stack size is limited to " + MAX_FUNCTION_STACK_SIZE + " bytes " +
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
        return findVariable(name, true);
    }

    public static VariableData findVariable(String name, boolean throwException) {
        VariableData result = findVariable(currentScope, name);
        if (result == null && throwException) {
            throw new IllegalArgumentException("Failed to find variable \"" + name + "\" in the current scope");
        }

        return result;
    }

    public static boolean isRootScope() {
        return currentScope.rootScope;
    }

    public static boolean isFunctionScope() {
        return currentScope.functionScope;
    }
}
