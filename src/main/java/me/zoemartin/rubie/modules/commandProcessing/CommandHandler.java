package me.zoemartin.rubie.modules.commandProcessing;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.exceptions.*;
import me.zoemartin.rubie.core.interfaces.Command;
import me.zoemartin.rubie.core.interfaces.CommandProcessor;
import me.zoemartin.rubie.core.managers.CommandManager;
import me.zoemartin.rubie.core.util.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.awt.*;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandHandler implements CommandProcessor {
    @Override
    public void process(GuildMessageReceivedEvent event, String input) {
        User user = event.getAuthor();
        MessageChannel channel = event.getChannel();

        List<String> inputs = new ArrayList<>();
        Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(input);
        while (m.find())
            inputs.add(m.group(1).replace("\"", ""));

        LinkedList<Command> commands = new LinkedList<>();
        inputs.forEach(s -> {
            if (commands.isEmpty()) commands.add(CommandManager.getCommands().stream()
                                                     .filter(c -> s.matches(c.regex().toLowerCase()))
                                                     .findFirst().orElse(null));
            else if (commands.getLast() != null) commands.getLast().subCommands().stream()
                                                     .filter(sc -> s.matches(sc.regex().toLowerCase()))
                                                     .findFirst().ifPresent(commands::add);

        });

        if (commands.isEmpty() || commands.getLast() == null) return;

        int commandLevel = commands.size();
        Command command = commands.getLast();

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

        List<String> arguments;

        arguments = inputs.subList(commandLevel, inputs.size());


        try {
            command.run(user, channel, Collections.unmodifiableList(arguments), event.getMessage(), inputs.get(commands.size() - 1));
        } catch (CommandArgumentException e) {
            Help.getHelper().send(user, channel,
                commands.stream().map(Command::name).collect(Collectors.toList()),
                event.getMessage(), commands.getFirst().name());
        } catch (ReplyError e) {
            channel.sendMessage(e.getMessage()).queue(message -> message.delete().queueAfter(10, TimeUnit.SECONDS));
        } catch (ConsoleError e) {
            throw new ConsoleError(String.format("[Command Error] %s: %s", command.getClass().getName(), e.getMessage()));
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
        }

        System.out.printf("[Command used] %s used command %s in %s\n", user.getId(), command.getClass().getCanonicalName(),
            event.getGuild().getId());
    }
}
