package com.bq.autocontainer;


import java.lang.annotation.*;

/**
 * This annotation is a mirror for {@link dagger.Component}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AutoContainer {
    Class<?>[] modules() default {};

    Class<?>[] dependencies() default {};

    Class<?> baseClass() default Object.class;

    String className() default "";

    Class<? extends Annotation> scope() default ContainerScope.class;
}
