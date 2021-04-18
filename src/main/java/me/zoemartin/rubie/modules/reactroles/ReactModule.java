package me.zoemartin.rubie.modules.reactroles;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.annotations.Module;
import me.zoemartin.rubie.core.interfaces.ModuleInterface;

@Module
public class ReactModule implements ModuleInterface {
    @Override
    public void init() {
        Bot.addListener(new ReactListener());
    }

    @Override
    public void initLate() {
        ReactManager.init();
    }
}
