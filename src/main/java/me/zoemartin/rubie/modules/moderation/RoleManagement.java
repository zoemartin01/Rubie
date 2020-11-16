package me.zoemartin.rubie.modules.moderation;

import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.annotations.*;
import me.zoemartin.rubie.core.exceptions.CommandArgumentException;
import me.zoemartin.rubie.core.exceptions.ReplyError;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.*;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

@Disabled
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
            embedReply(event, "Role Management", "Added %s to %s", m.getAsMention(),
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
            embedReply(event, "Role Management", "Removed %s from %s", m.getAsMention(),
                r.getAsMention()).queue(message -> message.delete().queueAfter(10, TimeUnit.SECONDS));
        }
    }
}
