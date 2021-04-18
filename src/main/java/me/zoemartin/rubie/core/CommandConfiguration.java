package me.zoemartin.rubie.core;

import me.zoemartin.rubie.core.interfaces.AbstractCommand;
import net.dv8tion.jda.api.Permission;

import java.util.Set;

public class CommandConfiguration {
    private final Set<AbstractCommand> subCommands;
    private final String name;
    private final CommandPerm perm;
    private final String usage;
    private final String description;
    private final Permission[] botPerms;
    private final String[] alias;
    private final String help;
    private final boolean hidden;

    public CommandConfiguration(Set<AbstractCommand> subCommands, String name, CommandPerm perm, String usage, String description, Permission[] botPerms, String[] alias, String help, boolean hidden) {
        this.subCommands = subCommands;
        this.name = name;
        this.perm = perm;
        this.usage = usage;
        this.description = description;
        this.botPerms = botPerms;
        this.alias = alias;
        this.help = help;
        this.hidden = hidden;
    }

    public Set<AbstractCommand> getSubCommands() {
        return subCommands;
    }

    public String getName() {
        return name;
    }

    public CommandPerm getPerm() {
        return perm;
    }

    public String getUsage() {
        return usage;
    }

    public String getDescription() {
        return description;
    }

    public Permission[] getBotPerms() {
        return botPerms;
    }

    public String[] getAlias() {
        return alias;
    }

    public String getHelp() {
        return help;
    }

    public boolean isHidden() {
        return hidden;
    }
}
