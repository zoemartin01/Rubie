package me.zoemartin.rubie.core.interfaces;

import me.zoemartin.rubie.core.CommandEvent;
import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.annotations.*;
import me.zoemartin.rubie.core.util.MessageUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.reflections8.Reflections;
import org.reflections8.scanners.SubTypesScanner;
import org.reflections8.scanners.TypeAnnotationsScanner;
import org.reflections8.util.ClasspathHelper;
import org.reflections8.util.ConfigurationBuilder;

import javax.annotation.Nonnull;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public abstract class AbstractCommand {
    private Set<AbstractCommand> subCommands = null;

    /**
     * Returns  collection containing all direct sub commands. If no sub commands exist this doesn't have to be
     * overwritten
     *
     * @return a collection containing all direct sub commands
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    public Set<AbstractCommand> subCommands() {
        if (subCommands != null) return subCommands;

        Reflections reflections = new Reflections(new ConfigurationBuilder()
                                                      .setUrls(ClasspathHelper.forPackage("me.zoemartin.rubie.modules"))
                                                      .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner())
                                                      .setExecutorService(Executors.newFixedThreadPool(4)));

        Set<Class<?>> commandReflect = reflections.getTypesAnnotatedWith(SubCommand.class);



        subCommands = commandReflect.stream()
                          .filter(c -> c.getAnnotationsByType(SubCommand.class)[0].value() == this.getClass())
                          .filter(c -> c.getAnnotationsByType(Disabled.class).length == 0)
                          .map(c -> {
                              AbstractCommand sub = null;
                              try {
                                  Constructor<? extends AbstractCommand> constructor = (Constructor<? extends AbstractCommand>)
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
        return subCommands;
    }

    /**
     * Returns the name of the command. This is used in {@link #usage()} and as the invoking string if {@link #regex()}
     * is not set.
     *
     * @return the name of the command
     */
    @Nonnull
    public String name() {
        CommandOptions[] options = this.getClass().getAnnotationsByType(CommandOptions.class);

        if (options == null || options.length == 0)
            throw new IllegalStateException("Command Class missing CommandOptions Annotation");

        return options[0].name();
    }

    /**
     * Returns a regex with which the command may be found. Aliases can be set this way. If no aliases should be set
     * this doesn't have to be overwritten
     *
     * @return the commands regex
     */
    @Nonnull
    @Deprecated
    public String regex() {
        CommandOptions[] options = this.getClass().getAnnotationsByType(CommandOptions.class);

        if (options == null || options.length == 0)
            throw new IllegalStateException("Command Class missing CommandOptions Annotation");

        if (options[0].alias().length == 0) return options[0].name();

        return options[0].name() + "|" + String.join("|", options[0].alias());
    }

    /**
     * The main functionality of the command
     *
     * @param user
     *     the user executing the command
     * @param channel
     *     the channel the command is executed in
     * @param args
     *     the arguments that are passed
     * @param original
     *     the original message
     * @param invoked
     *     the string that invoked the command
     */
    @Deprecated
    public void run(User user, MessageChannel channel, List<String> args, Message original, String invoked) {
        run(new CommandEvent(user, channel, original.getContentRaw(), original.getJDA(), args, List.of(invoked)));
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
        CommandOptions[] options = this.getClass().getAnnotationsByType(CommandOptions.class);

        if (options == null || options.length == 0)
            throw new IllegalStateException(String.format("Command '%s' missing CommandOptions Annotation", this.getClass().getName()));

        return options[0].perm();
    }

    /**
     * Returns a collection of discord {@link Permission}s a user needs to execute the command. If no special
     * permissions are required, this doesn't have to be overwritten
     *
     * @return a collection of discord permissions a user needs to execute the command
     */
    @Deprecated
    @Nonnull
    public Collection<Permission> required() {
        Checks.Permissions.Guild[] options = this.getClass().getAnnotationsByType(Checks.Permissions.Guild.class);

        if (options == null || options.length == 0)
            return Collections.singleton(Permission.UNKNOWN);

        return Arrays.asList(options[0].value());
    }

    /**
     * The command's parameters. If this command does not take any parameters this shouldn't be overwritten
     *
     * @return the command's parameters
     */
    @Deprecated
    @Nonnull
    public String usage() {
        CommandOptions[] options = this.getClass().getAnnotationsByType(CommandOptions.class);

        if (options == null || options.length == 0)
            throw new IllegalStateException("Command Class missing CommandOptions Annotation");

        return options[0].usage();
    }

    /**
     * Returns a short description of the command
     *
     * @return the commands description
     */
    @Deprecated
    @Nonnull
    public String description() {
        CommandOptions[] options = this.getClass().getAnnotationsByType(CommandOptions.class);

        if (options == null || options.length == 0)
            throw new IllegalStateException("Command Class missing CommandOptions Annotation");

        return options[0].description();
    }

    /**
     * Returns a detailed help message of the command if needed
     *
     * @return the detailed help message
     */
    @Nonnull
    public String detailedHelp() {
        return "";
    }

    protected String lastArg(int expectedIndex, CommandEvent event) {
        if (event.getArgs().isEmpty()) return "";
        if (event.getArgs().size() == expectedIndex + 1) return event.getArgs().get(expectedIndex);

        String orig = event.getContent();
        for (String s : event.getInvoked()) {
            orig = orig.replaceFirst(s, "");
        }

        return MessageUtils.getArgsFrom(orig, event.getArgs().get(expectedIndex));
    }
}
