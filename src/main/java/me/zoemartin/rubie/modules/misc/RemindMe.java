package me.zoemartin.rubie.modules.misc;

import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.annotations.Command;
import me.zoemartin.rubie.core.annotations.CommandOptions;
import me.zoemartin.rubie.core.exceptions.CommandArgumentException;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.interfaces.JobProcessorInterface;
import me.zoemartin.rubie.core.managers.JobManager;
import me.zoemartin.rubie.core.util.Check;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.util.concurrent.ConcurrentHashMap;

import static me.zoemartin.rubie.core.Job.CommonKeys.*;

@Command
@CommandOptions(
    name = "remindme",
    description = "Create Reminders ",
    usage = "<in> <reminder>",
    help = "Time format ex: 1y2mo5w3d2h1m10s"
)
public class RemindMe extends GuildCommand {
    static final String MSG_KEY = "reminder";

    private static final JobProcessorInterface processor = new RemindMeJob();
    private static final PeriodFormatter formatter = new PeriodFormatterBuilder()
                                                         .appendYears()
                                                         .appendSuffix("y")
                                                         .appendSeparator(" ", " ", new String[]{",", ", "})
                                                         .appendMonths()
                                                         .appendSuffix("mo")
                                                         .appendSeparator(" ", " ", new String[]{",", ", "})
                                                         .appendWeeks()
                                                         .appendSuffix("w")
                                                         .appendSeparator(" ", " ", new String[]{",", ", "})
                                                         .appendDays()
                                                         .appendSuffix("d")
                                                         .appendSeparator(" ", " ", new String[]{",", ", "})
                                                         .appendHours()
                                                         .appendSuffix("h")
                                                         .appendSeparator(" ", " ", new String[]{",", ", "})
                                                         .appendMinutes()
                                                         .appendSuffix("m")
                                                         .appendSeparator(" ", " ", new String[]{",", ", "})
                                                         .appendSecondsWithOptionalMillis()
                                                         .appendSuffix("s")
                                                         .toFormatter();

    @Override
    public void run(GuildCommandEvent event) {
        var args = event.getArgs();
        Check.check(!args.isEmpty(), CommandArgumentException::new);

        var period = Period.parse(args.get(0), formatter);
        var end = DateTime.now().plus(period);

        var settings = new ConcurrentHashMap<String, String>();
        var message = lastArg(1, event);

        settings.put(GUILD, event.getGuild().getId());
        settings.put(CHANNEL, event.getChannel().getId());
        settings.put(USER, event.getUser().getId());
        settings.put(MSG_KEY, message);

        JobManager.newJob(processor, end.getMillis(), settings);
        event.reply("Reminder", "Okay I'll remind you about `%s` at %s",
            message.isBlank() ? "¯\\_(ツ)_/¯" : message, end.toString("yyyy-MM-dd HH:mm:ss")).queue();
    }

}
