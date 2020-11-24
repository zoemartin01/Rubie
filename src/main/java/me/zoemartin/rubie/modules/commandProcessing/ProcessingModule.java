package me.zoemartin.rubie.modules.commandProcessing;

import com.google.auto.service.AutoService;
import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.interfaces.Module;
import me.zoemartin.rubie.core.managers.CommandManager;

@AutoService(Module.class)
public class ProcessingModule implements Module {
    @Override
    public void init() {
        Bot.addListener(new CommandListener());
        CommandManager.setCommandProcessor(new CommandHandler());
    }

    @Override
    public void initLate() {
        PermissionHandler.initPerms();
        Prefixes.init();
    }
}
