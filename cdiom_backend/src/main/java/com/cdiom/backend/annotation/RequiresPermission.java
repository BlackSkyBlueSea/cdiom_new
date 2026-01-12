package com.cdiom.backend.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限注解
 * 用于标记需要特定权限才能访问的方法
 * 
 * @author cdiom
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {

    /**
     * 权限代码（支持多个，只要有一个即可）
     */
    String[] value();

    /**
     * 是否要求所有权限都满足（默认false，即任意一个满足即可）
     */
    boolean requireAll() default false;
}



