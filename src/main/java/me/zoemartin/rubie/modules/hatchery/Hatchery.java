package me.zoemartin.rubie.modules.hatchery;

import me.zoemartin.rubie.core.LoadModule;
import me.zoemartin.rubie.core.interfaces.Module;
import me.zoemartin.rubie.core.managers.CommandManager;
import net.dv8tion.jda.api.entities.Guild;

@LoadModule
public class Hatchery implements Module {
    @Override
    public void init() {
        CommandManager.register(new MVC());
        CommandManager.register(new MVCRefresh());
    }
}
