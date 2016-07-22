package com.bq.autocontainer;

import java.lang.annotation.*;


@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Plugin {

    /**
     * The order to execute callbacks. If two plugins have the same priority
     * alphabetical order by class name is used. Same name and priority will lead to undefined behaviour.
     * <p>
     * Priority is ascending, lower means earlier call.
     * <p>
     * Defaults to {@link Priority#MID}.
     */
    int priority() default Priority.MID;
}
