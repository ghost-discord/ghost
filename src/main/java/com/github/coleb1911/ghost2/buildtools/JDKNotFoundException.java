package com.github.coleb1911.ghost2.buildtools;

/**
 * Thrown when {@link SystemUtils} can't find a valid JDK on the system
 */
final class JDKNotFoundException extends Exception {
    private static final String DEFAULT_MESSAGE = "Could not find the JDK via JAVA_HOME or your PATH variable." + System.lineSeparator() +
            "Check to make sure one of these variables is set, contains a valid JDK location, and contains JDK binaries" + System.lineSeparator() +
            "for your platform that you have permission to execute.";

    JDKNotFoundException() {
        super(DEFAULT_MESSAGE);
    }
}