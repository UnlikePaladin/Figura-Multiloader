package org.figuramc.figura.utils;

import java.util.Objects;

public class Pair<F, S> {
    private final F first;
    private final S second;

    public Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    public F getFirst() {
        return this.first;
    }

    public S getSecond() {
        return this.second;
    }

    public Pair<S, F> swap() {
        return of(this.second, this.first);
    }

    public String toString() {
        return "(" + this.first + ", " + this.second + ")";
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Pair)) {
            return false;
        } else {
            Pair<?, ?> other = (Pair)obj;
            return Objects.equals(this.first, other.first) && Objects.equals(this.second, other.second);
        }
    }

    public static <F, S> Pair<F, S> of(F first, S second) {
        return new Pair<>(first, second);
    }

    public int hashCode() {
        return com.google.common.base.Objects.hashCode(this.first, this.second);
    }
}