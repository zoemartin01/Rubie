package me.zoemartin.rubie.core.annotations;

import net.dv8tion.jda.api.Permission;

public @interface Checks {
    @interface Permissions {
        @interface Guild {
            Permission[] value();
        }

        @interface Channel {
            Permission[] value();
        }
    }
}
