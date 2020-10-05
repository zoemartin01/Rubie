package me.zoemartin.rubie.modules.commandProcessing;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.LoadModule;
import me.zoemartin.rubie.core.interfaces.Module;
import me.zoemartin.rubie.core.managers.CommandManager;
import me.zoemartin.rubie.core.util.DatabaseUtil;

@LoadModule
public class ProcessingModule implements Module {
    @Override
    public void init() {
        Bot.addListener(new CommandListener());
        CommandManager.setCommandProcessor(new CommandHandler());
        DatabaseUtil.setMapped(MemberPermission.class);
        DatabaseUtil.setMapped(RolePermission.class);
        DatabaseUtil.setMapped(Prefixes.class);
        DatabaseUtil.setMapped(LoggedError.class);
    }

    @Override
    public void initLate() {
        PermissionHandler.initPerms();
        Prefixes.init();
    }
}
