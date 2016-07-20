package com.bq.autocontainer;

import java.lang.annotation.*;


@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Callback {
    CallSuper callSuper() default CallSuper.UNSPECIFIED;

    enum CallSuper {
        BEFORE, AFTER, UNSPECIFIED
    }
}
