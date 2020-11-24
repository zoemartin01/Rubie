package me.zoemartin.rubie.modules.baseCommands;

import com.google.auto.service.AutoService;
import me.zoemartin.rubie.core.interfaces.Module;

@AutoService(Module.class)
public class BaseCommands implements Module {
    @Override
    public void init() {
        Help.helper();
    }
}
