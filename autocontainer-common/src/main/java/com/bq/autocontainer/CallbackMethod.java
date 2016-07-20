package com.bq.autocontainer;


/**
 * This class serves as an abstraction for a method in a container that may be overridden by any plugin.
 * Common use case is Android Activity methods, that are based on inheritance.
 * <p>
 * In order to override an activity method you must declare a {@code CallbackMethod<ReturnType>} as the first argument
 * followed by the proper container methods.
 * <p>
 * To specify the return value use {@link #override(ReturnType)}.
 * Trying to override a method overridden by other component will produce an exception.
 * <pre>{@code
 * @Callback
 * public void onCreateOptionsMenu(CallbackMethod<Boolean> m, Menu menu) {
 * if(m.overridden()) return;
 * // Do something with the menu
 * m.override(true); // Return true in the activity callback method
 * }
 * }</pre>
 * Instances of this class are generated automatically by the compiler.
 * <p>
 * Instances are shared between invocations to avoid unnecessary garbage. Do <strong>NOT</strong>
 * keep strong references to this class or the container will be leaked.
 */
public abstract class CallbackMethod<ReturnType> {

    private ReturnType returnValue;
    private boolean overridden = false;
    private Object consumedBy;
    private Object borrowedBy;

    /**
     * Track the current plugin that is using this method.Do not call this method directly, invocations are handled by the compiler.
     */
    public void borrow(Object borrower) {
        borrowedBy = borrower;
    }

    /**
     * Reset any temporal state. Do not call this method directly, invocations are handled by the compiler.
     */
    public void reset() {
        this.consumedBy = null;
        this.borrowedBy = null;
        this.overridden = false;
    }

    /**
     * Same as {@link #override(ReturnType)} with {@code returnValue = null}.
     * This is only acceptable for non-primitive value types, otherwise an NPE will be thrown when unboxing.
     */
    public void override() {
        this.override(null);
    }

    /**
     * Override the return value for this method in the container.
     * This will skip the container superclass behaviour unless you manually use any of the {@link #call()} methods.
     * <p>
     * Calling this method more than once form the same source invocation will produce an exception, always check
     * if any other component has overridden the value with {@link #overridden()}.
     */
    public void override(ReturnType returnValue) {
        if (overridden()) {
            throw new IllegalStateException(
                    "This event was already overridden by: " + (consumedBy != null ? consumedBy.toString() : "UNKNOWN"));
        }
        this.returnValue = returnValue;
        this.consumedBy = borrowedBy;
        this.overridden = true;
    }

    /**
     * @return {@code true} if {@link #override()} was called, {@code false} otherwise.
     */
    public boolean overridden() {
        return overridden;
    }

    /**
     * @return the value provided in {@link #override(Object)}. Might be null.
     */
    public ReturnType getOverriddenValue() {
        return returnValue;
    }

    /**
     * Call the container method with the provided arguments.
     * Parameter count and type must match.
     */
    public ReturnType call() {
        throw new UnsupportedOperationException("This method expected different arguments.");
    }

    /**
     * Call the container method with the provided arguments.
     * Parameter count and type must match.
     */
    public ReturnType call(Object arg0) {
        throw new UnsupportedOperationException("This method expected different arguments.");
    }

    /**
     * Call the container method with the provided arguments.
     * Parameter count and type must match..
     */
    public ReturnType call(Object arg0, Object arg1) {
        throw new UnsupportedOperationException("This method expected different arguments.");
    }

    /**
     * Call the container method with the provided arguments.
     * Parameter count and type must match.
     */
    public ReturnType call(Object arg0, Object arg1, Object arg2) {
        throw new UnsupportedOperationException("This method expected different arguments.");
    }

    /**
     * Call the container method with the provided arguments.
     * Parameter count and type must match.
     */
    public ReturnType call(Object arg0, Object arg1, Object arg2, Object arg3) {
        throw new UnsupportedOperationException("This method expected different arguments.");
    }

    /**
     * Call the container method with the provided arguments.
     * Parameter count and type must match.
     */
    public ReturnType call(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object... arg5) {
        throw new UnsupportedOperationException("This method expected different arguments.");
    }
}
