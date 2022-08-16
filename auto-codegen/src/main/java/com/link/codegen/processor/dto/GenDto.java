package com.link.codegen.processor.dto;

import java.lang.annotation.*;

/**
 * @author lin 2022/8/16 23:44
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GenDto {
    String pkgName();

    String sourcePath() default "src/main/java";

    boolean overrideSource() default false;

    boolean jpa() default true;

}
