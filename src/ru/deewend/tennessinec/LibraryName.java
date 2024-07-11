package ru.deewend.tennessinec;

import java.util.Objects;

public final class LibraryName {
    private final String name;
    private final String uppercaseName;

    private LibraryName(String name) {
        this.name = name;
        uppercaseName = name.toUpperCase();
    }

    public static LibraryName of(String name) {
        return new LibraryName(name);
    }

    public String getName() {
        return name;
    }

    public String getUppercaseName() {
        return uppercaseName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LibraryName that = (LibraryName) o;

        return Objects.equals(uppercaseName, that.uppercaseName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uppercaseName);
    }
}
