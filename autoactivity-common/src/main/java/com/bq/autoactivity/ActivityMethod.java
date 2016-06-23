package com.bq.autoactivity;

public abstract class ActivityMethod<ReturnType> {

    private ReturnType returnValue;
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
        releaseArguments();
    }

    public void override() {
        this.override(null);
    }

    public void override(ReturnType returnValue) {
        if (overridden()) {
            throw new IllegalStateException(
                    "This event was already overridden by: " + (consumedBy != null ? consumedBy.toString() : "UNKNOWN"));
        }
        this.returnValue = returnValue;
        this.consumedBy = borrowedBy;
        this.overridden = true;
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
        return returnValue;
    }

    public abstract ReturnType callActivityMethod();
}
