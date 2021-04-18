package me.zoemartin.rubie.modules.baseCommands;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.CommandEvent;
import me.zoemartin.rubie.core.annotations.Command;
import me.zoemartin.rubie.core.annotations.CommandOptions;
import me.zoemartin.rubie.core.interfaces.AbstractCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.PeriodFormatterBuilder;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;
import java.util.jar.Manifest;

@Command
@CommandOptions(
    name = "about",
    description = "Shows info about the bot",
    alias = "botinfo"
)
public class About extends AbstractCommand {
    private static String JDA_VERSION = null;
    private static DateTime STARTUP;
    private static String OWNER_TAG = null;

    public About() {
        STARTUP = DateTime.now();
    }

    @Override
    public void run(CommandEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("About").setColor(0xdf136c);

        String version = getClass().getPackage().getImplementationVersion();

        if (JDA_VERSION == null) findVersion();

        eb.addField("Bot Version", version == null ? "DEV BUILD" : version, true);
        eb.addField("Java Version", System.getProperty("java.version"), true);
        eb.addField("JDA Version", JDA_VERSION, true);
        eb.addField("Uptime", format(STARTUP, DateTime.now()), true);
        Runtime rt = Runtime.getRuntime();
        long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        eb.addField("Memory Usage", usedMB + " MB", true);
        eb.addField("Author", "<@!212591138945630213> / zowee#0001", false);
        eb.addField("Source Code", "https://github.com/zoemartin01/Rubie", false);
        eb.addField("Invite", "https://zoe.pm/invrubie", false);
        eb.setThumbnail(Bot.getJDA().getSelfUser().getAvatarUrl());
        eb.setFooter("Made with JDA",
            "https://raw.githubusercontent.com/DV8FromTheWorld/JDA/assets/assets/readme/logo.png");

        event.getChannel().sendMessage(eb.build()).queue();
    }

    private static String format(DateTime s, DateTime e) {
        // TODO: Refactor into TimeUtils
        var d = new Duration(s, e);
        var p = d.toPeriodFrom(s);

        var f1 = new PeriodFormatterBuilder()
                     .appendYears()
                     .appendSuffix(p.getYears() > 1 ? " years " : " year ")
                     .appendMonths()
                     .appendSuffix(p.getMonths() > 1 ? " months " : " month ")
                     .appendWeeks()
                     .appendSuffix(p.getWeeks() > 1 ? " weeks " : " week ")
                     .appendPrefix(d.getMillis() > TimeUnit.DAYS.toMillis(7) ? " and " : "")
                     .appendDays()
                     .appendSuffix(p.getDays() > 1 ? " days " : " day ")
                     .toFormatter();

        var f2 = new PeriodFormatterBuilder()
                     .appendWeeks()
                     .appendSuffix(p.getWeeks() > 1 ? " weeks " : " week ")
                     .appendDays()
                     .appendSuffix(p.getDays() > 1 ? " days " : " day ")
                     .appendHours()
                     .appendSuffix(p.getHours() > 1 ? " hours " : " hour ")
                     .appendMinutes()
                     .appendSuffix(p.getMinutes() > 1 ? " minutes " : " minute ")
                     .appendPrefix(d.getMillis() > TimeUnit.MINUTES.toMillis(1) ? " and " : "")
                     .appendSeconds()
                     .appendSuffix(p.getSeconds() > 1 ? " seconds" : " second")
                     .toFormatter();

        if (d.getMillis() >= TimeUnit.DAYS.toMillis(7)) return f1.print(p);
        else return f2.print(p);
    }

    private static void findVersion() {
        Enumeration<URL> resources;
        try {
            resources = About.class.getClassLoader()
                            .getResources("META-INF/MANIFEST.MF");
            while (resources.hasMoreElements()) {
                Manifest manifest = new Manifest(resources.nextElement().openStream());
                JDA_VERSION = manifest.getMainAttributes().getValue("jda-version");
                if (JDA_VERSION == null) JDA_VERSION = "UNKNOWN";
            }
        } catch (IOException e) {
            JDA_VERSION = "UNKNOWN";
        }
    }
}
