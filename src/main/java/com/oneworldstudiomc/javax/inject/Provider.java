package com.oneworldstudiomc.javax.inject;

/**
 * Compatibility bridge for plugins remapped from javax.inject to OneWorldCore namespace.
 */
@FunctionalInterface
public interface Provider<T> {
    T get();
}
