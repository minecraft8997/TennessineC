package ru.deewend.tennessinec;

import java.util.*;

public class Metadata {
    private Map<LibraryName, Set<TMethod>> imports = new HashMap<>();
    private boolean finishedConstructingImports;
    private int fileCount;
    private int minNTVersion = 4;
    private int subsystem = 2; /* gui */

    public void addImport(
            String library, DataType returnType, String functionName, List<DataType> parameterTypes, boolean hasVarargs
    ) {
        LibraryName libraryName = LibraryName.of(library);
        if (!imports.containsKey(libraryName)) {
            imports.put(libraryName, new HashSet<>());
        }
        imports.get(libraryName).add(TMethod.of(true, returnType, functionName, parameterTypes, hasVarargs));
    }

    public void finishConstructingImports() {
        imports = Collections.unmodifiableMap(imports);
        finishedConstructingImports = true;
    }

    public Set<Pair<LibraryName, Set<TMethod>>> importsSet() {
        if (!finishedConstructingImports) throw new IllegalStateException();

        Set<Pair<LibraryName, Set<TMethod>>> result = new HashSet<>();
        for (Map.Entry<LibraryName, Set<TMethod>> data : imports.entrySet()) {
            result.add(Pair.of(data.getKey(), Collections.unmodifiableSet(data.getValue())));
        }

        return result;
    }

    public void setMinNTVersion(int minNTVersion) {
        this.minNTVersion = minNTVersion;
    }

    public void setSubsystem(int subsystem) {
        this.subsystem = subsystem;
    }

    public int getMinNTVersion() {
        return minNTVersion;
    }

    public int getSubsystem() {
        return subsystem;
    }
}
