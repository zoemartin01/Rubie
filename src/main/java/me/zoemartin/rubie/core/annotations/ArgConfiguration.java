package me.zoemartin.rubie.core.annotations;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ArgConfiguration {
    Arguments[] value();
}
