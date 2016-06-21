package com.bq.autoactivity;

import android.app.Activity;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is a mirror for {@link dagger.Component}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AutoActivity {
    Class<?>[] modules() default {};

    Class<?>[] dependencies() default {};

    Class<? extends Activity> baseActivity() default Activity.class;

    Class<? extends Annotation> scope();
}
