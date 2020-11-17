package me.zoemartin.rubie.modules.commandProcessing;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.*;
import me.zoemartin.rubie.core.exceptions.*;
import me.zoemartin.rubie.core.interfaces.*;
import me.zoemartin.rubie.core.managers.CommandManager;
import me.zoemartin.rubie.core.util.*;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandHandler implements CommandProcessor {
    @Override
    public void process(MessageReceivedEvent event, String input) {
        User user = event.getAuthor();
        MessageChannel channel = event.getChannel();

        List<String> inputs = new ArrayList<>();
        Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(input);
        while (m.find())
            inputs.add(m.group(1).replace("\"", ""));

        LinkedList<AbstractCommand> commands = new LinkedList<>();
        LinkedList<String> invoked = new LinkedList<>();
        inputs.forEach(s -> {
            if (commands.isEmpty()) {
                AbstractCommand cmd = CommandManager.getCommands().stream()
                                  .filter(c -> s.matches(c.regex().toLowerCase()))
                                  .findFirst().orElse(null);
                commands.add(cmd);
                if (cmd != null) invoked.add(s);
            } else if (commands.getLast() != null) {
                AbstractCommand cmd = commands.getLast().subCommands().stream()
                                  .filter(sc -> s.matches(sc.regex().toLowerCase()))
                                  .findFirst().orElse(null);
                if (cmd != null) {
                    commands.add(cmd);
                    invoked.add(s);
                }
            }

        });

        if (commands.isEmpty() || commands.getLast() == null) return;

        int commandLevel = commands.size();
        AbstractCommand command = commands.getLast();

        if (event.isFromGuild()) {
            Guild guild = event.getGuild();
            Member member = guild.getMember(user);
            Check.notNull(member, () -> new ConsoleError("member is null"));

            if (command.commandPerm() != CommandPerm.EVERYONE) {
                Check.check(PermissionHandler.getHighestFromUser(guild, member).raw() >= command.commandPerm().raw(),
                    () -> new ConsoleError("Member '%s' doesn't have the required permission rank for Command '%s'",
                        member.getId(), command.name()));
            }

            Check.check((command.required().size() == 1 && command.required().contains(Permission.UNKNOWN))
                            || member.hasPermission(Permission.ADMINISTRATOR)
                            || command.required().stream().allMatch(member::hasPermission),
                () -> new ConsoleError("Member '%s' doesn't have the required permission for Command '%s'",
                    member.getId(), command.name()));
        } else {
            Check.check(command.commandPerm() == CommandPerm.EVERYONE
                            || command.commandPerm() == CommandPerm.BOT_USER
                            || user.getId().equals(Bot.getOWNER()),
                () -> new ReplyError("It looks like you dont have permissions for this command!"));
        }

        List<String> arguments;

        arguments = inputs.subList(commandLevel, inputs.size());

        GuildCommandEvent ce = null;
        try {
            //command.run(user, channel, Collections.unmodifiableList(arguments), event.getMessage(), inputs.get(commands.size() - 1));
            if (event.isFromGuild()) {
                GuildCommandEvent e = new GuildCommandEvent(event.getMessage(), arguments, invoked);
                command.run(e);
            } else {
                CommandEvent e = new CommandEvent(event.getMessage(), arguments, invoked);
                command.run(e);
            }
        } catch (CommandArgumentException e) {
            if(event.isFromGuild()) Help.getHelper().send(new GuildCommandEvent(event.getMessage(), invoked, List.of(invoked.getFirst())));
            else channel.sendMessageFormat("Sorry, I had an error trying to understand that command.").queue();
        } catch (ReplyError e) {
            channel.sendMessage(e.getMessage()).queue(message -> message.delete().queueAfter(10, TimeUnit.SECONDS));
        } catch (ConsoleError e) {
            throw e;
            //throw new ConsoleError(String.format("[Command Error] %s: %s", command.getClass().getName(), e.getMessage()));
        } catch (Exception e) {
            /*LoggedError error = new LoggedError(event.getGuild().getId(), event.getChannel().getId(), event.getAuthor().getId(),
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
                           .build()).queue();*/
            throw e;
        }

        System.out.printf("[Command used] %s used command %s in %s\n", user.getId(), command.getClass().getCanonicalName(),
            event.isFromGuild() ? event.getGuild().getId() : event.getChannel().getId());
    }

    private boolean isGuildCommand(AbstractCommand c) {
        return Arrays.asList(c.getClass().getClasses()).contains(GuildCommand.class);
    }
}
