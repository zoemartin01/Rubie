package me.zoemartin.rubie.modules.moderation;

import com.google.auto.service.AutoService;
import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.interfaces.Module;

@AutoService(Module.class)
public class Moderation implements Module {
    @Override
    public void init() {
        Bot.addListener(new CarlNoteWatcher());
    }
}
