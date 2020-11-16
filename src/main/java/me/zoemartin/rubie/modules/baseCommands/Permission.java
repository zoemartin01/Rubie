package me.zoemartin.rubie.modules.baseCommands;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.annotations.*;
import me.zoemartin.rubie.core.exceptions.CommandArgumentException;
import me.zoemartin.rubie.core.exceptions.ReplyError;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.*;
import me.zoemartin.rubie.modules.commandProcessing.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

@Command
@CommandOptions(
    name = "permission",
    description = "Bot Permission Management",
    perm = CommandPerm.BOT_ADMIN,
    alias = "perm"
)
@Checks.Permissions.Guild(net.dv8tion.jda.api.Permission.MANAGE_ROLES)
public class Permission extends GuildCommand {
    @Override
    public void run(GuildCommandEvent event) {
        throw new CommandArgumentException();
    }

    @SubCommand(Permission.class)
    @CommandOptions(
        name = "member",
        description = "Member Permission Management",
        perm = CommandPerm.BOT_ADMIN,
        alias = "m"
    )
    @Checks.Permissions.Guild(net.dv8tion.jda.api.Permission.MANAGE_ROLES)
    @SubCommand.AsBase(name = "memberperm", alias = "memberperms")
    public static class MemberPerm extends GuildCommand {
        @Override
        public void run(GuildCommandEvent event) {
            throw new CommandArgumentException();
        }

        @SubCommand(MemberPerm.class)
        @CommandOptions(
            name = "set",
            description = "Sets a Members Bot Permission",
            usage = "<user> <level>"
        )
        @Checks.Permissions.Guild(net.dv8tion.jda.api.Permission.MANAGE_ROLES)
        public static class set extends GuildCommand {
            @Override
            public @NotNull String name() {
                return "set";
            }

            @Override
            public void run(GuildCommandEvent event) {
                List<String> args = event.getArgs();
                Check.check(args.size() == 2 && Parser.User.isParsable(args.get(0))
                                && Parser.Int.isParsable(args.get(1)), CommandArgumentException::new);

                Member m = CacheUtils.getMemberExplicit(event.getGuild(), Parser.User.parse(args.get(0)));
                CommandPerm cp = CommandPerm.fromNum(Parser.Int.parse(args.get(1)));
                Check.notNull(cp, CommandArgumentException::new);
                Check.check(!cp.equals(CommandPerm.OWNER) || event.getUser().getId().equals(Bot.getOWNER()),
                    CommandArgumentException::new);

                if (cp.equals(CommandPerm.EVERYONE))
                    PermissionHandler.removeMemberPerm(event.getGuild().getId(), m.getId());
                else
                    PermissionHandler.addMemberPerm(event.getGuild().getId(), m.getId(), cp);
                embedReply(event, null, "Set `[%d] %s` to %s", cp.raw(), cp.toString(),
                    m.getAsMention()).queue();
            }
        }

        @SubCommand(MemberPerm.class)
        @CommandOptions(
            name = "remove",
            description = "Removes a Members Bot Permission",
            usage = "<user>",
            alias = {"rm", "delete", "del"}
        )
        @Checks.Permissions.Guild(net.dv8tion.jda.api.Permission.MANAGE_ROLES)
        public static class Remove extends GuildCommand {
            @Override
            public void run(GuildCommandEvent event) {
                Check.check(event.getArgs().size() == 1 && Parser.User.isParsable(event.getArgs().get(0)),
                    CommandArgumentException::new);

                Member m = CacheUtils.getMemberExplicit(event.getGuild(), Parser.User.parse(event.getArgs().get(0)));

                Check.check(PermissionHandler.removeMemberPerm(event.getGuild().getId(), m.getId()),
                    () -> new ReplyError("Error, Member does not have an assigned Member Permission"));

                embedReply(event, (String) null, "Removed Member Permission from %s", m.getAsMention()).queue();
            }
        }

        @SubCommand(MemberPerm.class)
        @CommandOptions(
            name = "list",
            description = "Lists all members with special bot member permissions"
        )
        public static class list extends GuildCommand {
            @Override
            public void run(GuildCommandEvent event) {
                Check.check(event.getArgs().isEmpty(), CommandArgumentException::new);

                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(event.getGuild().getSelfMember().getColor());
                eb.setTitle("Member Permission List");

                String list = PermissionHandler.getMemberPerms(
                    event.getGuild().getId())
                                  .stream().sorted(Comparator.comparingInt((MemberPermission o) -> o.getPerm().raw()).reversed())
                                  .map(
                                      mp -> "`[" + mp.getPerm().raw() + "] " + mp.getPerm().toString() + "` " +
                                                CacheUtils.getMemberExplicit(event.getGuild(), mp.getMember_id())
                                                    .getAsMention()
                                  ).collect(Collectors.joining("\n"));

                eb.setDescription(list);
                Check.check(!list.isEmpty(), () -> new ReplyError("No bot member permission overrides"));

                event.getChannel().sendMessage(eb.build()).queue();
            }
        }
    }

