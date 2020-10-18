package me.zoemartin.rubie.core.interfaces;

import me.zoemartin.rubie.core.CommandPerm;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import javax.annotation.Nonnull;
import java.util.*;

public interface Command {
    /**
     * Returns  collection containing all direct sub commands. If no sub commands exist this doesn't have to be
     * overwritten
     *
     * @return a collection containing all direct sub commands
     */
    @Nonnull
    default Set<Command> subCommands() {
        return Collections.emptySet();
    }

    /**
     * Returns the name of the command. This is used in {@link #usage()} and as the invoking string if {@link #regex()}
     * is not set.
     *
     * @return the name of the command
     */
    @Nonnull
    String name();

    /**
     * Returns a regex with which the command may be found. Aliases can be set this way. If no aliases should be set
     * this doesn't have to be overwritten
     *
     * @return the commands regex
     */
    @Nonnull
    default String regex() {
        return name();
    }

    /**
     * The main functionality of the command
     * @param user the user executing the command
     * @param channel the channel the command is executed in
     * @param args the arguments that are passed
     * @param original the original message
     * @param invoked the string that invoked the command
     */
    void run(User user, MessageChannel channel, List<String> args, Message original, String invoked);

    /**
     * Returns the {@link CommandPerm} needed to execute the command
     *
     * @return the CommandPerm needed to execute the command
     */
    @Nonnull
    CommandPerm commandPerm();

    /**
     * Returns a collection of discord {@link Permission}s a user needs to execute the command. If no special
     * permissions are required, this doesn't have to be overwritten
     *
     * @return a collection of discord permissions a user needs to execute the command
     */
    @Nonnull
    default Collection<Permission> required() {
        return Collections.singleton(Permission.UNKNOWN);
    }

    /**
     * The command's parameters. If this command does not take any parameters this shouldn't be overwritten
     *
     * @return the command's parameters
     */
    @Nonnull
    default String usage() {
        return name();
    }

    /**
     * Returns a short description of the command
     *
     * @return the commands description
     */
    @Nonnull
    String description();

    /**
     * Returns a detailed help message of the command if needed
     *
     * @return the detailed help message
     */
    @Nonnull
    default String detailedHelp() {
        return "";
    }
}
