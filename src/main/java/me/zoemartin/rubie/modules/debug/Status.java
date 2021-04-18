package me.zoemartin.rubie.modules.debug;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.CommandEvent;
import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.annotations.*;
import me.zoemartin.rubie.core.exceptions.CommandArgumentException;
import me.zoemartin.rubie.core.interfaces.AbstractCommand;
import me.zoemartin.rubie.core.util.Check;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@Command
@CommandOptions(
    name = "status",
    description = "Shows the current status",
    perm = CommandPerm.OWNER
)
public class Status extends AbstractCommand {
    private static final ScheduledExecutorService refresh = Executors.newSingleThreadScheduledExecutor();
    private static final ScheduledExecutorService rotate = Executors.newSingleThreadScheduledExecutor();
    private static final Map<String, Supplier<String>> replacements = new HashMap<>();
    private static final Counter counter = new Counter(1);

    private static List<String> status = new LinkedList<>();
    private static List<Activity.ActivityType> activity = new LinkedList<>();

    // Status rotates every 5 minutes and refreshes the templates every 60 seconds
    public Status() {
        loadRotation();

        Supplier<JDA> jda = Bot::getJDA;
        replacements.put("users", () -> {
            jda.get().getGuilds().forEach(guild -> guild.loadMembers().get());
            return String.valueOf(jda.get().getUsers().size());
        });
        replacements.put("servers", () -> String.valueOf(jda.get().getGuilds().size()));
        replacements.put("version", () -> {
            var ver = getClass().getPackage().getImplementationVersion();
            return ver == null ? "DEV BUILD" : ver;
        });

        refresh.scheduleAtFixedRate(() -> {
            try {
                var act = Activity.of(activity.get(counter.current), replace(status.get(counter.current)));
                jda.get().getPresence().setActivity(act);
            } catch (Exception ignored) {
            }
        }, 5, 60, TimeUnit.SECONDS);

        rotate.scheduleAtFixedRate(() -> {
            synchronized (counter) {
                if (counter.getMax() == 1) return;

                if (counter.getCurrent() + 1 == counter.getMax()) counter.setCurrent(0);
                else counter.incrementCurrent();
            }
        }, 5, 5, TimeUnit.MINUTES);
    }

    @SubCommand(Status.class)
    @CommandOptions(
        name = "custom",
        description = "Sets the Bot's status",
        usage = "<activity> <status...>",
        help = "Valid activities are: \n" +
                   "Playing/Streaming/Listening/Watching \n\n" +
                   "A status can contain the following templates: \n" +
                   "%users% - Shows the current Bot User Count \n" +
                   "%servers% - Shows the current Server count \n" +
                   "%version% - Shows the current Bot Version",
        perm = CommandPerm.OWNER
    )
    private static class Custom extends AbstractCommand {
        @Override
        public void run(CommandEvent event) {
            var args = event.getArgs();
            Check.check(!args.isEmpty(), CommandArgumentException::new);
            activity = new LinkedList<>();
            status = new LinkedList<>();

            activity.add(activityType(args.get(0)));
            status.add(lastArg(1, event));
            synchronized (counter) {
                counter.setMax(1);
                counter.setCurrent(0);
            }
            event.addCheckmark();
        }

        @SubCommand(Custom.class)
        @CommandOptions(
            name = "add",
            description = "Adds a status to the rotation",
            usage = "<activity> <status...>",
            help = "Valid activities are: \n" +
                       "Playing/Streaming/Listening/Watching \n\n" +
                       "A status can contain the following templates: \n" +
                       "%users% - Shows the current Bot User Count \n" +
                       "%servers% - Shows the current Server count \n" +
                       "%version% - Shows the current Bot Version",
            perm = CommandPerm.OWNER
        )
        private static class Add extends AbstractCommand {
            @Override
            public void run(CommandEvent event) {
                var args = event.getArgs();
                Check.check(!args.isEmpty(), CommandArgumentException::new);
                activity.add(activityType(args.get(0)));
                status.add(lastArg(1, event));
                synchronized (counter) {
                    counter.incrementMax();
                }
                event.addCheckmark();
            }
        }

        @SubCommand(Custom.class)
        @CommandOptions(
            name = "reset",
            description = "Resets the status to the config file",
            perm = CommandPerm.OWNER
        )
        private static class Reset extends AbstractCommand {
            @Override
            public void run(CommandEvent event) {
                loadRotation();
                event.addCheckmark();
            }
        }
    }

    @Override
    public void run(CommandEvent event) {
        event.reply("Current Status:", "%s %s",
            activity.get(counter.current).name().toLowerCase().replace("default", "playing"),
            status.get(counter.current)).queue();
        event.addCheckmark();
    }

    private static void loadRotation() {
        activity = new LinkedList<>();
        status = new LinkedList<>();

        var prop = Bot.getProperties();
        var count = Integer.parseInt(prop.getProperty("bot.status.count"));

        for (int i = 0; i < count; i++) {
            status.add(i, prop.getProperty("bot.status." + i));
            activity.add(i, activityType(prop.getProperty("bot.activity." + i)));
        }

        synchronized (counter) {
            counter.setMax(count);
        }
    }

    private static Activity.ActivityType activityType(String str) {
        return switch (str.toLowerCase()) {
            case "playing" -> Activity.ActivityType.DEFAULT;
            case "streaming" -> Activity.ActivityType.STREAMING;
            case "listening" -> Activity.ActivityType.LISTENING;
            case "watching" -> Activity.ActivityType.WATCHING;
            default -> throw new CommandArgumentException();
        };
    }

    private static String replace(String str) {
        var string = new AtomicReference<>(str);
        replacements.forEach((s, stringSupplier) ->
                                 string.set(string.get().replaceAll("%" + s + "%", stringSupplier.get())));
        return string.get();
    }

    private static final class Counter {
        private Integer current = 0;
        private Integer max;

        public Counter(Integer max) {
            this.max = max;
        }

        synchronized Integer getCurrent() {
            return current;
        }

        synchronized void setCurrent(Integer current) {
            this.current = current;
        }

        synchronized void incrementCurrent() {
            current++;
        }

        synchronized Integer getMax() {
            return max;
        }

        synchronized void setMax(Integer max) {
            this.max = max;
        }

        synchronized void incrementMax() {
            max++;
        }
    }
}
