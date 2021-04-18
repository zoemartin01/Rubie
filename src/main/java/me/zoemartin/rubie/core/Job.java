package me.zoemartin.rubie.core;

import me.zoemartin.rubie.core.annotations.DatabaseEntity;
import me.zoemartin.rubie.core.interfaces.DatabaseEntry;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Map;
import java.util.UUID;

@Table(name = "jobs")
@Entity
@DatabaseEntity
public class Job implements DatabaseEntry {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
        name = "UUID",
        strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "id")
    private UUID id;

    @Column(name = "job_uuid")
    private UUID jobId;

    @Column(name = "timestamp_end")
    private long end;

    @MapKeyColumn(name = "key")
    @CollectionTable(name = "jobs_settings")
    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "value", columnDefinition = "TEXT")
    private Map<String, String> settings;

    public Job() {
    }

    public Job(UUID jobId, long end, Map<String, String> settings) {
        this.jobId = jobId;
        this.end = end;
        this.settings = settings;
    }

    public UUID getJobId() {
        return jobId;
    }

    public Map<String, String> getSettings() {
        return settings;
    }

    public long getEnd() {
        return end;
    }

    public static class CommonKeys {
        public final static String
            GUILD = "guild_id",
            USER = "user_id",
            CHANNEL = "channel_id";
    }
}
