package me.zoemartin.rubie.modules.gatekeeper;

import me.zoemartin.rubie.core.annotations.DatabaseEntity;
import me.zoemartin.rubie.core.interfaces.DatabaseEntry;
import net.dv8tion.jda.api.entities.Member;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.Instant;
import java.util.UUID;

@DatabaseEntity
@Entity
@Table(name = "verification_keys")
public class VerificationEntity implements DatabaseEntry {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
        name = "UUID",
        strategy = "org.hibernate.id.UUIDGenerator"
    )
    private UUID key;

    @Column
    private String guild_id;

    @Column
    private String user_id;

    @Column
    private long timestamp;

    @Column
    private boolean verified;

    public VerificationEntity() {

    }

    public VerificationEntity(Member member) {
        this.guild_id = member.getGuild().getId();
        this.user_id = member.getId();
        this.timestamp = Instant.now().toEpochMilli();
        this.verified = false;
    }

    public UUID getKey() {
        return key;
    }

    public String getGuild_id() {
        return guild_id;
    }

    public String getUser_id() {
        return user_id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isVerified() {
        return verified;
    }

    @Override
    public String toString() {
        return "VerificationEntity{" +
                   "key=" + key +
                   ", guild_id='" + guild_id + '\'' +
                   ", user_id='" + user_id + '\'' +
                   ", timestamp=" + timestamp +
                   ", verified=" + verified +
                   '}';
    }
}
