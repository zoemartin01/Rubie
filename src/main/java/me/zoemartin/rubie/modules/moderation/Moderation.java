package me.zoemartin.rubie.modules.moderation;

import me.zoemartin.rubie.core.LoadModule;
import me.zoemartin.rubie.core.interfaces.Module;
import me.zoemartin.rubie.core.managers.CommandManager;
import me.zoemartin.rubie.core.util.DatabaseUtil;

@LoadModule
public class Moderation implements Module {
    @Override
    public void init() {
        DatabaseUtil.setMapped(ModLogEntity.class);
        DatabaseUtil.setMapped(NoteEntity.class);
        CommandManager.register(new Warn());
        CommandManager.register(new RoleManagement());
        CommandManager.register(new Note());
        CommandManager.register(new BackgroundCheck());
        CommandManager.register(new ModLogs());
    }
}
