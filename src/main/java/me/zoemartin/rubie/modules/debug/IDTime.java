package me.zoemartin.rubie.modules.debug;

import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.annotations.Command;
import me.zoemartin.rubie.core.annotations.CommandOptions;
import me.zoemartin.rubie.core.exceptions.CommandArgumentException;
import me.zoemartin.rubie.core.exceptions.ReplyError;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.Check;
import org.joda.time.Instant;

@Command
@CommandOptions(
    name = "idtime",
    description = "Outputs the creation time of a discord id",
    usage = "<id entity>"
)
public class IDTime extends GuildCommand {
    @Override
    public void run(GuildCommandEvent event) {
        Check.check(event.getArgs().size() == 1, CommandArgumentException::new);
        String ref = event.getArgs().get(0).replaceAll("\\D", "");
        Check.check(ref.matches("\\d{17,19}"),
            () -> new ReplyError("Error, `%s` is not a valid id", ref));

        long id = Long.parseLong(ref);
        long time = (id >> 22) + 1420070400000L;

        embedReply(event, "Timestamp of `" + id + "`", Instant.ofEpochMilli(time).toString()).queue();
    }
}
