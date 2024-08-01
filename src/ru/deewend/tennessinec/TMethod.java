package ru.deewend.tennessinec;

import ru.deewend.tennessinec.exporter.Exporter;

import java.util.*;
import java.util.stream.Collectors;

public class TMethod {
    public static final int UNINITIALIZED_VIRTUAL_ADDRESS = 0;
    public static final int UNINITIALIZED_STACK_SIZE = 0;

    private static final Set<TMethod> KNOWN_METHODS = new HashSet<>();

    private final boolean external;
    private final DataType returnType;
    private final String name;
    private final List<DataType> parameterTypes;
    private final boolean hasVarargs;
    private int virtualAddress;
    private int stackSize;

    private TMethod(
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

    public static TMethod of(
            boolean external, DataType returnType, String name, List<DataType> parameterTypes, boolean hasVarargs
    ) {
        TMethod method = new TMethod(external, returnType, name, parameterTypes, hasVarargs);
        if (KNOWN_METHODS.contains(method)) {
            throw new IllegalArgumentException("A method meeting " +
                    "the following characteristics is already defined: " + method);
        }
        KNOWN_METHODS.add(method);

        return method;
    }

    public static TMethod lookup(String name, int parameterCount) {
        for (TMethod method : KNOWN_METHODS) {
            if (method.name.equals(name)) {
                int currentCount = method.parameterTypes.size();
                if (currentCount == parameterCount || (method.hasVarargs && parameterCount > currentCount)) {
                    return method;
                }
            }
        }

        throw new IllegalArgumentException("Could not find a method \"" +
                name + "\" which parameter count would be equal to " + parameterCount);
    }

    public static TMethod lookupEntryMethod() {
        for (TMethod method : KNOWN_METHODS) {
            if (method.isEntryMethod()) return method;
        }

        throw new IllegalArgumentException("Could not find the entry method");
    }

    public static void putCallMethod(Exporter exporter, String name, int parameterCount) {
        TMethod method = TMethod.lookup(name, parameterCount);

        exporter.putInstruction("CallMethod", method);
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

    public boolean isEntryMethod() {
        return name.equals("main") && parameterTypes.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TMethod method = (TMethod) o;

        // "external", "returnType", "hasVarargs" and "virtualAddress" fields are not compared intentionally
        return Objects.equals(name, method.name) && (parameterTypes.size() == method.parameterTypes.size());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, parameterTypes);
    }

    @Override
    public String toString() {
        return "TMethod{name='" + name + "', parameterCount=" + parameterTypes.size() + "}";
    }

    public String toStringExtended() {
        return returnType.getName() + " " + name + "(" + parameterTypes.stream()
                .map(DataType::getName)
                .collect(Collectors.joining(", ")) +
                        (hasVarargs ? (parameterTypes.isEmpty() ? "..." : ", ...") : "") + ")";
    }
}
