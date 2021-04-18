package me.zoemartin.rubie.modules.debug;

import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.annotations.Command;
import me.zoemartin.rubie.core.annotations.CommandOptions;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import net.dv8tion.jda.api.entities.Member;

import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Command
@CommandOptions(
    name = "dump",
    description = "Refreshes the cache and dumps all Members",
    perm = CommandPerm.BOT_ADMIN
)
public class Dump extends GuildCommand {
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
}
