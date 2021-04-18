package me.zoemartin.rubie.modules.commandProcessing;

import me.zoemartin.rubie.core.CommandEvent;
import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.exceptions.*;
import me.zoemartin.rubie.core.interfaces.*;
import me.zoemartin.rubie.core.managers.CommandManager;
import me.zoemartin.rubie.core.util.Check;
import me.zoemartin.rubie.core.util.Help;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandHandler implements CommandProcessor {
    private final Logger log = LoggerFactory.getLogger(CommandHandler.class);

    @Override
    public void process(MessageReceivedEvent event, String input) {
        User user = event.getAuthor();
        MessageChannel channel = event.getChannel();

        Matcher cMatcher = Pattern.compile("(\\w*)").matcher(input);
        LinkedList<AbstractCommand> commands = new LinkedList<>();
        LinkedList<String> invoked = new LinkedList<>();
        int lastIndex = 0;
        while (cMatcher.find()) {
            String s = cMatcher.group().toLowerCase();
            if (commands.isEmpty()) {
                AbstractCommand cmd = CommandManager.getCommands().stream()
                                          .filter(c -> c.alias().contains(s))
                                          .findFirst().orElse(null);
                if (cmd == null) return;
                commands.add(cmd);
                invoked.add(s);
                lastIndex = cMatcher.end();
            } else if (commands.getLast() != null) {
                AbstractCommand cmd = commands.getLast().subCommands().stream()
                                          .filter(sc -> sc.alias().contains(s))
                                          .findFirst().orElse(null);
                if (cmd != null) {
                    commands.add(cmd);
                    invoked.add(s);
                    lastIndex = cMatcher.end();
                }
            }
        }

        LinkedList<String> arguments = new LinkedList<>();
        String argString = input.substring(lastIndex).strip();

        Matcher argMatcher = Pattern.compile("((?:[^\"]\\S*)|(?:\"(?:.+?[^\\\\])\"))\\s*").matcher(argString);
        while (argMatcher.find()) {
            arguments.add(argMatcher.group().strip().replaceAll("(?:\"(.*[^\\\\])\")", "$1"));
        }

        AbstractCommand command = commands.getLast();
        try {
            if (event.isFromGuild()) {
                GuildCommandEvent e = new GuildCommandEvent(event.getMessage(), arguments, invoked, argString);
                if (command instanceof GuildCommand) {
                    Check.check(((GuildCommand) command).checkGuildPerms(e),
                        () -> new CommandPermissionException(
                            "Error, you are missing the necessary guild permissions for this command!"));
                    Check.check(((GuildCommand) command).checkChannelPerms(e),
                        () -> new CommandPermissionException(
                            "Error, you are missing the necessary permissions in this channel for this command!"));
                    Check.check(((GuildCommand) command).checkNecessaryPerms(e),
                        () -> new CommandPermissionException(
                            "Error, I seem to be missing the necessary permissions to run this command!"));
                }
                Check.check(PermissionHandler.checkUserPerms(command, e),
                    () -> new ConsoleError(
                        "[Permission Error] Member 'U:%s(%s)' doesn't have the required permission rank for Command '%s' on '%s'",
                        e.getUser().getAsTag(), e.getUser().getId(), command.name(), e.getGuild()));
                command.run(e);
            } else {
                CommandEvent e = new CommandEvent(event.getMessage(), arguments, invoked, argString);
                Check.check(PermissionHandler.checkUserPerms(command, e),
                    () -> new ReplyError("It looks like you dont have permissions for this command!"));
                command.run(e);
            }
        } catch (CommandArgumentException e) {
            if (event.isFromGuild())
                Help.getHelper().send(new GuildCommandEvent(event.getMessage(), invoked, List.of(invoked.getFirst()), argString));
            else channel.sendMessageFormat("Sorry, I had an error trying to understand that command.").queue();
        } catch (ReplyError e) {
            channel.sendMessage(e.getMessage()).queue(message -> message.delete().queueAfter(10, TimeUnit.SECONDS));
        } /*catch (ConsoleError e) {
            throw e;
            //throw new ConsoleError(String.format("[Command Error] %s: %s", command.getClass().getName(), e.getMessage()));
        } catch (Exception e) {
            LoggedError error = new LoggedError(event.getGuild().getId(), event.getChannel().getId(), event.getAuthor().getId(),
                event.getMessageId(), event.getMessage().getContentRaw(), e.getMessage(), e.getStackTrace(), System.currentTimeMillis());

            DatabaseUtil.saveObject(error);

            channel.sendMessageFormat("> Error Code:  `%s`", error.getUuid())
                .embed(new EmbedBuilder()
                           .setColor(Color.RED)
                           .setTitle("An internal error has occurred")
                           .setDescription("For support send this code to the Developer " +
                                               "along with a description of what you were doing.")
                           .setFooter(error.getUuid().toString())
                           .setTimestamp(Instant.now())
                           .build()).queue();
            throw e;
        }*/
        if (event.isFromGuild())
            log.info("{}/({}) used {} in {}-{}({})", user.getAsTag(), user.getId(), command.getClass().getName(),
                event.getGuild(), event.getChannel().getName(), event.getChannel().getId());
        else
            log.info("{}/({}) used {} in DMs {}", user.getAsTag(), user.getId(), command.getClass().getName(),
                event.getChannel().getId());
    }

    private boolean isGuildCommand(AbstractCommand c) {
        return Arrays.asList(c.getClass().getClasses()).contains(GuildCommand.class);
    }
}
