package com.link.codegen.annotation;

import java.lang.annotation.*;

/**
 * @author 84168
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target({ElementType.FIELD})
public @interface TypeConverter {
  String toTypeFullName() default "java.lang.String";

}
