package com.bq.autoactivity;

public abstract class ActivityMethod<T> {

    private T overriddenReturnValue;
    private boolean overridden = false;
    private Object consumedBy;
    private Object borrowedBy;

    public void borrow(Object borrower) {
        borrowedBy = borrower;
    }

    public void reset() {
        this.consumedBy = null;
        this.borrowedBy = null;
        this.overridden = false;
    }

    public void override() {
        this.override(null);
    }

    public void override(T overriddenReturnValue) {
        if (overridden()) {
            throw new IllegalStateException(
                    "This event was already overridden by: " + (consumedBy != null ? consumedBy.toString() : "UNKNOWN"));
        }
        this.overriddenReturnValue = overriddenReturnValue;
        consumedBy = borrowedBy;
        overridden = true;
    }

    public void captureArguments(Object... args) {

    }


    public void releaseArguments() {

    }


    public boolean overridden() {
        return overridden;
    }

    public T getOverriddenValue() {
        return overriddenReturnValue;
    }

    public abstract T callActivityMethod();
}
