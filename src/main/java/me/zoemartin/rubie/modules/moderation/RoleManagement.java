package me.zoemartin.rubie.modules.moderation;

import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.annotations.*;
import me.zoemartin.rubie.core.exceptions.CommandArgumentException;
import me.zoemartin.rubie.core.exceptions.ReplyError;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Command
@CommandOptions(
    name = "role",
    description = "Role Management",
    perm = CommandPerm.BOT_MANAGER

)
public class RoleManagement extends GuildCommand {
    @Override
    public void run(GuildCommandEvent event) {
        throw new CommandArgumentException();
    }

    @SubCommand(RoleManagement.class)
    @CommandOptions(
        name = "info",
        description = "Shows information about a Role",
        usage = "<role>",
        perm = CommandPerm.BOT_MODERATOR
    )
    @SubCommand.AsBase(name = "roleinfo")
    private static class Info extends GuildCommand {
        @Override
        public void run(GuildCommandEvent event) {
            Check.check(!event.getArgs().isEmpty(), CommandArgumentException::new);

            String roleRef = lastArg(0, event);
            Role role = Parser.Role.getRole(event.getGuild(), roleRef);
            Check.entityReferenceNotNull(role, Role.class, roleRef);

            EmbedBuilder eb =
                new EmbedBuilder()
                    .addField("Name", role.getName(), true)
                    .addField("ID", role.getId(), true)
                    .addField("Color", role.getColor() != null ? "#" + Integer.toHexString(
                        role.getColor().getRGB()).substring(2) : "n/a", true)
                    .addField("Mention", role.getAsMention(), true)
                    .addField("Member Count", String.valueOf(role.getGuild().getMembersWithRoles(role).size()), true)
                    .addField("Misc Info",
                        String.format("Hoisted: %s\nMentionable: %s\nPosition: %d",
                            role.isHoisted(), role.isMentionable(), role.getPosition()), true)
                    .addField("Created ago", TimeUtils.dateAgo(role.getTimeCreated(), OffsetDateTime.now()), false)

                    .setFooter("ID: " + role.getId() + " | Created")
                    .setTimestamp(role.getTimeCreated())
                    .setColor(role.getColor());

            event.getChannel().sendMessage(eb.build()).queue();
        }
    }

    @SubCommand(RoleManagement.class)
    @CommandOptions(
        name = "add",
        description = "Adds a role to a user",
        usage = "<user> <role>",
        perm = CommandPerm.BOT_MANAGER,
        botPerms = Permission.MANAGE_ROLES
    )
    @Checks.Permissions.Guild(Permission.MANAGE_ROLES)
    private static class Add extends GuildCommand {

        @Override
        public @NotNull String name() {
            return "add";
        }

        @Override
        public void run(GuildCommandEvent event) {
            Check.check(event.getArgs().size() >= 2, CommandArgumentException::new);

            Guild g = event.getGuild();
            Member m = CacheUtils.getMember(g, event.getArgs().get(0));
            String rRef = lastArg(1, event);
            Role r = Parser.Role.getRole(g, rRef);

            Check.entityReferenceNotNull(m, Member.class, event.getArgs().get(0));
            Check.entityReferenceNotNull(r, Role.class, rRef);
            g.addRoleToMember(m, r).queue();

            event.addCheckmark();
            event.reply("Role Management", "Added %s to %s", m.getAsMention(),
                r.getAsMention()).queue(message -> message.delete().queueAfter(10, TimeUnit.SECONDS));
        }
    }

    @SubCommand(RoleManagement.class)
    @CommandOptions(
        name = "remove",
        description = "Removes a role from a user",
        usage = "<user> <role>",
        perm = CommandPerm.BOT_MANAGER,
        botPerms = Permission.MANAGE_ROLES
    )
    @Checks.Permissions.Guild(Permission.MANAGE_ROLES)
    private static class Remove extends GuildCommand {
        @Override
        public void run(GuildCommandEvent event) {
            Check.check(event.getArgs().size() >= 2, CommandArgumentException::new);

            Guild g = event.getGuild();
            Member m = CacheUtils.getMember(g, event.getArgs().get(0));
            String rRef = lastArg(1, event);
            Role r = Parser.Role.getRole(g, rRef);

            Check.entityReferenceNotNull(m, Member.class, event.getArgs().get(0));
            Check.entityReferenceNotNull(r, Role.class, rRef);
            Check.check(m.getRoles().contains(r), () -> new ReplyError("Member does not have that role"));
            g.removeRoleFromMember(m, r).queue();

            event.addCheckmark();
            event.reply("Role Management", "Removed %s from %s", m.getAsMention(),
                r.getAsMention()).queue(message -> message.delete().queueAfter(10, TimeUnit.SECONDS));
        }
    }

    @Checks.Permissions.Guild(Permission.MANAGE_ROLES)
    @SubCommand(RoleManagement.class)
    @CommandOptions(
        name = "create",
        description = "Create a role",
        usage = "<name> [color] [mentionable] [hoisted] [perms...]",
        perm = CommandPerm.BOT_ADMIN,
        botPerms = Permission.MANAGE_ROLES
    )

    private static class Create extends GuildCommand {
        @Override
        public void run(GuildCommandEvent event) {
            var args = event.getArgs();
            Check.check(!args.isEmpty(), CommandArgumentException::new);

            var name = args.get(0);
            var color = args.size() >= 2 ? getColor(args.get(1)) : new Color(0);
            var mentionable = args.size() >= 3 && args.get(2).matches("true|1");
            var hoisted = args.size() >= 4 && args.get(3).matches("true|1");
            var perms = args.size() > 4 ?
                            args.subList(4, args.size()).stream().map(s -> EnumSet.allOf(Permission.class).stream().filter(
                                permission -> permission.getName().equals(s)
                                                  || s.matches("\\d{1,5}")
                                                         && permission.getOffset() == Integer.parseInt(s)
                            ).findAny().orElse(null)).filter(Objects::nonNull).collect(Collectors.toSet())
                            : EnumSet.noneOf(Permission.class);

            var role = event.getGuild().createRole()
                           .setName(name)
                           .setColor(color)
                           .setMentionable(mentionable)
                           .setHoisted(hoisted).setPermissions(perms)
                           .complete();

            event.reply("Role Created", "Created role %s", role.getAsMention()).queue();
        }

        private static Color getColor(String s) {
            try {
                return new Color(Integer.decode(s));
            } catch (NumberFormatException n) {
                return new Color(0);
            }
        }
    }
}
