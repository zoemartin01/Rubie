package me.zoemartin.rubie.modules.trigger;

import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.annotations.*;
import me.zoemartin.rubie.core.exceptions.*;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.Check;
import me.zoemartin.rubie.core.util.EmbedUtil;
import me.zoemartin.rubie.modules.pagedEmbeds.PageListener;
import me.zoemartin.rubie.modules.pagedEmbeds.PagedEmbed;
import net.dv8tion.jda.api.EmbedBuilder;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

@Command
@CommandOptions(
    name = "trigger",
    description = "Trigger Management",
    usage = "<regex> <output...>",
    alias = {"ar", "autoresponse"},
    perm = CommandPerm.BOT_MANAGER
)
public class TriggerCommand extends GuildCommand {
    @Override
    public void run(GuildCommandEvent event) {
        Check.check(event.getArgs().size() > 1, CommandArgumentException::new);

        String regex = event.getArgs().get(0);
        try {
            Pattern.compile(regex);
        } catch (PatternSyntaxException exception) {
            throw new ReplyError("That regex is not valid!");
        }

        String message = lastArg(1, event);

        Triggers.addTrigger(event.getGuild(), regex, message);
        event.getChannel().sendMessageFormat("Successfully added trigger `%s`", regex).queue();

    }

    @SubCommand(TriggerCommand.class)
    @CommandOptions(
        name = "list",
        description = "List all triggers",
        perm = CommandPerm.BOT_MODERATOR
    )
    private static class TList extends GuildCommand {
        @Override
        public void run(GuildCommandEvent event) {
            Check.check(event.getArgs().isEmpty(), CommandArgumentException::new);
            Check.check(Triggers.hasTriggers(event.getGuild()),
                () -> new EntityNotFoundException("No triggers found!"));

            PagedEmbed p = new PagedEmbed(EmbedUtil.pagedDescription(
                new EmbedBuilder().setTitle("Available Triggers").build(), Triggers.getTriggers(
                    event.getGuild()).stream().map(t -> String.format("`%s` - `%s`\n",
                    t.getRegex(), t.getOutput())).collect(Collectors.toList())), event);

            PageListener.add(p);
        }
    }

    @SubCommand(TriggerCommand.class)
    @CommandOptions(
        name = "remove",
        description = "Deletes a trigger",
        usage = "<regex>",
        alias = {"delete", "del", "rm"},
        perm = CommandPerm.BOT_MANAGER
    )
    private static class Remove extends GuildCommand {
        @Override
        public void run(GuildCommandEvent event) {
            Check.check(event.getArgs().size() == 1, CommandArgumentException::new);
            Check.check(Triggers.isTrigger(event.getGuild(), event.getArgs().get(0)),
                () -> new ReplyError("That trigger does not exist!"));

            event.getChannel().sendMessageFormat("Removed the trigger `%s`", Triggers.removeTrigger(event.getGuild(),
                event.getArgs().get(0))).queue();
        }
    }
}
