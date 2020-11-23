package me.zoemartin.rubie.core.managers;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.exceptions.ConsoleError;
import me.zoemartin.rubie.core.exceptions.ReplyError;
import me.zoemartin.rubie.core.interfaces.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.*;

public class CommandManager {
    private CommandManager() {
        throw new IllegalAccessError();
    }

    private static final Collection<AbstractCommand> registered = new HashSet<>();

    private static CommandProcessor processor;
    private static CommandLogger logger = null;

    public static void register(AbstractCommand c) {
        registered.add(c);
    }

    public static void setCommandProcessor(CommandProcessor cp) {
        processor = cp;
    }

    public static void setCommandLogger(CommandLogger cl) {
        logger = cl;
    }


    public static void process(MessageReceivedEvent event, String input) {
        new Thread(() -> {
            try {
                processor.process(event, input);
            } catch (ConsoleError e) {
                if (event.getAuthor().getId().equals(Bot.getOWNER())) {
                    event.getChannel().sendMessageFormat("Error: `%s`", e.getMessage()).queue();
                    throw e;
                }
                else System.err.println(e.getMessage());
            } catch (ReplyError e) {
                if (!event.isFromGuild()) event.getChannel().sendMessage(e.getMessage()).queue();
                else throw e;
            }
        }).start();
    }

    public static Collection<AbstractCommand> getCommands() {
        return Collections.unmodifiableCollection(registered);
    }

    public static CommandLogger getCommandLogger() {
        return logger;
    }
}
