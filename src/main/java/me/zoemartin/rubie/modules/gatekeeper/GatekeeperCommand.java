package me.zoemartin.rubie.modules.gatekeeper;

import me.zoemartin.rubie.core.*;
import me.zoemartin.rubie.core.annotations.*;
import me.zoemartin.rubie.core.exceptions.CommandArgumentException;
import me.zoemartin.rubie.core.exceptions.ReplyError;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.*;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.hibernate.Session;

import javax.persistence.criteria.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Command
@CommandOptions(
    name = "gatekeeper",
    description = "Gatekeeper Configuration",
    perm = CommandPerm.BOT_ADMIN
)
public class GatekeeperCommand extends GuildCommand {
    @Override
    public void run(GuildCommandEvent event) {
        throw new CommandArgumentException();
    }

    @SubCommand(GatekeeperCommand.class)
    @CommandOptions(
        name = "config",
        description = "Configure the Gatekeeper",
        perm = CommandPerm.BOT_ADMIN,
        alias = "conf"
    )
    private static class Config extends GuildCommand {
        @Override
        public void run(GuildCommandEvent event) {
            throw new CommandArgumentException();
        }

        @SubCommand(Config.class)
        @CommandOptions(
            name = "param",
            description = "Gatekeeper Parameter Configuration",
            perm = CommandPerm.BOT_ADMIN,
            alias = "parameter",
            usage = "[key] [value]"
        )
        static class Param extends AutoConfig<GatekeeperConfig> {
            @Override
            protected GatekeeperConfig supply(GuildCommandEvent event) {
                return Gatekeeper.getConfig(event.getGuild());
            }
        }

        @SubCommand(Config.class)
        @CommandOptions(
            name = "addroles",
            description = "Toggle roles given after successful verification",
            usage = "[role]",
            perm = CommandPerm.BOT_ADMIN,
            botPerms = Permission.MANAGE_ROLES
        )
        private static class AddRoles extends GuildCommand {
            @Override
            public void run(GuildCommandEvent event) {
                List<String> args = event.getArgs();
                GatekeeperConfig config = Gatekeeper.getConfig(event.getGuild());

                if (args.size() == 0) {
                    event.reply("Roles given after Verification", config.getRolesToAdd()
                                                                      .stream().map(s -> event.getGuild().getRoleById(s))
                                                                      .filter(Objects::nonNull).map(Role::getAsMention)
                                                                      .collect(Collectors.joining("\n"))).queue();
                    return;
                }

                boolean contains = false;
                String rRef = lastArg(0, event);
                if (config.getRolesToAdd().contains(rRef)) {
                    config.removeRolesToAdd(rRef);
                    contains = true;
                } else {
                    Role r = Parser.Role.getRole(event.getGuild(), rRef);
                    Check.entityReferenceNotNull(r, Role.class, rRef);

                    Check.check(event.getGuild().getSelfMember().canInteract(r),
                        () -> new ReplyError("I cannot assign this role!"));

                    config.addRolesToAdd(r.getId());
                }

                DatabaseUtil.updateObject(config);
                Role r = Parser.Role.getRole(event.getGuild(), rRef);
                if (contains) {
                    event.reply(null, "%s won't be awarded any longer after verification!",
                        r == null ? "`" + rRef + "`" : r.getAsMention()).queue();
                } else {
                    event.reply(null, "%s will now be awarded after verification!",
                        r == null ? "`" + rRef + "`" : r.getAsMention()).queue();
                }

            }
        }

        @SubCommand(Config.class)
        @CommandOptions(
            name = "removeroles",
            description = "Toggle roles removed after successful verification",
            usage = "[role]",
            perm = CommandPerm.BOT_ADMIN,
            botPerms = Permission.MANAGE_ROLES
        )
        private static class RemoveRoles extends GuildCommand {
            @Override
            public void run(GuildCommandEvent event) {
                List<String> args = event.getArgs();
                GatekeeperConfig config = Gatekeeper.getConfig(event.getGuild());

                if (args.size() == 0) {
                    event.reply("Roles removed after Verification", config.getRolesToRemove()
                                                                        .stream().map(s -> event.getGuild().getRoleById(s))
                                                                        .filter(Objects::nonNull).map(Role::getAsMention)
                                                                        .collect(Collectors.joining("\n"))).queue();
                    return;
                }

                boolean contains = false;
                String rRef = lastArg(0, event);
                if (config.getRolesToAdd().contains(rRef)) {
                    config.removeRolesToAdd(rRef);
                    contains = true;
                } else {
                    Role r = Parser.Role.getRole(event.getGuild(), rRef);
                    Check.entityReferenceNotNull(r, Role.class, rRef);
                    config.addRolesToAdd(r.getId());
                }

                DatabaseUtil.updateObject(config);
                Role r = Parser.Role.getRole(event.getGuild(), rRef);
                if (contains) {
                    event.reply(null, "%s won't be removed any longer after verification!",
                        r == null ? "`" + rRef + "`" : r.getAsMention()).queue();
                } else {
                    event.reply(null, "%s will now be removed after verification!",
                        r == null ? "`" + rRef + "`" : r.getAsMention()).queue();
                }
            }
        }
    }

    @SubCommand(GatekeeperCommand.class)
    @CommandOptions(
        name = "approve",
        description = "Manually pass a user through the gatekeeper.",
        perm = CommandPerm.BOT_MANAGER
    )
    private static class Approve extends GuildCommand {
        @Override
        public void run(GuildCommandEvent event) {
            Check.check(!event.getArgs().isEmpty(), CommandArgumentException::new);

            Member m = event.getGuild().getMemberById(event.getArgs().get(0));
            Check.entityReferenceNotNull(m, Member.class, event.getArgs().get(0));

            Session s = DatabaseUtil.getSessionFactory().openSession();
            CriteriaBuilder cb = s.getCriteriaBuilder();

            CriteriaQuery<VerificationEntity> q = cb.createQuery(VerificationEntity.class);
            Root<VerificationEntity> r = q.from(VerificationEntity.class);
            List<VerificationEntity> entities = s.createQuery(q.select(r).where(
                cb.equal(r.get("guild_id"), m.getGuild().getId()),
                cb.equal(r.get("user_id"), m.getId()))).getResultList();

            if (entities.isEmpty()) {
                VerificationEntity entity = new VerificationEntity(m);
                DatabaseUtil.saveObject(entity);
                Gatekeeper.verify(entity);
            } else {
                Gatekeeper.verify(entities.get(0));
            }

            event.addCheckmark();
            event.reply(null, "Approved %s", m.getAsMention()).queue();
        }
    }
}
