package me.zoemartin.rubie.modules.debug;

import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

public class Dump implements GuildCommand {
    @Override
    public @NotNull String name() {
        return "dump";
    }

    @Override
    public void run(GuildCommandEvent event) {
        List<Member> members = event.getGuild().loadMembers().get();

        String dump = members.stream().map(member -> String.format("%d. %s / %s / %s / Joined at: %s UTC",
            members.indexOf(member) + 1, member.getUser().getAsTag(), member.getEffectiveName(), member.getId(),
            Timestamp.valueOf(member.getTimeCreated().atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()))
        ).collect(Collectors.joining("\n"));

        if (dump.length() <= 1990) event.getChannel().sendMessage("```" + dump + "```").queue();
        else {
            event.getChannel().sendFile(dump.getBytes(), String.format("%s-dump-%s.txt",
                event.getGuild().getName(), System.currentTimeMillis())).queue();
        }
    }

    @Override
    public @NotNull CommandPerm commandPerm() {
        return CommandPerm.BOT_ADMIN;
    }

    @Override
    public @NotNull String description() {
        return "Refreshes the cache and dumps all users";
    }
}
