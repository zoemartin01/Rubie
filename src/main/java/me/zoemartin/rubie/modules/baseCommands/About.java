package me.zoemartin.rubie.modules.baseCommands;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.CommandEvent;
import me.zoemartin.rubie.core.annotations.Command;
import me.zoemartin.rubie.core.annotations.CommandOptions;
import me.zoemartin.rubie.core.interfaces.AbstractCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import org.joda.time.*;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Manifest;

@Command
@CommandOptions(
    name = "about",
    description = "Shows info about the bot",
    alias = "botinfo"
)
public class About extends AbstractCommand {
    private String JDA_VERSION = null;
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
        PeriodFormatter formatter = new PeriodFormatterBuilder()
                                        .appendDays()
                                        .appendSuffix(" days, ")
                                        .appendHours()
                                        .appendSuffix(" hours, ")
                                        .appendMinutes()
                                        .appendSuffix(" minutes, ")
                                        .appendSeconds()
                                        .appendSuffix(" seconds")
                                        .toFormatter();
        eb.addField("Uptime", formatter.print(new Duration(STARTUP, DateTime.now()).toPeriodFrom(STARTUP)), true);
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

    private void findVersion() {
        Enumeration<URL> resources;
        try {
            resources = getClass().getClassLoader()
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
