package com.omen.framework.annotation;

import java.lang.annotation.*;

/**
 * @Description:
 * @Auther: xuzhoukai
 * @Date: 2019/4/7 18:57
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface MyAutoWired {
    String value() default "";
}
