package me.zoemartin.rubie.modules.trigger;

import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.exceptions.*;
import me.zoemartin.rubie.core.interfaces.Command;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.Check;
import me.zoemartin.rubie.core.util.EmbedUtil;
import me.zoemartin.rubie.modules.pagedEmbeds.PageListener;
import me.zoemartin.rubie.modules.pagedEmbeds.PagedEmbed;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public class TriggerCommand implements GuildCommand {
    @Override
    public @NotNull Set<Command> subCommands() {
        return Set.of(new TList(), new Remove());
    }

    @Override
    public @NotNull String name() {
        return "trigger";
    }

    @Override
    public @NotNull String regex() {
        return "trigger|autoresponse|ar";
    }

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

    @Override
    public @NotNull CommandPerm commandPerm() {
        return CommandPerm.BOT_MANAGER;
    }

    @Override
    public @NotNull String usage() {
        return "<regex> <output...>";
    }

    @Override
    public @NotNull String description() {
        return "Create/List/Remove a regex message trigger";
    }

    private static class TList implements GuildCommand {
        @Override
        public @NotNull String name() {
            return "list";
        }

        @Override
        public void run(GuildCommandEvent event) {
            Check.check(event.getArgs().isEmpty(), CommandArgumentException::new);
            Check.check(Triggers.hasTriggers(event.getGuild()),
                () -> new EntityNotFoundException("No triggers found!"));

            PagedEmbed p = new PagedEmbed(EmbedUtil.pagedDescription(
                new EmbedBuilder().setTitle("Available Triggers").build(), Triggers.getTriggers(
                    event.getGuild()).stream().map(t -> String.format("`%s` - `%s`\n",
                    t.getRegex(), t.getOutput())).collect(Collectors.toList())),
                event.getChannel(), event.getUser());

            PageListener.add(p);
        }

        @Override
        public @NotNull CommandPerm commandPerm() {
            return CommandPerm.BOT_MODERATOR;
        }

        @Override
        public @NotNull String description() {
            return "Lists all triggers";
        }
    }

    private static class Remove implements GuildCommand {
        @Override
        public @NotNull String name() {
            return "remove";
        }

        @Override
        public @NotNull String regex() {
            return "remove|delete|del|rem|rm";
        }

        @Override
        public void run(GuildCommandEvent event) {
            Check.check(event.getArgs().size() == 1, CommandArgumentException::new);
            Check.check(Triggers.isTrigger(event.getGuild(), event.getArgs().get(0)),
                () -> new ReplyError("That trigger does not exist!"));

            event.getChannel().sendMessageFormat("Removed the trigger `%s`", Triggers.removeTrigger(event.getGuild(),
                event.getArgs().get(0))).queue();
        }

        @Override
        public @NotNull CommandPerm commandPerm() {
            return CommandPerm.BOT_MANAGER;
        }

        @Override
        public @NotNull String usage() {
            return "<regex>";
        }

        @Override
        public @NotNull String description() {
            return "Deletes a trigger";
        }
    }
}
