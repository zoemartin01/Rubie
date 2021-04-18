package me.zoemartin.rubie.modules.commandProcessing;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.annotations.Module;
import me.zoemartin.rubie.core.interfaces.ModuleInterface;
import me.zoemartin.rubie.core.managers.CommandManager;

@Module
public class ProcessingModule implements ModuleInterface {
    @Override
    public void init() {
        Bot.addListener(new CommandListener());
        Bot.addListener(new GuildJoin());
        CommandManager.setCommandProcessor(new CommandHandler());
    }

    @Override
    public void initLate() {
        PermissionHandler.initPerms();
        Prefixes.init();
    }
}
