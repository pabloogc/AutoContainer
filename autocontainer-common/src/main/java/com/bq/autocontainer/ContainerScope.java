package com.bq.autocontainer;

import javax.inject.Scope;
import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Scope
public @interface ContainerScope {
}