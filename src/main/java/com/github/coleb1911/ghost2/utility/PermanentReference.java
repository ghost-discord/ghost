package com.github.coleb1911.ghost2.utility;

/**
 * An object reference that may only be updated once.
 *
 * @param <T> The type of object referred to by this reference
 */
public final class PermanentReference<T> {
    private T value;
    private boolean finalized = false;

    /**
     * Creates a new PermanentReference.
     */
    public PermanentReference() {
    }

    /**
     * Creates a new PermanentReference with the given value.
     *
     * @param value Initial value
     */
    public PermanentReference(T value) {
        set(value);
    }

    /**
     * Sets the value. May only be called once.
     *
     * @throws IllegalStateException if the value has already been set
     */
    public void set(T value) {
        if (finalized) throw new IllegalStateException("PermanentReference has already been finalized");
        this.value = value;
        finalized = true;
    }

    /**
     * Gets the value, if it exists.
     */
    public T get() {
        return value;
    }

    /**
     * Returns the String representation of the value.
     */
    @Override
    public String toString() {
        return value.toString();
    }
}