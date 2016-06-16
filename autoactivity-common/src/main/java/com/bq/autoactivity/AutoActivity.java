package com.bq.autoactivity;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
@Documented
public @interface AutoActivity {
    Class<?>[] modules() default {};

    Class<?>[] dependencies() default {};
}
