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
     * Callback method specific priority. If no value is specified the plugin priority is used.
     *
     * @see Plugin#priority()
     */
    int priority() default Integer.MIN_VALUE;


    /**
     * Callback method relative priority. This will be added or subtracted to the specific priority.
     *
     * @see Plugin#priority()
     */
    int relativePriority() default 0;

    enum CallSuper {
        BEFORE, AFTER, UNSPECIFIED
    }
}
