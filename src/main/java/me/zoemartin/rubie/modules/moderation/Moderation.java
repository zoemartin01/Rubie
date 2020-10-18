package me.zoemartin.rubie.modules.moderation;

import me.zoemartin.rubie.core.interfaces.Module;
import me.zoemartin.rubie.core.managers.CommandManager;
import me.zoemartin.rubie.core.util.DatabaseUtil;

public class Moderation implements Module {
    @Override
    public void init() {
        DatabaseUtil.setMapped(WarnEntity.class);
        CommandManager.register(new Warn());
        CommandManager.register(new RoleManagement());
    }
}
