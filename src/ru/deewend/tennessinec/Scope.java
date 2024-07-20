package ru.deewend.tennessinec;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Scope {
    public static final int METHOD_STACK_SIZE = 256;

    private static final Scope ROOT_SCOPE = new Scope(null, true, false);

    private static Scope currentScope = ROOT_SCOPE;
    private static int sizeOfCurrentMethodVariables;

    private final Scope parent;
    private final boolean rootScope;
    private final boolean methodScope;
    private final Map<String, VariableData> variables;

    private Scope(Scope parent, boolean rootScope, boolean methodScope) {
        this.parent = parent;
        this.rootScope = rootScope;
        this.methodScope = methodScope;
        this.variables = new HashMap<>();

        if (methodScope) sizeOfCurrentMethodVariables = 0;
    }

    public static void pushScope() {
        currentScope = new Scope(currentScope, false, (currentScope == ROOT_SCOPE));
    }

    public static void popScope() {
        if (currentScope == ROOT_SCOPE) return;

        currentScope = currentScope.parent;
    }

    public static void addVariable(String name, VariableData data) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(data);

        if (currentScope == ROOT_SCOPE) {
            throw new IllegalStateException("Global variables are not supported in this TennessineC version");
        }

        if (findVariable(currentScope, name) != null) {
            throw new IllegalArgumentException("Variable \"" + name + "\" is already defined in the current scope");
        }
        currentScope.variables.put(name, data);
        sizeOfCurrentMethodVariables += data.getType().getSize();
        data.setStackOffset(METHOD_STACK_SIZE - sizeOfCurrentMethodVariables);
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
