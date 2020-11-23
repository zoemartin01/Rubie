package me.zoemartin.rubie.modules.gatekeeper;

import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.annotations.Mapped;
import me.zoemartin.rubie.core.util.DatabaseConverter;
import net.dv8tion.jda.api.entities.Guild;

import javax.persistence.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mapped
@Table(name = "gatekeeper_configs")
@Entity
public class GatekeeperConfig {
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

    @Column(name = "timeout_ignore_roles", columnDefinition="TEXT")
    @Convert(converter = DatabaseConverter.StringListConverter.class)
    private Collection<String> kickIgnoredRoles;

    @Column(name = "enable_roles_add")
    boolean addRoles;

    @Column(name = "enable_roles_remove")
    boolean removeRoles;

    @Column(name = "roles_add", columnDefinition="TEXT")
    @Convert(converter = DatabaseConverter.StringListConverter.class)
    private Collection<String> rolesToAdd;

    @Column(name = "roles_remove", columnDefinition="TEXT")
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

    protected GatekeeperConfig() {
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

    public boolean isGatekeeperEnabled() {
        return gatekeeperEnabled;
    }

    public boolean isKickEnabled() {
        return kickEnabled;
    }

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

    public String getVerificationTrigger() {
        return verificationTrigger;
    }

    public boolean isLoggingEnabled() {
        return logsEnabled;
    }

    public String getLogChannelId() {
        return logChannelId;
    }

    public Mode getGatekeeperMode() {
        return gatekeeperMode;
    }

    public boolean doSendMessage() {
        return sendMessage;
    }

    public boolean doAddRoles() {
        return addRoles;
    }

    public boolean doRemoveRoles() {
        return removeRoles;
    }

    public void setGuildId(String guildId) {
        this.guildId = guildId;
    }

    public void setGatekeeperEnabled(boolean gatekeeperEnabled) {
        this.gatekeeperEnabled = gatekeeperEnabled;
    }

    public void setKickEnabled(boolean kickEnabled) {
        this.kickEnabled = kickEnabled;
    }

    public void setKickAfter(long kickAfter) {
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

    public void setVerificationTrigger(String verificationTrigger) {
        this.verificationTrigger = verificationTrigger;
    }

    public void setLogsEnabled(boolean logsEnabled) {
        this.logsEnabled = logsEnabled;
    }

    public void setLogChannelId(String logChannelId) {
        this.logChannelId = logChannelId;
    }

    public void setGatekeeperMode(Mode gatekeeperMode) {
        this.gatekeeperMode = gatekeeperMode;
    }

    public void setSendMessage(boolean sendMessage) {
        this.sendMessage = sendMessage;
    }

    public void setAddRoles(boolean addRoles) {
        this.addRoles = addRoles;
    }

    public void setRemoveRoles(boolean removeRoles) {
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
