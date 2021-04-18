package me.zoemartin.rubie.core.annotations;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Setter {
    String name() default "";

    String type() default "";
}