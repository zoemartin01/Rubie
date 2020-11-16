package me.zoemartin.rubie.core.annotations;

import me.zoemartin.rubie.core.CommandPerm;
import net.dv8tion.jda.api.Permission;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandOptions {
    String name();

    CommandPerm perm() default CommandPerm.EVERYONE;

    String usage() default "";

    String description();

    Permission[] botPerms() default {};

    String[] alias() default {};

    @interface Alias {
        String[] value();
    }
}
