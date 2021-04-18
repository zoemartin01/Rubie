package me.zoemartin.rubie.modules.debug;

import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.annotations.Command;
import me.zoemartin.rubie.core.annotations.CommandOptions;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.Parser;
import me.zoemartin.rubie.core.util.*;
import me.zoemartin.rubie.modules.pagedEmbeds.PageListener;
import me.zoemartin.rubie.modules.pagedEmbeds.PagedEmbed;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import org.apache.commons.cli.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Command
@CommandOptions(
    name = "members",
    description = "Shows a filtered list of members",
    perm = CommandPerm.BOT_MANAGER,
    usage = "--help"
)
public class Members extends GuildCommand {
    @Override
    public void run(GuildCommandEvent event) {
        CommandLineParser parser = new DefaultParser();
        CommandLine command;
        Options options = options();

        try {
            command = parser.parse(options, event.getArgs().toArray(String[]::new));
        } catch (ParseException e) {
            event.getChannel().sendMessage(helpEmbed(event)).queue();
            return;
        }

        if (command.hasOption("H")) {
            event.getChannel().sendMessage(helpEmbed(event)).queue();
            return;
        }

        var guild = event.getGuild();
        var members = guild.loadMembers().get();

        if (command.hasOption("h")) {
            members = members.stream().filter(member -> !member.getUser().isBot()).collect(Collectors.toList());
        }

        if (command.hasOption("b")) {
            members = members.stream().filter(member -> member.getUser().isBot()).collect(Collectors.toList());
        }

        if (command.hasOption("r")) {
            var roles = Arrays.stream(command.getOptionValues("r")).map(
                s -> {
                    var r = Parser.Role.getRole(guild, s);
                    Check.entityReferenceNotNull(r, Role.class, s);
                    return r;
                }
            ).collect(Collectors.toList());

            members = members.stream()
                          .filter(member -> member.getRoles().stream().anyMatch(roles::contains))
                          .collect(Collectors.toList());
        }

        if (command.hasOption("ar")) {
            var roles = Arrays.stream(command.getOptionValues("ar")).map(
                s -> {
                    var r = Parser.Role.getRole(guild, s);
                    Check.entityReferenceNotNull(r, Role.class, s);
                    return r;
                }
            ).collect(Collectors.toList());

            members = members.stream()
                          .filter(member -> member.getRoles().containsAll(roles))
                          .collect(Collectors.toList());
        }

        if (command.hasOption("nr")) {
            var roles = Arrays.stream(command.getOptionValues("nr")).map(
                s -> {
                    var r = Parser.Role.getRole(guild, s);
                    Check.entityReferenceNotNull(r, Role.class, s);
                    return r;
                }
            ).collect(Collectors.toList());

            members = members.stream()
                          .filter(member -> member.getRoles().stream().noneMatch(roles::contains))
                          .collect(Collectors.toList());
        }

        if (command.hasOption("nc")) {
            var name = command.getOptionValue("nc").toLowerCase();

            members = members.stream()
                          .filter(member -> member.getUser().getAsTag().toLowerCase().contains(name)
                                                || member.getEffectiveName().toLowerCase().contains(name))
                          .collect(Collectors.toList());
        }

        if (command.hasOption("NR")) {
            var regex = command.getOptionValue("NR");

            members = members.stream()
                          .filter(member -> member.getUser().getAsTag().toLowerCase().matches(regex))
                          .collect(Collectors.toList());
        }

        List<String> output;
        var mem = members;

        if (command.hasOption("f")) {
            var format = command.getOptionValue("f");

            output = members.stream().map(member -> {
                var out = format;
                out = out.replaceAll("%id", member.getId());
                out = out.replaceAll("%ind", String.valueOf(mem.indexOf(member) + 1));
                out = out.replaceAll("%cd", String.valueOf(member.getTimeCreated()));
                out = out.replaceAll("%jd", String.valueOf(member.getTimeJoined()));
                out = out.replaceAll("%tag", member.getUser().getAsTag());
                out = out.replaceAll("%nick", member.getEffectiveName());
                out = out.replaceAll("%me", member.getAsMention());
                return out;
            }).collect(Collectors.toList());
        } else {
            output = members.stream().map(member -> String.format(
                "%d. %s (%s)", mem.indexOf(member) + 1, member.getUser().getAsTag(), member.getId()
            )).collect(Collectors.toList());
        }

        if (command.hasOption("out")) {
            event.getChannel().sendFile(String.join("\n", output).getBytes(), "members.txt").queue();
        } else if (command.hasOption("c")) {
            event.reply("Member Count", "%d members", mem.size()).queue();
        } else {
            var template = new EmbedBuilder()
                               .setTitle("Members")
                               .setColor(event.getGuild().getSelfMember().getColor()).build();
            var paged = new PagedEmbed(
                EmbedUtil.pagedDescription(template, output.stream().map(s -> s + "\n").collect(Collectors.toList())),
                event);

            PageListener.add(paged);
        }
    }

