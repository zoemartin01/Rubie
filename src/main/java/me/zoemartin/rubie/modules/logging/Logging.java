package me.zoemartin.rubie.modules.logging;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.annotations.Module;
import me.zoemartin.rubie.core.interfaces.ModuleInterface;

@Module
public class Logging implements ModuleInterface {
    @Override
    public void init() {
        Bot.addListener(new Nickname.NicknameListener());
        Bot.addListener(new Username.UsernameListener());
    }
}
