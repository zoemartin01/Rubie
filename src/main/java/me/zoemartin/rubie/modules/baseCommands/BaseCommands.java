package me.zoemartin.rubie.modules.baseCommands;

import me.zoemartin.rubie.core.interfaces.Module;
import me.zoemartin.rubie.core.managers.CommandManager;
import me.zoemartin.rubie.core.annotations.LoadModule;

@LoadModule
public class BaseCommands implements Module {
    @Override
    public void init() {
        Help.helper();
    }
}
