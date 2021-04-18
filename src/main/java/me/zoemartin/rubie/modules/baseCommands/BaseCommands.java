package me.zoemartin.rubie.modules.baseCommands;

import me.zoemartin.rubie.core.annotations.Module;
import me.zoemartin.rubie.core.interfaces.ModuleInterface;

@Module
public class BaseCommands implements ModuleInterface {
    @Override
    public void init() {
        Help.helper();
    }
}
