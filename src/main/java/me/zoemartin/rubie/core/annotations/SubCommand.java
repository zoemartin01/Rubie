package me.zoemartin.rubie.core.annotations;

import me.zoemartin.rubie.core.interfaces.AbstractCommand;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SubCommand {
    Class<? extends AbstractCommand> value();

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface AsBase {
        String name();

        String[] alias() default {};
    }
}