    @SubCommand(Permission.class)
    @CommandOptions(
        name = "role",
        description = "Role Permission Management",
        perm = CommandPerm.BOT_ADMIN,
        alias = "r"
    )
    @Checks.Permissions.Guild(net.dv8tion.jda.api.Permission.MANAGE_ROLES)
    @SubCommand.AsBase(name = "roleperm", alias = "roleperms")
    public static class RolePerm extends GuildCommand {
        @Override
        public void run(GuildCommandEvent event) {
            throw new CommandArgumentException();
        }

        @SubCommand(RolePerm.class)
        @CommandOptions(
            name = "set",
            description = "Sets a Roles Bot Permission",
            usage = "<role> <level>"
        )
        @Checks.Permissions.Guild(net.dv8tion.jda.api.Permission.MANAGE_ROLES)
        public static class set extends GuildCommand {

            @Override
            public void run(GuildCommandEvent event) {
                Check.check(event.getArgs().size() == 2 && Parser.Int.isParsable(event.getArgs().get(1)),
                    CommandArgumentException::new);

                Role r = Parser.Role.getRole(event.getGuild(), event.getArgs().get(0));
                CommandPerm cp = CommandPerm.fromNum(Parser.Int.parse(event.getArgs().get(1)));
                Check.notNull(cp, CommandArgumentException::new);
                Check.entityReferenceNotNull(r, Role.class, event.getArgs().get(0));
                Check.check(!cp.equals(CommandPerm.OWNER) || event.getUser().getId().equals(Bot.getOWNER()),
                    CommandArgumentException::new);

                if (cp.equals(CommandPerm.EVERYONE))
                    PermissionHandler.removeRolePerm(event.getGuild().getId(), r.getId());
                else
                    PermissionHandler.addRolePerm(event.getGuild().getId(), r.getId(), cp);
                embedReply(event, null, "Set `[%d] %s` to %s", cp.raw(), cp.toString(),
                    r.getAsMention()).queue();
            }
        }

        @SubCommand(RolePerm.class)
        @CommandOptions(
            name = "remove",
            description = "Removes a Roles Bot Permission",
            usage = "<role>",
            alias = {"rm", "delete", "del"}
        )
        public static class Remove extends GuildCommand {
            @Override
            public void run(GuildCommandEvent event) {
                Check.check(!event.getArgs().isEmpty(), CommandArgumentException::new);

                String rRef = lastArg(0, event);
                Role r = Parser.Role.getRole(event.getGuild(), rRef);
                Check.entityReferenceNotNull(r, Role.class, rRef);

                Check.check(PermissionHandler.removeRolePerm(event.getGuild().getId(), r.getId()),
                    () -> new ReplyError("Error, Role does not have an assigned Role Permission"));

                embedReply(event, (String) null, "Removed Role Permission from %s", r.getAsMention()).queue();
            }
        }

        @SubCommand(RolePerm.class)
        @CommandOptions(
            name = "list",
            description = "Lists all roles with special bot role permissions"
        )
        public static class list extends GuildCommand {
            @Override
            public void run(GuildCommandEvent event) {
                Check.check(event.getArgs().isEmpty(), CommandArgumentException::new);

                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(event.getGuild().getSelfMember().getColor());
                eb.setTitle("Role Permission List");

                String list = PermissionHandler.getRolePerms(
                    event.getGuild().getId())
                                  .stream().sorted(Comparator.comparingInt((RolePermission r) -> r.getPerm().raw()).reversed())
                                  .map(
                                      rp -> {
                                          Role r = event.getGuild().getRoleById(rp.getRole_id());
                                          return "`[" + rp.getPerm().raw() + "] " + rp.getPerm().toString() + "` " +
                                                     (r == null ? "" : r.getAsMention());
                                      }
                                  ).collect(Collectors.joining("\n"));

                eb.setDescription(list);
                Check.check(!list.isEmpty(), () -> new ReplyError("No bot role permission overrides"));

                event.getChannel().sendMessage(eb.build()).queue();
            }
        }
    }
}
