package me.zoemartin.rubie.modules.gatekeeper;

import me.zoemartin.rubie.core.annotations.*;
import me.zoemartin.rubie.core.interfaces.DatabaseEntry;
import me.zoemartin.rubie.core.util.DatabaseConverter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;

import javax.persistence.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@DatabaseEntity
@Table(name = "gatekeeper_configs")
@Entity
public class GatekeeperConfig implements DatabaseEntry {
    private static final String DEFAULT_TRIGGER = ";agree";

    @Id
    @Column(name = "guild_id")
    private String guildId;

    @Column(name = "enabled")
    boolean gatekeeperEnabled;

    @Column(name = "enable_gatekeeper_timeout")
    boolean kickEnabled;

    @Column(name = "timeout")
    long kickAfter;

    @Column(name = "timeout_ignore_roles", columnDefinition = "TEXT")
    @Convert(converter = DatabaseConverter.StringListConverter.class)
    private Collection<String> kickIgnoredRoles;

    @Column(name = "enable_roles_add")
    boolean addRoles;

    @Column(name = "enable_roles_remove")
    boolean removeRoles;

    @Column(name = "roles_add", columnDefinition = "TEXT")
    @Convert(converter = DatabaseConverter.StringListConverter.class)
    private Collection<String> rolesToAdd;

    @Column(name = "roles_remove", columnDefinition = "TEXT")
    @Convert(converter = DatabaseConverter.StringListConverter.class)
    private Collection<String> rolesToRemove;

    @Column(name = "trigger")
    private String verificationTrigger;

    @Column(name = "enable_logging")
    private boolean logsEnabled;

    @Column(name = "log_channel")
    private String logChannelId;

    @Column(name = "gatekeeper_mode")
    @Convert(converter = Mode.Converter.class)
    private Mode gatekeeperMode;

    @Column(name = "message_on_verification")
    private boolean sendMessage;

    public GatekeeperConfig() {
    }

    public GatekeeperConfig(Guild g) {
        this.guildId = g.getId();
        gatekeeperEnabled = false;
        kickEnabled = false;
        kickAfter = 0;
        kickIgnoredRoles = Collections.newSetFromMap(new ConcurrentHashMap<>());
        addRoles = false;
        removeRoles = false;
        rolesToAdd = Collections.newSetFromMap(new ConcurrentHashMap<>());
        rolesToRemove = Collections.newSetFromMap(new ConcurrentHashMap<>());
        logChannelId = null;
        verificationTrigger = DEFAULT_TRIGGER;
        gatekeeperMode = Mode.TRIGGER;
        sendMessage = true;
    }

    public String getGuildId() {
        return guildId;
    }

    @Getter("enabled")
    public boolean isGatekeeperEnabled() {
        return gatekeeperEnabled;
    }

    @Getter("timeout_enabled")
    public boolean isKickEnabled() {
        return kickEnabled;
    }

    @Getter("timeout_ms")
    public long getKickAfter() {
        return kickAfter;
    }

    public Collection<String> getKickIgnoredRoles() {
        return kickIgnoredRoles;
    }

    public Collection<String> getRolesToAdd() {
        return rolesToAdd;
    }

    public Collection<String> getRolesToRemove() {
        return rolesToRemove;
    }

    @Getter("verification_trigger")
    public String getVerificationTrigger() {
        return verificationTrigger;
    }

    @Getter("logs_enabled")
    public boolean isLoggingEnabled() {
        return logsEnabled;
    }

    @Getter("logs_channel")
    public String getLogChannelId() {
        return logChannelId;
    }

    @Getter("mode")
    public Mode getGatekeeperMode() {
        return gatekeeperMode;
    }

    @Getter("send_private_verification_message")
    public boolean doSendMessage() {
        return sendMessage;
    }

    @Getter("adds_roles")
    public boolean doAddRoles() {
        return addRoles;
    }

    @Getter("removed_roles")
    public boolean doRemoveRoles() {
        return removeRoles;
    }

    @Setter(name = "enabled")
    public void setGatekeeperEnabled(Boolean gatekeeperEnabled) {
        this.gatekeeperEnabled = gatekeeperEnabled;
    }

    @Setter(name = "timeout_enabled")
    public void setKickEnabled(Boolean kickEnabled) {
        this.kickEnabled = kickEnabled;
    }

    @Setter(name = "timeout_ms", type = "Time in ms")
    public void setKickAfter(Long kickAfter) {
        this.kickAfter = kickAfter;
    }

    public boolean addKickIgnoredRoles(String roleId) {
        return kickIgnoredRoles.add(roleId);
    }

    public boolean removeKickIgnoredRoles(String roleId) {
        return kickIgnoredRoles.remove(roleId);
    }

    public boolean addRolesToAdd(String roleId) {
        return rolesToAdd.add(roleId);
    }

    public boolean removeRolesToAdd(String roleId) {
        return rolesToAdd.remove(roleId);
    }

    public boolean addRolesToRemove(String roleId) {
        return rolesToRemove.add(roleId);
    }

    public boolean removeRolesToRemove(String roleId) {
        return rolesToRemove.remove(roleId);
    }

    @Setter(name = "verification_trigger")
    public void setVerificationTrigger(String verificationTrigger) {
        this.verificationTrigger = verificationTrigger;
    }

    @Setter(name = "logs_enabled")
    public void setLogsEnabled(Boolean logsEnabled) {
        this.logsEnabled = logsEnabled;
    }

    @Setter(name = "logs_channel")
    public void setLogChannelId(TextChannel channel) {
        this.logChannelId = channel.getId();
    }

    @Setter(name = "mode", type = "on join/on trigger")
    public void setGatekeeperMode(Mode gatekeeperMode) {
        this.gatekeeperMode = gatekeeperMode;
    }

    @Setter(name = "send_private_verification_message")
    public void setSendMessage(Boolean sendMessage) {
        this.sendMessage = sendMessage;
    }

    @Setter(name = "adds_roles")
    public void setAddRoles(Boolean addRoles) {
        this.addRoles = addRoles;
    }

    @Setter(name = "removed_roles")
    public void setRemoveRoles(Boolean removeRoles) {
        this.removeRoles = removeRoles;
    }

    enum Mode {
        JOIN,
        TRIGGER;

        static Mode fromName(String s) {
            if (s == null) return null;

            return EnumSet.allOf(Mode.class).stream()
                       .filter(e -> s.toLowerCase().equals(e.name().toLowerCase()))
                       .findAny().orElse(null);
        }

        public static class Converter implements AttributeConverter<Mode, String> {
            @Override
            public String convertToDatabaseColumn(Mode attribute) {
                return attribute.name();
            }

            @Override
            public Mode convertToEntityAttribute(String dbData) {
                return Mode.fromName(dbData);
            }
        }
    }
}
