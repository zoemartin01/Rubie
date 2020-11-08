package me.zoemartin.rubie.modules.moderation;

import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.exceptions.CommandArgumentException;
import me.zoemartin.rubie.core.exceptions.ReplyError;
import me.zoemartin.rubie.core.interfaces.Command;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.*;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class RoleManagement implements GuildCommand {
    @Override
    public @NotNull Set<Command> subCommands() {
        return Set.of(new Add(), new Remove());
    }

    @Override
    public @NotNull String name() {
        return "role";
    }

    @Override
    public void run(GuildCommandEvent event) {
        throw new CommandArgumentException();
    }

    @Override
    public @NotNull CommandPerm commandPerm() {
        return CommandPerm.BOT_MANAGER;
    }

    @Override
    public @NotNull Collection<Permission> required() {
        return Set.of(Permission.MANAGE_ROLES);
    }

    @Override
    public @NotNull String description() {
        return "Role Management";
    }

    private static class Add implements GuildCommand {

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
            embedReply(event, "Role Management", "Added %s to %s", m.getAsMention(),
                r.getAsMention()).queue(message -> message.delete().queueAfter(10, TimeUnit.SECONDS));
        }

        @Override
        public @NotNull CommandPerm commandPerm() {
            return CommandPerm.BOT_MANAGER;
        }

        @Override
        public @NotNull Collection<Permission> required() {
            return Set.of(Permission.MANAGE_ROLES);
        }

        @Override
        public @NotNull String usage() {
            return "<@user> <role>";
        }

        @Override
        public @NotNull String description() {
            return "Adds a role to a user";
        }
    }

    private static class Remove implements GuildCommand {

        @Override
        public @NotNull String name() {
            return "remove";
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
            Check.check(m.getRoles().contains(r), () -> new ReplyError("Member does not have that role"));
            g.removeRoleFromMember(m, r).queue();

            event.addCheckmark();
            embedReply(event, "Role Management", "Removed %s from %s", m.getAsMention(),
                r.getAsMention()).queue(message -> message.delete().queueAfter(10, TimeUnit.SECONDS));
        }

        @Override
        public @NotNull CommandPerm commandPerm() {
            return CommandPerm.BOT_MANAGER;
        }

        @Override
        public @NotNull Collection<Permission> required() {
            return Set.of(Permission.MANAGE_ROLES);
        }

        @Override
        public @NotNull String description() {
            return "Removes a role from a user";
        }
    }
}
