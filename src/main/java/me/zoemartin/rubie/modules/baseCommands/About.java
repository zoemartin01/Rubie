package me.zoemartin.rubie.modules.baseCommands;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.interfaces.Command;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.jar.Manifest;

public class About implements Command {
    private String JDA_VERSION = null;
    private static DateTime STARTUP;

    public About() {
        STARTUP = DateTime.now();
    }

    @Override
    public @NotNull String name() {
        return "about";
    }

    @Override
    public @NotNull String regex() {
        return "about|botinfo";
    }

    @Override
    public void run(User user, MessageChannel channel, List<String> args, Message original, String invoked) {
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
        eb.addField("Uptime", formatter.print(new Duration(STARTUP, DateTime.now()).toPeriod()), true);
        Runtime rt = Runtime.getRuntime();
        long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        eb.addField("Memory Usage", usedMB + " MB", true);
        eb.addField("Author", "<@!212591138945630213> / zowee#0001", false);
        eb.addField("Source Code", "https://github.com/zoemartin01/Rubie", false);
        eb.setThumbnail(Bot.getJDA().getSelfUser().getAvatarUrl());
        eb.setFooter("Made with JDA",
            "https://raw.githubusercontent.com/DV8FromTheWorld/JDA/assets/assets/readme/logo.png");

        channel.sendMessage(eb.build()).queue();
    }

    @Override
    public @NotNull CommandPerm commandPerm() {
        return CommandPerm.EVERYONE;
    }

    @Override
    public @NotNull String description() {
        return "Shows info about the bot";
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
