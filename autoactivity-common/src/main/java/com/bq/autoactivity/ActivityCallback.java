package com.bq.autoactivity;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ActivityCallback {
    CallSuper callSuper() default CallSuper.UNSPECIFIED;

    enum CallSuper {
        BEFORE, AFTER, UNSPECIFIED
    }
}
