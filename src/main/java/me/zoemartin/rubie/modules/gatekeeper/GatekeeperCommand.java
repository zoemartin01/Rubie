package me.zoemartin.rubie.modules.gatekeeper;

import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.annotations.*;
import me.zoemartin.rubie.core.exceptions.CommandArgumentException;
import me.zoemartin.rubie.core.exceptions.ReplyError;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.*;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.hibernate.Session;

import javax.persistence.criteria.*;
import java.util.*;
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
        usage = "[key] [value]"
    )
    private static class Config extends GuildCommand {
        private static final Map<String, Class<?>> keys = Map.of(
            "enabled", Boolean.class,
            "timeout_enabled", Boolean.class,
            "logging_enabled", Boolean.class,
            "message_on_verification", Boolean.class,
            "add_roles", Boolean.class,
            "remove_roles", Boolean.class,
            "log_channel", TextChannel.class,
            "timeout", Long.class,
            "trigger", String.class,
            "mode", GatekeeperConfig.Mode.class
        );


        @Override
        public void run(GuildCommandEvent event) {
            if (event.getArgs().size() == 0) {
                event.reply("Valid Config Keys", keys.entrySet().stream().map(e -> String.format("`%s` - `%s`",
                    e.getKey(), e.getValue().getSimpleName())).collect(Collectors.joining("\n"))).queue();
                return;
            }

            String key = event.getArgs().get(0);
            Check.check(keys.containsKey(key), () -> new ReplyError("Not a valid config key!"));
            GatekeeperConfig config = Gatekeeper.getConfig(event.getGuild());


            if (event.getArgs().size() == 1) {
                Object value;

                switch (key) {
                    case "enabled" -> value = config.isGatekeeperEnabled();
                    case "timeout_enabled" -> value = config.isKickEnabled();
                    case "logging_enabled" -> value = config.isLoggingEnabled();
                    case "message_on_verification" -> value = config.doSendMessage();
                    case "add_roles" -> value = config.doAddRoles();
                    case "remove_roles" -> value = config.doRemoveRoles();
                    case "log_channel" -> value = config.getLogChannelId() == null ? null : event.getGuild().getTextChannelById(config.getLogChannelId());
                    case "timeout" -> value = config.getKickAfter();
                    case "trigger" -> value = config.getVerificationTrigger();
                    case "mode" -> value = config.getGatekeeperMode();
                    default -> value = null;
                }

                event.reply(null, "Key `%s` is set to `%s`", key, value).queue();
                return;
            }

            Object value;

            String s = event.getArgs().get(1);
            switch (key) {
                case "enabled", "timeout_enabled", "logging_enabled", "message_on_verification", "add_roles", "remove_roles" -> value = Boolean.parseBoolean(s);
                case "log_channel" -> value = Parser.Channel.getTextChannel(event.getGuild(), s);
                case "timeout" -> value = Long.parseLong(s);
                case "trigger" -> value = s;
                case "mode" -> value = GatekeeperConfig.Mode.fromName(s);
                default -> value = null;
            }

            Check.check(value != null /*&& value.getClass() == keys.get(key)*/,
                () -> new ReplyError("Argument invalid!"));

            switch (key) {
                case "enabled" -> config.setGatekeeperEnabled((Boolean) value);
                case "timeout_enabled" -> config.setKickEnabled((Boolean) value);
                case "logging_enabled" -> config.setLogsEnabled((Boolean) value);
                case "message_on_verification" -> config.setSendMessage((Boolean) value);
                case "add_roles" -> config.setAddRoles((Boolean) value);
                case "remove_roles" -> config.setRemoveRoles((Boolean) value);
                case "log_channel" -> config.setLogChannelId(((TextChannel) value).getId());
                case "timeout" -> config.setKickAfter((Long) value);
                case "trigger" -> config.setVerificationTrigger((String) value);
                case "mode" -> config.setGatekeeperMode((GatekeeperConfig.Mode) value);
            }

            DatabaseUtil.updateObject(config);
            event.addCheckmark();
            event.reply(null, "Updated config key `%s` to `%s`", key, value).queue();
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
