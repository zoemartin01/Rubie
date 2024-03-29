package me.zoemartin.rubie.modules.baseCommands;

import me.zoemartin.rubie.core.*;
import me.zoemartin.rubie.core.annotations.*;
import me.zoemartin.rubie.core.exceptions.ReplyError;
import me.zoemartin.rubie.core.interfaces.AbstractCommand;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.managers.CommandManager;
import me.zoemartin.rubie.core.util.Check;
import me.zoemartin.rubie.core.util.EmbedUtil;
import me.zoemartin.rubie.modules.commandProcessing.PermissionHandler;
import me.zoemartin.rubie.modules.pagedEmbeds.PageListener;
import me.zoemartin.rubie.modules.pagedEmbeds.PagedEmbed;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import java.util.*;
import java.util.stream.Collectors;

@Command
@CommandOptions(
    name = "help",
    description = "Shows commands help or a list of all available commands",
    usage = "[command]"
)
@Arguments()
@Arguments(Command.class)
public class Help extends GuildCommand {
    @Override
    public void run(GuildCommandEvent event) {
        if (event.getArgs().size() != 0) commandHelp(event);
        else {
            Guild guild = event.getGuild();
            Member member = event.getMember();

            PagedEmbed p = new PagedEmbed(EmbedUtil.pagedDescription(new EmbedBuilder()
                                                                         .setTitle("Help").setColor(0xdf136c).build(),
                CommandManager.getCommands().stream()
                    .filter(command -> !command.getConfiguration().isHidden())
                    .filter(
                        command -> PermissionHandler.getHighestFromUser(guild, member).raw() >= command.commandPerm().raw())
                    .sorted(Comparator.comparing(AbstractCommand::name))
                    .map(command -> String.format("`%s` | %s\n\n", command.name(), command.description()))
                    .collect(Collectors.toList())), event);

            PageListener.add(p);
        }
    }

    private static void commandHelp(CommandEvent event) {
        LinkedList<AbstractCommand> hierarchy = new LinkedList<>();

        event.getArgs().forEach(s -> {
            if (hierarchy.isEmpty()) {
                AbstractCommand cmd = CommandManager.getCommands().stream()
                                          .filter(c -> c.alias().contains(s.toLowerCase()))
                                          .findFirst().orElse(null);
                hierarchy.add(cmd);
            } else if (hierarchy.getLast() != null) {
                hierarchy.getLast().subCommands().stream()
                    .filter(sc -> sc.alias().contains(s.toLowerCase()))
                    .findFirst().ifPresent(hierarchy::add);
            }
        });

        Check.check(!hierarchy.isEmpty() && hierarchy.getLast() != null, () -> new ReplyError("No such command!"));

        String name = hierarchy.stream().map(AbstractCommand::name)
                          .collect(Collectors.joining(" "));
        AbstractCommand cmd = hierarchy.getLast();
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("`" + name.toUpperCase() + "`")
            .setColor(0xdf136c);
        eb.addField("Description:", cmd.description(), false);
        //if (!cmd.help().isEmpty()) eb.addField("Detailed Help:", cmd.help(), false);
        eb.addField("Usage: ", cmd.usage().isEmpty() ?
                                   String.format("`%s`", name) : String.format("`%s %s`", name, cmd.usage()),
            false);


        if (!cmd.help().isBlank()) eb.setDescription(cmd.help());

        CommandPerm perm = cmd.commandPerm();
        if (perm != CommandPerm.EVERYONE)
            eb.addField("Permission Level:", String.format("`[%d] %s`", perm.raw(), perm.toString()), false);

        eb.addField("Aliases:", String.format("`%s`",
            cmd.alias().size() > 1 ?
                cmd.alias().stream().filter(s -> !s.equalsIgnoreCase(cmd.name()))
                    .collect(Collectors.joining(", "))
                : "n/a"), false);

        StringBuilder sub = new StringBuilder();
        Iterator<AbstractCommand> iterator = cmd.subCommands().iterator();

        while (iterator.hasNext()) {
            AbstractCommand c = iterator.next();

            if (iterator.hasNext())
                sub.append("`├─ ")
                    .append(c.commandPerm() != CommandPerm.EVERYONE ? "[" + c.commandPerm().raw() + "] " : "")
                    .append(c.name()).append("`\n");
            else sub.append("`└─ ")
                     .append(c.commandPerm() != CommandPerm.EVERYONE ? "[" + c.commandPerm().raw() + "] " : "")
                     .append(c.name()).append("`");
        }

        if (sub.length() > 0)
            eb.addField("Subcommand(s)", sub.toString(), false);

        event.getChannel().sendMessageEmbeds(eb.build()).queue();
    }

    public static void helper() {
        me.zoemartin.rubie.core.util.Help.setHelper(Help::commandHelp);
    }
}
