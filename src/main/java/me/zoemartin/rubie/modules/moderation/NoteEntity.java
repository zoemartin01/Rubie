package me.zoemartin.rubie.modules.moderation;

import me.zoemartin.rubie.core.annotations.DatabaseEntity;
import me.zoemartin.rubie.core.interfaces.DatabaseEntry;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.persistence.*;
import java.util.UUID;

@DatabaseEntity
@Entity
@Table(name = "notes")
public class NoteEntity implements DatabaseEntry {

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
    private String note;

    @Column
    private long timestamp;

    public NoteEntity() {
    }

    public NoteEntity(String guild_id, String user_id, String moderator_id, String note, long timestamp) {
        this.uuid = UUID.randomUUID();
        this.guild_id = guild_id;
        this.user_id = user_id;
        this.moderator_id = moderator_id;
        this.note = note;
        this.timestamp = timestamp;
    }

    public NoteEntity(UUID uuid, String guild_id, String user_id, String moderator_id, String note, long timestamp) {
        this.uuid = uuid;
        this.guild_id = guild_id;
        this.user_id = user_id;
        this.moderator_id = moderator_id;
        this.note = note;
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        NoteEntity that = (NoteEntity) o;

        return new EqualsBuilder()
                   .append(timestamp, that.timestamp)
                   .append(guild_id, that.guild_id)
                   .append(user_id, that.user_id)
                   .append(moderator_id, that.moderator_id)
                   .append(note, that.note)
                   .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                   .append(guild_id)
                   .append(user_id)
                   .append(moderator_id)
                   .append(note)
                   .append(timestamp)
                   .toHashCode();
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

    public String getNote() {
        return note;
    }

    public long getTimestamp() {
        return timestamp;
    }
}

