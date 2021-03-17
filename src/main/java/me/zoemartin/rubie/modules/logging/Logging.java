package me.zoemartin.rubie.modules.logging;

import com.google.auto.service.AutoService;
import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.interfaces.Module;

@AutoService(Module.class)
public class Logging implements Module {
    @Override
    public void init() {
        Bot.addListener(new Nickname.NicknameListener());
        Bot.addListener(new Username.UsernameListener());
    }
}
