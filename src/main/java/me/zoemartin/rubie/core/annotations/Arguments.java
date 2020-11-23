package me.zoemartin.rubie.core.annotations;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ArgConfiguration.class)
public @interface Arguments {
    Class<?>[] value() default {};


}