    private MessageEmbed helpEmbed(GuildCommandEvent event) {
        var eb = new EmbedBuilder();
        var form = new HelpFormatter();
        var opt = options();
        StringWriter usage = new StringWriter();
        PrintWriter usagePW = new PrintWriter(usage);

        form.printUsage(usagePW, Integer.MAX_VALUE, name(), opt);

        eb.setTitle(String.format("`%s`", name().toUpperCase()));
        eb.setColor(0xdf136c);
        eb.addField("Description:", description(), false);
        eb.addField("Usage:", String.format("```%s```", usage.toString().replaceFirst("usage: ", "")), false);
        eb.addField("Options:", opt.getOptions().stream().map(o -> {
            if (o.hasArgs())
                return String.format("`-%s, --%s <args...>` - %s", o.getOpt(), o.getLongOpt(), o.getDescription());
            if (o.hasArg())
                return String.format("`-%s, --%s <arg>` - %s", o.getOpt(), o.getLongOpt(), o.getDescription());
            return String.format("`-%s, --%s` - %s", o.getOpt(), o.getLongOpt(), o.getDescription());
        }).collect(Collectors.joining("\n")), false);

        var perm = commandPerm();
        if (perm != CommandPerm.EVERYONE)
            eb.addField("Permission Level:", String.format("`[%d] %s`", perm.raw(), perm.toString()), false);

        usage.flush();


        return eb.build();
    }

    private Options options() {
        Options options = new Options();

        options.addOption(
            Option.builder("r")
                .longOpt("roles")
                .desc("Shows members with at least one of these roles")
                .hasArgs()
                .required(false)
                .build()
        );

        options.addOption(
            Option.builder("ar")
                .longOpt("all-roles")
                .desc("Shows members with all of these roles")
                .hasArgs()
                .required(false)
                .build()
        );

        options.addOption(
            Option.builder("nr")
                .longOpt("not-roles")
                .hasArgs()
                .desc("Shows members who do not have these roles")
                .required(false)
                .build()
        );

        options.addOption(
            Option.builder("f")
                .longOpt("format")
                .desc("Specifies the output format. \n" +
                          "`%id` - user id\n" +
                          "`%ind` - list index\n" +
                          "`%cd` - creation date\n" +
                          "`%jd` - join date\n" +
                          "`%me` - user mention\n" +
                          "`%tag` - user tag\n" +
                          "`%nick` - user nickname\n")
                .hasArg(true)
                .required(false)
                .build()
        );

        options.addOption(
            Option.builder("nc")
                .longOpt("name-contains")
                .desc("Shows members who's name contains the following")
                .hasArg()
                .required(false)
                .build()
        );

        options.addOption(
            Option.builder("NR")
                .longOpt("name-regex")
                .hasArg()
                .desc("Shows members who's name matches the following")
                .required(false)
                .build()
        );


        var out = Option.builder("out")
                      .longOpt("file-output")
                      .desc("The result will be provided as a text file")
                      .hasArg(false)
                      .required(false)
                      .build();

        var count = Option.builder("c")
                        .longOpt("count")
                        .desc("The result will be a filtered member count")
                        .hasArg(false)
                        .required(false)
                        .build();

        options.addOptionGroup(new OptionGroup().addOption(out).addOption(count));


        options.addOption(
            Option.builder("H")
                .longOpt("help")
                .desc("Command Help")
                .hasArg(false)
                .required(false)
                .build()
        );

        var human = Option.builder("h")
                        .longOpt("humans")
                        .desc("Shows humans")
                        .hasArg(false)
                        .required(false)
                        .build();

        var bot = Option.builder("b")
                      .longOpt("bots")
                      .desc("Shows bots")
                      .hasArg(false)
                      .required(false)
                      .build();

        options.addOptionGroup(new OptionGroup().addOption(human).addOption(bot));

        return options;
    }
}
