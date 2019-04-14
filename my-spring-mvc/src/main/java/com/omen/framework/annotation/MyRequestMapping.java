package com.omen.framework.annotation;

import java.lang.annotation.*;

/**
 * @Description:
 * @Auther: xuzhoukai
 * @Date: 2019/4/7 18:56
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD,ElementType.TYPE})
public @interface MyRequestMapping {
    String value() default "";
}
