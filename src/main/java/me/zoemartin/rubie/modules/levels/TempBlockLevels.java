package me.zoemartin.rubie.modules.levels;

import com.google.auto.service.AutoService;
import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.Job;
import me.zoemartin.rubie.core.interfaces.JobProcessor;
import me.zoemartin.rubie.core.util.DatabaseUtil;

import java.util.Set;
import java.util.function.Consumer;

import static me.zoemartin.rubie.core.Job.CommonKeys.*;

@AutoService(JobProcessor.class)
public class TempBlockLevels implements JobProcessor {
    @Override
    public String uuid() {
        return "6b54f418-4937-4cf2-af70-d1792fef1360";
    }

    @Override
    public Consumer<Job> process() {
        return job -> {
            var settings = job.getSettings();

            if (!settings.keySet().containsAll(Set.of(GUILD, USER))) return;
            var g = Bot.getJDA().getGuildById(settings.get(GUILD));
            if (g == null) return;
            var conf = Levels.getConfig(g);
            conf.unblocksUser(settings.get(USER));
            DatabaseUtil.updateObject(conf);
        };
    }
}
