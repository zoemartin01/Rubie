package me.zoemartin.rubie.modules.misc;

import me.zoemartin.rubie.core.LoadModule;
import me.zoemartin.rubie.core.interfaces.Module;
import me.zoemartin.rubie.core.managers.CommandManager;

@LoadModule
public class Misc implements Module {
    @Override
    public void init() {
        CommandManager.register(new Enlarge());
        CommandManager.register(new Avatar());
        CommandManager.register(new Find());
    }
}
