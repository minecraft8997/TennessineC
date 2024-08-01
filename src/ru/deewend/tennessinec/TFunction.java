package ru.deewend.tennessinec;

import ru.deewend.tennessinec.exporter.Exporter;

import java.util.*;
import java.util.stream.Collectors;

public class TFunction {
    public static final int UNINITIALIZED_VIRTUAL_ADDRESS = 0;
    public static final int UNINITIALIZED_STACK_SIZE = 0;

    private static final Set<TFunction> KNOWN_FUNCTIONS = new HashSet<>();

    private final boolean external;
    private final DataType returnType;
    private final String name;
    private final List<DataType> parameterTypes;
    private final boolean hasVarargs;
    private int virtualAddress;
    private int stackSize;

    private TFunction(
            boolean external, DataType returnType, String name, List<DataType> parameterTypes, boolean hasVarargs
    ) {
        Objects.requireNonNull(returnType);
        Objects.requireNonNull(name);
        Objects.requireNonNull(parameterTypes);
        for (DataType type : parameterTypes) Objects.requireNonNull(type);

        this.external = external;
        this.returnType = returnType;
        this.name = name;
        this.parameterTypes = new ArrayList<>(parameterTypes);
        this.hasVarargs = hasVarargs;
    }

    public static TFunction of(
            boolean external, DataType returnType, String name, List<DataType> parameterTypes, boolean hasVarargs
    ) {
        TFunction function = new TFunction(external, returnType, name, parameterTypes, hasVarargs);
        if (KNOWN_FUNCTIONS.contains(function)) {
            throw new IllegalArgumentException("A function meeting " +
                    "the following characteristics is already defined: " + function);
        }
        KNOWN_FUNCTIONS.add(function);

        return function;
    }

    public static TFunction lookup(String name, int parameterCount) {
        for (TFunction function : KNOWN_FUNCTIONS) {
            if (function.name.equals(name)) {
                int currentCount = function.parameterTypes.size();
                if (currentCount == parameterCount || (function.hasVarargs && parameterCount > currentCount)) {
                    return function;
                }
            }
        }

        throw new IllegalArgumentException("Could not find a function \"" +
                name + "\" which parameter count would be equal to " + parameterCount);
    }

    public static TFunction lookupEntryFunction() {
        for (TFunction function : KNOWN_FUNCTIONS) {
            if (function.isEntryFunction()) return function;
        }

        throw new IllegalArgumentException("Could not find the entry function");
    }

    public static void putCallFunction(Exporter exporter, String name, int parameterCount) {
        TFunction function = TFunction.lookup(name, parameterCount);

        exporter.putInstruction("CallFunction", function);
    }

    public boolean isExternal() {
        return external;
    }

    public DataType getReturnType() {
        return returnType;
    }

    public String getName() {
        return name;
    }

    public List<DataType> getParameterTypes() {
        return Collections.unmodifiableList(parameterTypes);
    }

    public boolean hasVarargs() {
        return hasVarargs;
    }

    public int getVirtualAddress() {
        return virtualAddress;
    }

    public void setVirtualAddress(int virtualAddress) {
        this.virtualAddress = virtualAddress;
    }

    public int getStackSize() {
        return stackSize;
    }

    public void setStackSize(int stackSize) {
        this.stackSize = stackSize;
    }

    public boolean isEntryFunction() {
        return name.equals("main") && parameterTypes.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TFunction function = (TFunction) o;

        // "external", "returnType", "hasVarargs" and "virtualAddress" fields are not compared intentionally
        return Objects.equals(name, function.name) && (parameterTypes.size() == function.parameterTypes.size());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, parameterTypes);
    }

    @Override
    public String toString() {
        return "TFunction{name='" + name + "', parameterCount=" + parameterTypes.size() + "}";
    }

    public String toStringExtended() {
        return returnType.getName() + " " + name + "(" + parameterTypes.stream()
                .map(DataType::getName)
                .collect(Collectors.joining(", ")) +
                        (hasVarargs ? (parameterTypes.isEmpty() ? "..." : ", ...") : "") + ")";
    }
}
