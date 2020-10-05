package me.zoemartin.rubie.modules.baseCommands;

import me.zoemartin.rubie.core.interfaces.Module;
import me.zoemartin.rubie.core.managers.CommandManager;
import me.zoemartin.rubie.core.LoadModule;

@LoadModule
public class BaseCommands implements Module {
    @Override
    public void init() {
        CommandManager.register(new Ping());
        CommandManager.register(new Help());
        CommandManager.register(new Usage());
        CommandManager.register(new About());
        CommandManager.register(new UserInfo());
        CommandManager.register(new RoleInfo());
        CommandManager.register(new ServerInfo());
        CommandManager.register(new Permission());
        CommandManager.register(new Prefix());
        Help.helper();
    }
}
