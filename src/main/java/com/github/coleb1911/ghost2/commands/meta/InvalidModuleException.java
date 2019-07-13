package com.github.coleb1911.ghost2.commands.meta;

import java.util.StringJoiner;

/**
 * Thrown to indicate that a {@link Module} subclass is written incorrectly.
 */
public class InvalidModuleException extends RuntimeException {
    private static final String MESSAGE = "Module subclass %s is written incorrectly. Refer to the wiki for help. (Reason(s): %s)";

    private final Reason[] reasons;

    /**
     * Constructs a new InvalidModuleException with the problem module and the given Reasons.
     *
     * @param module  The Module that caused the exception
     * @param reasons List of Reasons that the Module is written incorrectly
     */
    public InvalidModuleException(Class<? extends Module> module, Reason... reasons) {
        super(String.format(MESSAGE, module.getName(), Reason.concat(reasons)));
        this.reasons = reasons;
    }

    /**
     * Constructs a new InvalidModuleException with the Reason {@link Reason#EXCEPTION_IN_CONSTRUCTOR EXCEPTION_IN_CONSTRUCTOR},
     * the problem Module, and the exception that came from its constructor. If the exception passed in is an
     * InvalidModuleException, the reason(s) from that exception will be appended to this exception's message.
     *
     * @param module The Module that caused the exception
     * @param cause  The exception from its cosntructor
     */
    public InvalidModuleException(Class<? extends Module> module, Throwable cause) {
        super(String.format(MESSAGE, module.getName(),
                (cause instanceof InvalidModuleException) ?
                        Reason.EXCEPTION_IN_CONSTRUCTOR + cause.getClass().getSimpleName() + ": " + ((InvalidModuleException) cause).getReasonsString() :
                        Reason.EXCEPTION_IN_CONSTRUCTOR + cause.getClass().getName()));
        this.reasons = new Reason[]{Reason.EXCEPTION_IN_CONSTRUCTOR};
    }

    private String getReasonsString() {
        return Reason.concat(reasons);
    }

    /**
     * The many causes of an {@code InvalidModuleException}.
     */
    public enum Reason {
        EXCEPTION_IN_CONSTRUCTOR("Module threw an exception in its constructor. "),
        NOT_INSTANTIABLE("Module is missing a public constructor or is non-instantiable"),
        INVALID_NAME("Module has a null, empty, or all-whitespace name"),
        INVALID_DESCRIPTION("Module has a null, empty, or all-whitespace description"),
        INVALID_BOT_PERMISSIONS("Module has a null bot permission list"),
        INVALID_USER_PERMISSIONS("Module has a null user permission list"),
        INVALID_TYPE("Module has a null type"),
        INVALID_ALIASES("Module has a null alias array");

        private final String message;

        Reason(String message) {
            this.message = message;
        }

        static String concat(Reason... reasons) {
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