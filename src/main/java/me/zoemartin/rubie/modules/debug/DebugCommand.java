package me.zoemartin.rubie.modules.debug;

import me.zoemartin.rubie.core.*;
import me.zoemartin.rubie.core.annotations.Command;
import me.zoemartin.rubie.core.annotations.CommandOptions;
import me.zoemartin.rubie.core.exceptions.ReplyError;
import me.zoemartin.rubie.core.interfaces.AbstractCommand;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.managers.CommandManager;
import me.zoemartin.rubie.core.util.Check;
import me.zoemartin.rubie.core.util.Parser;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.cli.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Command
@CommandOptions(
    name = "debug",
    description = "Debug a command with parameters",
    perm = CommandPerm.OWNER

)
public class DebugCommand extends AbstractCommand {
    @Override
    public void run(CommandEvent event) {
        CommandLineParser parser = new DefaultParser();
        CommandLine command;
        Options options = options();


        try {
            command = parser.parse(options, event.getArgs().toArray(String[]::new));
        } catch (ParseException e) {
            argumentError(options);
            return;
        }

        LinkedList<AbstractCommand> commands = new LinkedList<>();
        LinkedList<String> invoked = new LinkedList<>();
        Arrays.stream(command.getOptionValues("c")).forEach(s -> {
            if (commands.isEmpty()) {
                AbstractCommand cmd = CommandManager.getCommands().stream()
                                          .filter(c -> c.alias().contains(s.toLowerCase()))
                                          .findFirst().orElseThrow(() -> new ReplyError("No valid command"));
                commands.add(cmd);
                invoked.add(s);
            } else {
                AbstractCommand cmd = commands.getLast().subCommands().stream()
                                          .filter(sc -> sc.alias().contains(s.toLowerCase()))
                                          .findFirst().orElse(null);
                if (cmd != null) {
                    commands.add(cmd);
                    invoked.add(s);
                }
            }
        });

        String guildId;

        if (command.hasOption("g")) guildId = command.getOptionValue("g");
        else if (event instanceof GuildCommandEvent) guildId = ((GuildCommandEvent) event).getGuild().getId();
        else {
            argumentError(options);
            return;
        }

        Check.check(guildId.matches("\\d{17,19}"), () -> new ReplyError("Malformed guild id: `%s`", guildId));
        Guild guild = event.getJDA().getGuildById(guildId);
        Check.entityReferenceNotNull(guild, Guild.class, guildId);

        Member member = command.hasOption("u") ? guild.getMemberById(command.getOptionValue("u")) : guild.getSelfMember();
        Check.entityNotNull(member, Member.class);

        String content;
        List<String> args;
        if (command.hasOption("a")) {
            args = Arrays.stream(command.getOptionValues("a")).collect(Collectors.toList());
            content = Stream.concat(Arrays.stream(command.getOptionValues("c")), Arrays.stream(command.getOptionValues("a")))
                          .collect(Collectors.joining(" "));
        } else {
            args = Collections.emptyList();
            content = String.join(" ", command.getOptionValues("c"));
        }

        Constructor<GuildCommandEvent> debugConstructor;
        try {
            debugConstructor = GuildCommandEvent.class.getDeclaredConstructor(
                User.class, MessageChannel.class, String.class,
                JDA.class, List.class, List.class, Message.class,
                Member.class, Guild.class, TextChannel.class, String.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return;
        }

        debugConstructor.setAccessible(true);

        GuildCommandEvent guildEvent;
        if (command.hasOption("tc")) {
            String channelRef = command.getOptionValue("tc");
            TextChannel channel = Parser.Channel.getTextChannel(guild, channelRef);
            Check.entityReferenceNotNull(channel, TextChannel.class, channelRef);
            guildEvent = new GuildCommandEvent(member, channel, content, event.getJDA(), args, invoked, String.join(" ", args));
        } else {
            try {
                guildEvent = debugConstructor.newInstance(member.getUser(), event.getChannel(), content, event.getJDA(),
                    args, invoked, null, member, guild, null, String.join(" ", args));
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
                debugConstructor.setAccessible(false);
                return;
            }
        }


        try {
            commands.getLast().run(guildEvent);
        } catch (ReplyError e) {
            try {
                new GuildCommand() {
                    @Override
                    public void run(GuildCommandEvent event) {
                    }
                }.sendHelp(debugConstructor.newInstance(member.getUser(), event.getChannel(), content, event.getJDA(),
                    invoked, List.of(invoked.getFirst()), null, member, guild, null, String.join(" ", args)));
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException instantiationException) {
                instantiationException.printStackTrace();
                debugConstructor.setAccessible(false);
            }
        }

    }

    private void argumentError(Options options) throws ReplyError {
        HelpFormatter formatter = new HelpFormatter();
        StringWriter out = new StringWriter();
        PrintWriter pw = new PrintWriter(out);

        formatter.printHelp(pw, 80, "run", null, options,
            formatter.getLeftPadding(), formatter.getDescPadding(), null, true);
        pw.flush();

        throw new ReplyError(out.toString());
    }

    private Options options() {
        Options options = new Options();

        options.addOption(
            Option.builder("g")
                .longOpt("guild")
                .hasArg().numberOfArgs(1)
                .required(false)
                .build()
        );

        options.addOption(
            Option.builder("tc")
                .longOpt("channel")
                .hasArg().numberOfArgs(1)
                .required(false)
                .build()
        );

        options.addOption(
            Option.builder("u")
                .longOpt("user")
                .hasArg().numberOfArgs(1)
                .required(false)
                .build()
        );


        options.addOption(
            Option.builder("c")
                .longOpt("command")
                .hasArgs()
                .required()
                .build()
        );

        options.addOption(
            Option.builder("a")
                .longOpt("args")
                .hasArgs()
                .required(false)
                .build()
        );

        return options;
    }
}
