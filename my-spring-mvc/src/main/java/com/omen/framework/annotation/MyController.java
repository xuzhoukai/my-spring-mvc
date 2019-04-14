package com.omen.framework.annotation;

import java.lang.annotation.*;

/**
 * @Description:
 * @Auther: xuzhoukai
 * @Date: 2019/4/7 18:56
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface MyController {
    String value() default "";
}
