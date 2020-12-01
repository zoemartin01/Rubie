package me.zoemartin.rubie.modules.funCommands;

import com.google.auto.service.AutoService;
import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.Job;
import me.zoemartin.rubie.core.interfaces.JobProcessor;
import me.zoemartin.rubie.core.util.CacheUtils;
import net.dv8tion.jda.api.EmbedBuilder;

import java.time.Instant;
import java.util.Set;
import java.util.function.Consumer;

import static me.zoemartin.rubie.core.Job.CommonKeys.*;

@AutoService(JobProcessor.class)
public class RemindMeJob implements JobProcessor {
    private static final String DEFAULT = "¯\\_(ツ)_/¯";

    @Override
    public String uuid() {
        return "3b05704a-a483-41bd-89a8-ae3c133280af";
    }

    @Override
    public Consumer<Job> process() {
        return job -> {
            var settings = job.getSettings();

            if (!settings.keySet().containsAll(Set.of(CHANNEL, GUILD, USER))) return;

            var g = Bot.getJDA().getGuildById(settings.get(GUILD));
            if (g == null) return;
            var u = CacheUtils.getMember(g, settings.get(USER));
            var c = g.getTextChannelById(settings.get(CHANNEL));
            if (c == null || u == null) return;

            var message = settings.getOrDefault(RemindMe.MSG_KEY, DEFAULT);
            var embed = new EmbedBuilder().setTitle("Reminder").setTimestamp(Instant.now())
                .setDescription("You asked me to remind you about\n\n" + (message.isBlank() ? DEFAULT : message))
                            .build();
            c.sendMessage(u.getAsMention()).embed(embed).queue();
        };
    }
}
