package me.zoemartin.rubie.core;

import me.zoemartin.rubie.core.interfaces.Module;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface LoadModule {
    Class<? extends Module>[] loadAfter() default {};
}
