package com.omen.framework.annotation;

import java.lang.annotation.*;

/**
 * @Description:
 * @Auther: xuzhoukai
 * @Date: 2019/4/7 18:58
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MyResponeBody {
    String value() default "";
}
