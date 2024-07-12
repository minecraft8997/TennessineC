package ru.deewend.tennessinec;

import java.util.Objects;

public class Triple<A, B, C> {
    private final A first;
    private final B second;
    private final C third;

    private Triple(A first, B second, C third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public static <A, B, C> Triple<A, B, C> of(A first, B second, C third) {
        return new Triple<>(first, second, third);
    }

    public A getFirst() {
        return first;
    }

    public B getSecond() {
        return second;
    }

    public C getThird() {
        return third;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Triple<?, ?, ?> triple = (Triple<?, ?, ?>) o;

        return Objects.equals(first, triple.first) && Objects.equals(second, triple.second) &&
                Objects.equals(third, triple.third);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second, third);
    }
}
