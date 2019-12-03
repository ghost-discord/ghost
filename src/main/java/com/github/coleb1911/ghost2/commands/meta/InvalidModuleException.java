package com.github.coleb1911.ghost2.commands.meta;

import java.util.Set;
import java.util.StringJoiner;

/**
 * Thrown to indicate that a {@link Module} subclass is written incorrectly.
 */
public class InvalidModuleException extends RuntimeException {

    private final Class<? extends Module> module;
    private final Set<Reason> reasons;

    /**
     * Constructs a new InvalidModuleException with the problem module and the given Reason.
     *
     * @param module The Module that caused the exception
     * @param reason The Reason for the exception
     */
    public InvalidModuleException(Class<? extends Module> module, Reason reason) {
        this(module, Set.of(reason));
    }

    /**
     * Constructs a new InvalidModuleException with the problem module and the given Reasons.
     *
     * @param module  The Module that caused the exception
     * @param reasons Set of Reasons for the exception
     */
    InvalidModuleException(Class<? extends Module> module, Set<Reason> reasons) {
        super(formatErrorMessage(module.getName(), Reason.concat(reasons)));
        this.module = module;
        this.reasons = reasons;
    }

    /**
     * Constructs a new InvalidModuleException with the Reason {@link Reason#EXCEPTION_IN_CONSTRUCTOR EXCEPTION_IN_CONSTRUCTOR},
     * the problem Module, and the exception that came from its constructor. If the exception passed in is an
     * InvalidModuleException, the reason(s) from that exception will be appended to this exception's message.
     *
     * @param module                 The Module that caused the exception
     * @param exceptionInConstructor The exception from its cosntructor
     */
    public InvalidModuleException(Class<? extends Module> module, Throwable exceptionInConstructor) {
        super(formatErrorMessage(module.getName(),
                (exceptionInConstructor instanceof InvalidModuleException) ?
                        Reason.EXCEPTION_IN_CONSTRUCTOR + exceptionInConstructor.getClass().getSimpleName() + ": " + ((InvalidModuleException) exceptionInConstructor).getReasonsString() :
                        Reason.EXCEPTION_IN_CONSTRUCTOR + exceptionInConstructor.getClass().getName()));
        this.module = module;
        this.reasons = Set.of(Reason.EXCEPTION_IN_CONSTRUCTOR);
    }

    /**
     * Formats the error message.
     *
     * @param moduleName The module name
     * @param reasons    The reasons description. Might contain more than one reason.
     */
    private static String formatErrorMessage(String moduleName, String reasons) {
        return "Module subclass "
                + moduleName
                + " is written incorrectly. Refer to the wiki for help. (Reason(s): "
                + reasons
                + ")";

    }

    /**
     * @return A {@code String} containing all the messages of each {@code Reason} for this {@code InvalidModuleException}
     */
    private String getReasonsString() {
        return Reason.concat(reasons);
    }

    /**
     * @return The {@code Module} that caused this {@code InvalidModuleException}
     */
    public Class<? extends Module> getModule() {
        return module;
    }

    /**
     * The many causes of an {@code InvalidModuleException}.
     */
    public enum Reason {
        EXCEPTION_IN_CONSTRUCTOR("Module threw an exception in its constructor. "),
        NOT_INSTANTIABLE("Module is not visible, is missing a public no-args constructor, or is a non-instantiable type"),
        INVALID_NAME("Module has a null, empty, or all-whitespace name"),
        INVALID_DESCRIPTION("Module has a null, empty, or all-whitespace description"),
        INVALID_BOT_PERMISSIONS("Module has a null bot permission list"),
        INVALID_USER_PERMISSIONS("Module has a null user permission list"),
        INVALID_TYPE("Module has a null type"),
        INVALID_ALIASES("Module has a null alias list");

        private final String message;

        Reason(String message) {
            this.message = message;
        }

        static String concat(Set<Reason> reasons) {
            StringJoiner joiner = new StringJoiner("; ");
            for (Reason reason : reasons) {
                joiner.add(reason.message);
            }
            return joiner.toString();
        }

        @Override
        public String toString() {
            return message;
        }
    }
}