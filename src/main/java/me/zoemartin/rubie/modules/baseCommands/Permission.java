package me.zoemartin.rubie.modules.baseCommands;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.exceptions.CommandArgumentException;
import me.zoemartin.rubie.core.exceptions.ReplyError;
import me.zoemartin.rubie.core.interfaces.Command;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.*;
import me.zoemartin.rubie.modules.commandProcessing.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class Permission implements GuildCommand {
    @Override
    public @NotNull Set<Command> subCommands() {
        return Set.of(new MemberPerm(), new RolePerm());
    }

    @Override
    public @NotNull String name() {
        return "permission";
    }

    @Override
    public @NotNull String regex() {
        return "permission|perm";
    }

    @Override
    public void run(GuildCommandEvent event) {
        throw new CommandArgumentException();
    }

    @Override
    public @NotNull CommandPerm commandPerm() {
        return CommandPerm.BOT_ADMIN;
    }

    @Override
    public @NotNull String usage() {
        return "help";
    }

    @Override
    public @NotNull String description() {
        return "Bot Permission Management";
    }

    private static class MemberPerm implements GuildCommand {
        @Override
        public @NotNull Set<Command> subCommands() {
            return Set.of(new set(), new Remove(), new list());
        }

        @Override
        public @NotNull String name() {
            return "member";
        }

        @Override
        public @NotNull String regex() {
            return "member|m";
        }

        @Override
        public void run(GuildCommandEvent event) {
            throw new CommandArgumentException();
        }

        @Override
        public @NotNull CommandPerm commandPerm() {
            return CommandPerm.BOT_ADMIN;
        }

        @Override
        public @NotNull String description() {
            return "Member Permission Management";
        }

        private static class set implements GuildCommand {
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

            @Override
            public @NotNull CommandPerm commandPerm() {
                return CommandPerm.BOT_ADMIN;
            }

            @Override
            public @NotNull String usage() {
                return "<user> <level>";
            }

            @Override
            public @NotNull String description() {
                return "Adds Bot Permissions to a Member";
            }
        }

        private static class Remove implements GuildCommand {
            @Override
            public @NotNull String name() {
                return "remove";
            }

            @Override
            public void run(GuildCommandEvent event) {
                Check.check(event.getArgs().size() == 1 && Parser.User.isParsable(event.getArgs().get(0)),
                    CommandArgumentException::new);

                Member m = CacheUtils.getMemberExplicit(event.getGuild(), Parser.User.parse(event.getArgs().get(0)));

                Check.check(PermissionHandler.removeMemberPerm(event.getGuild().getId(), m.getId()),
                    () -> new ReplyError("Error, Member does not have an assigned Member Permission"));

                embedReply(event, (String) null, "Removed Member Permission from %s", m.getAsMention()).queue();
            }

            @Override
            public @NotNull CommandPerm commandPerm() {
                return CommandPerm.BOT_ADMIN;
            }

            @Override
            public @NotNull String usage() {
                return "<user>";
            }

            @Override
            public @NotNull String description() {
                return "Removes Bot Permissions from a Member";
            }
        }

        private static class list implements GuildCommand {
            @Override
            public @NotNull String name() {
                return "list";
            }

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

            @Override
            public @NotNull CommandPerm commandPerm() {
                return CommandPerm.BOT_ADMIN;
            }

            @Override
            public @NotNull String description() {
                return "Lists all members with special bot member permissions";
            }
        }
    }

    private static class RolePerm implements GuildCommand {
        @Override
        public @NotNull Set<Command> subCommands() {
            return Set.of(new set(), new Remove(), new list());
        }

        @Override
        public @NotNull String name() {
            return "role";
        }

        @Override
        public @NotNull String regex() {
            return "role|r";
        }

        @Override
        public void run(GuildCommandEvent event) {
            throw new CommandArgumentException();
        }

        @Override
        public @NotNull CommandPerm commandPerm() {
            return CommandPerm.BOT_ADMIN;
        }

        @Override
        public @NotNull String description() {
            return "Role Permission Management";
        }

        private static class set implements GuildCommand {
            @Override
            public @NotNull String name() {
                return "set";
            }

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

            @Override
            public @NotNull CommandPerm commandPerm() {
                return CommandPerm.BOT_ADMIN;
            }

            @Override
            public @NotNull String usage() {
                return "<role> <level>";
            }

            @Override
            public @NotNull String description() {
                return "Adds Bot Permissions to a Role";
            }
        }

        private static class Remove implements GuildCommand {
            @Override
            public @NotNull String name() {
                return "remove";
            }

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

            @Override
            public @NotNull CommandPerm commandPerm() {
                return CommandPerm.BOT_ADMIN;
            }

            @Override
            public @NotNull String usage() {
                return "<role>";
            }

            @Override
            public @NotNull String description() {
                return "Remove Bot Permissions from a Role";
            }
        }

        private static class list implements GuildCommand {
            @Override
            public @NotNull String name() {
                return "list";
            }

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

            @Override
            public @NotNull CommandPerm commandPerm() {
                return CommandPerm.BOT_ADMIN;
            }

            @Override
            public @NotNull String description() {
                return "Lists all roles with special bot role permissions";
            }
        }
    }
}
