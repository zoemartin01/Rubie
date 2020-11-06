package me.zoemartin.rubie.modules.debug;

import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.util.List;

public class Stats implements GuildCommand {
    private static DateTime STARTUP;

    @Override
    public void run(Member user, TextChannel channel, List<String> args, Message original, String invoked) {
        Member self = original.getGuild().getSelfMember();

        EmbedBuilder eb = new EmbedBuilder()
                              .setTitle("Stats")
                              .setColor(self.getColor())
                              .setAuthor(String.format("%s (%s)", self.getUser().getAsTag(), self.getId()), null,
                                  self.getUser().getAvatarUrl());

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
        channel.sendMessage(eb.build()).queue();
    }

    public Stats() {
        STARTUP = DateTime.now();
    }

    @NotNull
    @Override
    public String name() {
        return "stats";
    }

    @NotNull
    @Override
    public CommandPerm commandPerm() {
        return CommandPerm.BOT_USER;
    }

    @NotNull
    @Override
    public String description() {
        return "Displays stats about the bot";
    }
}
