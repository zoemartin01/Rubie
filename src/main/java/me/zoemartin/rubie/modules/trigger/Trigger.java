package me.zoemartin.rubie.modules.trigger;

import com.google.auto.service.AutoService;
import me.zoemartin.rubie.core.interfaces.DatabaseEntry;

import javax.persistence.*;
import java.util.UUID;

@AutoService(DatabaseEntry.class)
@Entity
@Table(name = "trigger")
public class Trigger implements DatabaseEntry {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID uuid;

    @Column(name = "guild_id", updatable = false, nullable = false)
    private String guild_id;

    @Column(name = "regex", updatable = false, nullable = false)
    private String regex;

    @Column(name = "output", updatable = false, nullable = false)
    private String output;

    public Trigger(UUID uuid, String guild_id, String regex, String output) {
        this.uuid = uuid;
        this.guild_id = guild_id;
        this.regex = regex;
        this.output = output;
    }

    public Trigger(String guild_id, String regex, String output) {
        this.uuid = UUID.randomUUID();
        this.guild_id = guild_id;
        this.regex = regex;
        this.output = output;
    }

    public Trigger() {
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getGuild_id() {
        return guild_id;
    }

    public String getRegex() {
        return regex;
    }

    public String getOutput() {
        return output;
    }
}
