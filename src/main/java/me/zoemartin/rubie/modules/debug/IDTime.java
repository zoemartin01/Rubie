package me.zoemartin.rubie.modules.debug;

import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.exceptions.CommandArgumentException;
import me.zoemartin.rubie.core.exceptions.ReplyError;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.Check;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;
import org.joda.time.Instant;

import java.util.List;

public class IDTime implements GuildCommand {
    @Override
    public void run(Member user, TextChannel channel, List<String> args, Message original, String invoked) {
        Check.check(args.size() == 1, CommandArgumentException::new);
        String ref = args.get(0).replaceAll("\\D", "");
        Check.check(ref.matches("\\d{17,19}"),
            () -> new ReplyError("Error, `%s` is not a valid id", ref));

        long id = Long.parseLong(ref);
        long time = (id >> 22) + 1420070400000L;

        embedReply(original, channel, "Timestamp of `" + id + "`", Instant.ofEpochMilli(time).toString()).queue();
    }

    @NotNull
    @Override
    public String name() {
        return "idtime";
    }

    @NotNull
    @Override
    public CommandPerm commandPerm() {
        return CommandPerm.EVERYONE;
    }

    @NotNull
    @Override
    public String description() {
        return "Outputs the creation time of a discord id";
    }
}
