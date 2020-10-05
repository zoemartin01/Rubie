package me.zoemartin.rubie.core.interfaces;

import me.zoemartin.rubie.core.CommandPerm;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.*;

public interface Command {
    default Set<Command> subCommands() {
        return Collections.emptySet();
    }

    String name();

    default String regex() {
        return name();
    }

    void run(User user, MessageChannel channel, List<String> args, Message original, String invoked);

    CommandPerm commandPerm();

    default Collection<Permission> required() {
        return Set.of(Permission.UNKNOWN);
    }

    default String usage() {
        return name();
    }

    String description();
}
