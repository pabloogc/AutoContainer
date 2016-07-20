package com.bq.autocontainer;

import java.lang.annotation.*;


@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Callback {
    /**
     * When to call this callback relative to super invocation.
     * Unspecified will use {@link CallSuper#BEFORE} for simple callbacks, and {@link CallSuper#AFTER} for
     * callbacks that may override the container.
     * <p>
     * Using {@link CallSuper#BEFORE} and overriding is an error (can't override after calling super).
     */
    CallSuper callSuper() default CallSuper.UNSPECIFIED;

    /**
     * The order to execute callbacks. If two classes have the same priority
     * alphabetical order by class name is used. Same name and priority will lead to undefined behaviour.
     * <p>
     * Priority is ascending, lower means earlier call.
     * <p>
     * Defaults to 100.
     */
    int priority() default 100;

    enum CallSuper {
        BEFORE, AFTER, UNSPECIFIED
    }
}
