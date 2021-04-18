package me.zoemartin.rubie.core.interfaces;

import me.zoemartin.rubie.core.*;
import me.zoemartin.rubie.core.annotations.*;
import org.apache.commons.lang3.StringUtils;
import org.reflections8.Reflections;
import org.reflections8.scanners.SubTypesScanner;
import org.reflections8.scanners.TypeAnnotationsScanner;
import org.reflections8.util.*;

import javax.annotation.Nonnull;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractCommand {
    protected final CommandConfiguration configuration;

    @SuppressWarnings("unchecked")
    protected AbstractCommand() {
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                                                      .setUrls(ClasspathHelper.forPackage(this.getClass().getPackageName()))
                                                      .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner())
                                                      .filterInputsBy(new FilterBuilder().includePackage(this.getClass().getPackageName()))
                                                      .setExecutorService(Executors.newFixedThreadPool(4)));

        Set<Class<?>> commandReflect = reflections.getTypesAnnotatedWith(SubCommand.class);

        Set<AbstractCommand> subCommands = commandReflect.stream()
                                               .filter(c -> c.getAnnotationsByType(SubCommand.class)[0].value() == this.getClass())
                                               .filter(c -> c.getAnnotationsByType(Disabled.class).length == 0)
                                               .map(c -> {
                                                   AbstractCommand sub = null;
                                                   try {
                                                       Constructor<? extends AbstractCommand> constructor =
                                                           (Constructor<? extends AbstractCommand>)
                                                               Arrays.stream(c.getDeclaredConstructors()).findAny().orElseThrow(
                                                                   () -> new IllegalStateException("Command Class missing a public no-args Constructor")
                                                               );
                                                       constructor.setAccessible(true);
                                                       sub = constructor.newInstance();
                                                   } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                                                       System.err.println(e.getMessage());
                                                   }
                                                   if (sub == null) return null;

                                                   sub.subCommands();
                                                   return sub;
                                               }).filter(Objects::nonNull)
                                               .collect(Collectors.toSet());

        if (this.getClass().getAnnotationsByType(CommandOptions.class).length == 0)
            throw new IllegalStateException("Command Class missing CommandOptions Annotation");

        CommandOptions options = this.getClass().getAnnotationsByType(CommandOptions.class)[0];

        configuration = new CommandConfiguration(subCommands, options.name(), options.perm(), options.usage(),
            options.description(), options.botPerms(), options.alias(), options.help(), options.hidden());
    }

    protected AbstractCommand(CommandConfiguration configuration) {
        this.configuration = configuration;
    }

    public CommandConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Returns  collection containing all direct sub commands. If no sub commands exist this doesn't have to be
     * overwritten
     *
     * @return a collection containing all direct sub commands
     */
    @Nonnull
    public Set<AbstractCommand> subCommands() {
        return this.getConfiguration().getSubCommands();
    }


    @Nonnull
    public String name() {
        return this.getConfiguration().getName();
    }

    public List<String> alias() {
        return Stream.concat(Stream.of(this.getConfiguration().getName()),
            Stream.of(this.getConfiguration().getAlias())).map(String::toLowerCase).collect(Collectors.toList());
    }

    /**
     * The main functionality of the command
     *
     * @param event
     *     the command event containing all important parameters
     */
    public abstract void run(CommandEvent event);

    /**
     * Returns the {@link CommandPerm} needed to execute the command
     *
     * @return the CommandPerm needed to execute the command
     */
    @Nonnull
    public CommandPerm commandPerm() {
        return this.configuration.getPerm();
    }

    /**
     * The command's parameters. If this command does not take any parameters this shouldn't be overwritten
     *
     * @return the command's parameters
     */
    @Nonnull
    public String usage() {
        return this.configuration.getUsage();
    }

    /**
     * Returns a short description of the command
     *
     * @return the commands description
     */
    @Nonnull
    public String description() {
        return this.configuration.getDescription();
    }

    /**
     * Returns a detailed help message of the command if needed
     *
     * @return the detailed help message
     */
    @Nonnull
    public String help() {
        return this.configuration.getHelp();
    }

    protected String lastArg(int expectedIndex, CommandEvent event) {
        String s = event.getArgString();
        List<String> args = event.getArgs();

        for (int i = 0; i < expectedIndex; i++) {
            String arg = args.get(i);
            if (s.startsWith("\"")) s = StringUtils.replaceOnce(s, "\"" + arg + "\"", "").strip();
            else s = StringUtils.replaceOnce(s, arg, "").strip();
        }
        return s;
    }

}
