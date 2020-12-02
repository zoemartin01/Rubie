package me.zoemartin.rubie.modules.reactroles;

import com.google.auto.service.AutoService;
import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.interfaces.Module;

@AutoService(Module.class)
public class ReactModule implements Module {
    @Override
    public void init() {
        Bot.addListener(new ReactListener());
    }

    @Override
    public void initLate() {
        ReactManager.init();
    }
}
