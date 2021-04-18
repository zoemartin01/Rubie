package me.zoemartin.rubie.modules.reactroles;

import me.zoemartin.rubie.core.annotations.DatabaseEntity;
import me.zoemartin.rubie.core.interfaces.DatabaseEntry;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "reactroles")
@DatabaseEntity
public class ReactRole implements DatabaseEntry {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
        name = "UUID",
        strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "id")
    private UUID id;

    @Column(name = "guild_id")
    private String guildId;

    @Column(name = "channel_id")
    private String channelId;

    @Column(name = "message_id")
    private String messageId;

    @Column(name = "role_id")
    private String roleId;

    @Column(name = "react")
    private String react;

    public ReactRole() {
    }

    public ReactRole(Message message, Role role, String react) {
        this.guildId = message.getGuild().getId();
        this.channelId = message.getChannel().getId();
        this.messageId = message.getId();
        this.roleId = role.getId();
        this.react = react;
    }

    public UUID getId() {
        return id;
    }

    public String getGuildId() {
        return guildId;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getRoleId() {
        return roleId;
    }

    public String getReact() {
        return react;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        ReactRole reactRole = (ReactRole) o;

        return new EqualsBuilder()
                   .append(guildId, reactRole.guildId)
                   .append(channelId, reactRole.channelId)
                   .append(messageId, reactRole.messageId)
                   .append(roleId, reactRole.roleId)
                   .append(react, reactRole.react)
                   .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                   .append(guildId)
                   .append(channelId)
                   .append(messageId)
                   .append(roleId)
                   .append(react)
                   .toHashCode();
    }
}
