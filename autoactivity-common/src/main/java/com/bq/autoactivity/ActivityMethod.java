package com.bq.autoactivity;

public abstract class ActivityMethod<ReturnType> {

    private ReturnType overriddenReturnValue;
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

    public void override(ReturnType overriddenReturnValue) {
        if (overridden()) {
            throw new IllegalStateException(
                    "This event was already overridden by: " + (consumedBy != null ? consumedBy.toString() : "UNKNOWN"));
        }
        this.overriddenReturnValue = overriddenReturnValue;
        consumedBy = borrowedBy;
        overridden = true;
    }

    public void captureArguments(Object... args) {
        //No-op
    }


    public void releaseArguments() {
        //No-op
    }


    public boolean overridden() {
        return overridden;
    }

    public ReturnType getOverriddenValue() {
        return overriddenReturnValue;
    }

    public abstract ReturnType callActivityMethod();
}
