package me.zoemartin.rubie.modules.debug;

import me.zoemartin.rubie.core.CommandPerm;
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
    public void run(Member user, TextChannel channel, List<String> args, Message original, String invoked) {
        List<Member> members = original.getGuild().loadMembers().get();

        String dump = members.stream().map(member -> String.format("%d. %s / %s / %s / Joined at: %s UTC",
            members.indexOf(member) + 1, member.getUser().getAsTag(), member.getEffectiveName(), member.getId(),
            Timestamp.valueOf(member.getTimeCreated().atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()))
        ).collect(Collectors.joining("\n"));

        if (dump.length() <= 1990) channel.sendMessage("```" + dump + "```").queue();
        else {
            channel.sendFile(dump.getBytes(), String.format("%s-dump-%s.txt",
                original.getGuild().getName(), System.currentTimeMillis())).queue();
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
