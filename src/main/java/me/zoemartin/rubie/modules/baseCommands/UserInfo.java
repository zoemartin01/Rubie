package me.zoemartin.rubie.modules.baseCommands;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.annotations.Command;
import me.zoemartin.rubie.core.annotations.CommandOptions;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.*;
import me.zoemartin.rubie.modules.commandProcessing.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import java.sql.Timestamp;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

@Command
@CommandOptions(
    name = "userinfo",
    description = "Shows information about a User",
    usage = "[user]",
    alias = {"i", "info", "profile"}
)
public class UserInfo extends GuildCommand {
    @Override
    public void run(GuildCommandEvent event) {
        User u = null;
        String arg;
        if (event.getArgs().isEmpty()) u = event.getUser();
        else if (Parser.User.isParsable(arg = lastArg(0, event))) u = CacheUtils.getUser(arg);
        else if (Parser.User.tagIsParsable(arg)) u = Bot.getJDA().getUserByTag(arg);
        if (u == null) u = event.getUser();
        Member member = CacheUtils.getMember(event.getGuild(), u.getId());

        EmbedBuilder eb;
        if (member == null) {
            eb = new EmbedBuilder()
                     .setAuthor(u.getAsTag(), null, u.getEffectiveAvatarUrl())
                     .setDescription(u.getId())
                     .setThumbnail(u.getAvatarUrl())
                     .setFooter("ID: " + u.getId())
                     .setTimestamp(Instant.now())
                     .addField("Mention", u.getAsMention(), true)
                     .addField("Username", u.getAsTag(), true)
                     .addField("Avatar",
                         String.format("[Link](%s)", u.getEffectiveAvatarUrl()), true)
                     .addField("Registered at",
                         Timestamp.valueOf(u.getTimeCreated().atZoneSameInstant(ZoneOffset.UTC)
                                               .toLocalDateTime()) + " UTC",
                         true)
                     .addField("Account Age", TimeUtils.dateAgo(u.getTimeCreated(),
                         OffsetDateTime.now()), true);

        } else {
            eb = new EmbedBuilder()
                     .setAuthor(u.getAsTag(), null, u.getEffectiveAvatarUrl())
                     .setColor(member.getColor())
                     .setDescription(u.getId())
                     .setThumbnail(u.getAvatarUrl())
                     .setFooter("ID: " + u.getId())
                     .setTimestamp(Instant.now())
                     .addField("Mention", u.getAsMention(), true)
                     .addField("Username", u.getAsTag(), true)
                     .addField("Nickname", member.getEffectiveName(), true)
                     .addField("Avatar", String.format("[Link](%s)", u.getEffectiveAvatarUrl()), true)
                     .addField("Highest Role",
                         member.getRoles().isEmpty() ? "n/a" : member.getRoles().get(0).getAsMention(), true);

            MemberPermission mp = PermissionHandler.getMemberPerm(event.getGuild().getId(), member.getId());
            CommandPerm mcp = mp.getPerm();

            CommandPerm rp = CommandPerm.fromNum(
                PermissionHandler.getRolePerms(
                    event.getGuild().getId()).stream().filter(
                    rolePermission -> member.getRoles().stream()
                                          .anyMatch(role -> role.getId().equals(rolePermission.getRole_id()))
                ).map(RolePermission::getPerm)
                    .map(CommandPerm::raw).max(Integer::compareTo).orElse(0));

            if (mcp.raw() >= rp.raw() && mcp.raw() > 0)
                eb.addField("Bot Perm", String.format("`[%d] %s`", mp.getPerm().raw(), mp.getPerm().toString()), true);
            else if (rp.raw() >= mcp.raw() && rp.raw() > 0)
                eb.addField("Bot Perm", String.format("`[%d] %s`", rp.raw(), rp.toString()), true);


            eb.addField("Registered at",
                Timestamp.valueOf(u.getTimeCreated().atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()) + " UTC",
                true)
                .addField("Account Age", TimeUtils.dateAgo(u.getTimeCreated(), OffsetDateTime.now()), true)
                .addField("Joined at",
                    Timestamp.valueOf(member.getTimeJoined().atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime())
                        + " UTC", true)
                .addField("Joined Server", TimeUtils.dateAgo(member.getTimeJoined(), OffsetDateTime.now())
                                               + " ago\n"
                                               + ChronoUnit.DAYS.between(event.getGuild().getTimeCreated(),
                    member.getTimeJoined())
                                               + " days after the server was created", false);

            String roles = member.getRoles().stream().map(Role::getAsMention).collect(Collectors.joining(", "));
            eb.addField(String.format("Roles (%s)", member.getRoles().size()),
                roles.length() <= 1024 ? roles : "Too many to list", false);

        }
        event.getChannel().sendMessageEmbeds(eb.build()).queue();
    }
}
