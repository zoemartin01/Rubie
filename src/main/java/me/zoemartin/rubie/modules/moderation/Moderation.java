package me.zoemartin.rubie.modules.moderation;

import me.zoemartin.rubie.core.annotations.LoadModule;
import me.zoemartin.rubie.core.interfaces.Module;
import me.zoemartin.rubie.core.managers.CommandManager;
import me.zoemartin.rubie.core.util.DatabaseUtil;

@LoadModule
public class Moderation implements Module {
    @Override
    public void init() {
        DatabaseUtil.setMapped(ModLogEntity.class);
        DatabaseUtil.setMapped(NoteEntity.class);
    }
}
