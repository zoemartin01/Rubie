package me.zoemartin.rubie.modules.baseCommands;

import de.androidpit.colorthief.ColorThief;
import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.annotations.Command;
import me.zoemartin.rubie.core.annotations.CommandOptions;
import me.zoemartin.rubie.core.exceptions.CommandArgumentException;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.Check;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.EnumSet;
import java.util.stream.Collectors;

@Command
@CommandOptions(
    name = "serverinfo",
    description = "Shows information about the Server"
)
public class ServerInfo extends GuildCommand {
    @SuppressWarnings("ConstantConditions")
    @Override
    public void run(GuildCommandEvent event) {
        Check.check(event.getArgs().isEmpty(), CommandArgumentException::new);

        Guild guild = event.getGuild();

        EmbedBuilder eb = new EmbedBuilder()
                              .setAuthor(guild.getName(), null, guild.getIconUrl())
                              .setThumbnail(guild.getIconUrl())
                              .setTitle("Server Info");
        try {
            int[] color = ColorThief.getColor(ImageIO.read(new URL(guild.getIconUrl())));
            eb.setColor(new Color(color[0], color[1], color[2]));
        } catch (IOException ignored) {
        }

        guild.loadMembers().get();
        eb.addField("Owner", guild.getOwner().getAsMention(), true);

        long vcLocked = guild.getVoiceChannelCache().stream().filter(c -> {
            if (c.getPermissionOverride(guild.getPublicRole()) == null) return false;

            EnumSet<Permission> overrides = c.getPermissionOverride(guild.getPublicRole()).getDenied();
            return overrides.contains(Permission.MESSAGE_READ) || overrides.contains(Permission.VOICE_CONNECT);
        }).count();
        long textLocked = guild.getTextChannelCache().stream().filter(c -> {
            if (c.getPermissionOverride(guild.getPublicRole()) == null) return false;

            EnumSet<Permission> overrides = c.getPermissionOverride(guild.getPublicRole()).getDenied();
            return overrides.contains(Permission.MESSAGE_READ) || overrides.contains(Permission.MESSAGE_WRITE);
        }).count();

        var features = guild.getFeatures().stream()
                           .map(s -> s.replaceAll("_", " ")
                                         .toLowerCase()).collect(Collectors.toList());

        eb.addField("Channels",
            String.format("<:voice_channel:758143690845454346> %d %s\n<:text_channel:758143729902288926> %d %s",
                guild.getVoiceChannelCache().size(), vcLocked > 0 ? "(" + vcLocked + " locked)" : "",
                guild.getTextChannelCache().size(), textLocked > 0 ? "(" + textLocked + " locked)" : ""), true)

            .addField("Members", String.format("Total: %d\nHumans: %d\nBots: %d",
                guild.getMemberCount(),
                guild.getMembers().stream().filter(member -> !member.getUser().isBot()).count(),
                guild.getMembers().stream().filter(member -> member.getUser().isBot()).count()), true)

            .addField("Roles", guild.getRoles().size() + "", true)
            .addField("Region", guild.getRegionRaw(), true)
            .addField("Categories", guild.getCategories().size() + "", true)

            .addField("Features", features.isEmpty() ? "`n/a`" :
                                      ("```" + String.join(", ", features) + "```"), true)

            .setFooter("ID: " + guild.getId() + " | Created")
            .setTimestamp(guild.getTimeCreated());

        event.getChannel().sendMessage(eb.build()).queue();
    }
}
