package me.zoemartin.rubie.core;

import me.zoemartin.rubie.core.annotations.*;
import me.zoemartin.rubie.core.interfaces.AbstractCommand;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import net.dv8tion.jda.api.Permission;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class SubcommandAdapter<C extends AbstractCommand> extends AbstractCommand {
    private final C c;

    @Override
    public void run(CommandEvent event) {
        c.run(event);
    }

    public SubcommandAdapter(C c) {
        if (c.getClass().getAnnotationsByType(SubCommand.AsBase.class).length == 0)
            throw new IllegalStateException(
                "Command Class missing @SubCommand.AsBase annotation to be used with the SubcommandAdapter");

        this.c = c;
    }

    @NotNull
    @Override
    public Set<AbstractCommand> subCommands() {
        return c.subCommands();
    }

    @NotNull
    @Override
    public String name() {
        return c.getClass().getAnnotationsByType(SubCommand.AsBase.class)[0].name();
    }

    @NotNull
    @Override
    public String regex() {
        SubCommand.AsBase[] options = c.getClass().getAnnotationsByType(SubCommand.AsBase.class);
        if (options[0].alias().length == 0) return options[0].name();

        return options[0].name() + "|" + String.join("|", options[0].alias());
    }

    @NotNull
    @Override
    public CommandPerm commandPerm() {
        CommandOptions[] options = c.getClass().getAnnotationsByType(CommandOptions.class);

        if (options == null || options.length == 0)
            throw new IllegalStateException(String.format("Command '%s' missing CommandOptions Annotation", this.getClass().getName()));

        return options[0].perm();
    }

    @NotNull
    @Override
    public Collection<Permission> required() {
        Checks.Permissions.Guild[] options = c.getClass().getAnnotationsByType(Checks.Permissions.Guild.class);

        if (options == null || options.length == 0)
            return Collections.singleton(Permission.UNKNOWN);

        return Arrays.asList(options[0].value());
    }

    @NotNull
    @Override
    public String usage() {
        CommandOptions[] options = c.getClass().getAnnotationsByType(CommandOptions.class);

        if (options == null || options.length == 0)
            throw new IllegalStateException("Command Class missing CommandOptions Annotation");

        return options[0].usage();
    }

    @NotNull
    @Override
    public String description() {
        CommandOptions[] options = c.getClass().getAnnotationsByType(CommandOptions.class);

        if (options == null || options.length == 0)
            throw new IllegalStateException("Command Class missing CommandOptions Annotation");

        return options[0].description();
    }
}
