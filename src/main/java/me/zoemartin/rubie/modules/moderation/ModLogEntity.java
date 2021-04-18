package me.zoemartin.rubie.modules.moderation;

import me.zoemartin.rubie.core.annotations.DatabaseEntity;
import me.zoemartin.rubie.core.interfaces.DatabaseEntry;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.persistence.*;
import java.util.Set;
import java.util.UUID;

@DatabaseEntity
@Entity
@Table(name = "modactions")
public class ModLogEntity implements DatabaseEntry {

    @Id
    @GeneratedValue
    @Column
    private UUID uuid;

    @Column
    private String guild_id;

    @Column
    private String user_id;

    @Column
    private String moderator_id;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column
    private long timestamp;

    @Column
    @Convert(converter = ModLogType.Converter.class)
    private ModLogType type;

    public ModLogEntity() {
    }

    public ModLogEntity(String guild_id, String user_id, String moderator_id, String reason, long timestamp, ModLogType type) {
        this.uuid = UUID.randomUUID();
        this.guild_id = guild_id;
        this.user_id = user_id;
        this.moderator_id = moderator_id;
        this.reason = reason;
        this.timestamp = timestamp;
        this.type = type;
    }

    public ModLogEntity(UUID uuid, String guild_id, String user_id, String moderator_id, String reason, long timestamp, ModLogType type) {
        this.uuid = uuid;
        this.guild_id = guild_id;
        this.user_id = user_id;
        this.moderator_id = moderator_id;
        this.reason = reason;
        this.timestamp = timestamp;
        this.type = type;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getGuild_id() {
        return guild_id;
    }

    public String getUser_id() {
        return user_id;
    }

    public String getModerator_id() {
        return moderator_id;
    }

    public String getReason() {
        return reason;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public ModLogType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        ModLogEntity that = (ModLogEntity) o;

        return new EqualsBuilder()
                   .append(timestamp, that.timestamp)
                   .append(guild_id, that.guild_id)
                   .append(user_id, that.user_id)
                   .append(moderator_id, that.moderator_id)
                   .append(reason, that.reason)
                   .append(type, that.type)
                   .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                   .append(guild_id)
                   .append(user_id)
                   .append(moderator_id)
                   .append(reason)
                   .append(timestamp)
                   .append(type)
                   .toHashCode();
    }

    enum ModLogType {
        NONE(-1), WARN(0), MUTE(1), UNMUTE(2), KICK(3), BAN(4), UNBAN(5);

        private final int raw;

        ModLogType(int i) {
            raw = i;
        }

        public int raw() {
            return raw;
        }

        public static ModLogType fromNum(Integer num) {
            if (num == null) return null;

            return Set.of(ModLogType.values()).stream().filter(a -> num.equals(a.raw())).findAny()
                       .orElse(null);
        }

        private static class Converter implements AttributeConverter<ModLogType, Integer> {
            @Override
            public Integer convertToDatabaseColumn(ModLogType attribute) {
                return attribute.raw();
            }

            @Override
            public ModLogType convertToEntityAttribute(Integer dbData) {
                return ModLogType.fromNum(dbData);
            }
        }
    }
}
