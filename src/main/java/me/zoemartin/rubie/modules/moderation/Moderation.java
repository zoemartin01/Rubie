package me.zoemartin.rubie.modules.moderation;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.annotations.Module;
import me.zoemartin.rubie.core.interfaces.ModuleInterface;

@Module
public class Moderation implements ModuleInterface {
    @Override
    public void init() {
        Bot.addListener(new CarlNoteWatcher());
        Bot.addListener(new Subscribe.Listeners());
    }

    @Override
    public void initLate() {
        SubscriptionManager.init();
    }
}
