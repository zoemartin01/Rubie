package me.zoemartin.rubie.modules.Export;

import me.zoemartin.rubie.core.LoadModule;
import me.zoemartin.rubie.core.interfaces.Module;

import static me.zoemartin.rubie.core.managers.CommandManager.register;

@LoadModule
public class Export implements Module {
    @Override
    public void init() {
        register(new Notes());
    }
}
