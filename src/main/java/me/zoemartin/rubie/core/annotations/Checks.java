package me.zoemartin.rubie.core.annotations;

import net.dv8tion.jda.api.Permission;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Checks {
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Permissions {
        @Target(ElementType.TYPE)
        @Retention(RetentionPolicy.RUNTIME)
        @interface Guild {
            Permission[] value();
        }

        @Target(ElementType.TYPE)
        @Retention(RetentionPolicy.RUNTIME)
        @interface Channel {
            Permission[] value();
        }
    }
}
