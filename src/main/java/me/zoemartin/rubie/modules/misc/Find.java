package me.zoemartin.rubie.modules.misc;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.exceptions.CommandArgumentException;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.Check;
import me.zoemartin.rubie.core.util.EmbedUtil;
import me.zoemartin.rubie.modules.levels.Levels;
import me.zoemartin.rubie.modules.pagedEmbeds.PageListener;
import me.zoemartin.rubie.modules.pagedEmbeds.PagedEmbed;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Find implements GuildCommand {
    @Override
    public void run(GuildCommandEvent event) {
        Check.check(!event.getArgs().isEmpty(), CommandArgumentException::new);

        List<Member> guildMembers = event.getGuild().loadMembers().get();
        String search = lastArg(0, event);
        List<Member> members = guildMembers.stream()
                                   .filter(m -> m.getEffectiveName().contains(search)).collect(Collectors.toList());
        List<User> users = event.getJDA().getUsers().stream()
                               .filter(u -> u.getName().contains(search)
                                                && members.stream().map(Member::getUser).noneMatch(u::equals))
                               .collect(Collectors.toList());

        PagedEmbed p = new PagedEmbed(EmbedUtil.pagedCommentedDescription(
            new EmbedBuilder().setTitle("Users with `" + search + "`").build(),
            Stream.concat(
                members.stream().map(m -> String.format("%d. %s - %s - %s\n\n",
                    members.indexOf(m) + 1, m.getEffectiveName(), m.getUser().getAsTag(), m.getId())),
                users.stream().map(u -> String.format("%d. %s - %s\n\n",
                    users.indexOf(u) + members.size(), u.getAsTag(), u.getId()))
            ).collect(Collectors.toList())),
            event.getChannel(), event.getUser());

        PageListener.add(p);
    }

    @NotNull
    @Override
    public String name() {
        return "find";
    }

    @NotNull
    @Override
    public CommandPerm commandPerm() {
        return CommandPerm.BOT_MODERATOR;
    }

    @NotNull
    @Override
    public String description() {
        return "Finds a user";
    }
}
