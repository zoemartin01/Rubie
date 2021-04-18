package me.zoemartin.rubie.modules.embeds.pinnedEmbeds;


import me.zoemartin.rubie.core.annotations.DatabaseEntity;
import me.zoemartin.rubie.core.interfaces.DatabaseEntry;

import javax.persistence.*;
import java.util.UUID;

@DatabaseEntity
@Entity
@Table(name = "pinned_embeds")
public class PineEntity implements DatabaseEntry {

    @Id
    @Column(updatable = false, nullable = false)
    @GeneratedValue
    private UUID uuid;

    @Column(updatable = false, nullable = false)
    private String guild_id;

    @Column(updatable = false, nullable = false)
    private String channel_id;

    @Column(updatable = false, nullable = false)
    private String message_id;

    @Column(updatable = false, nullable = false, columnDefinition = "TEXT")
    private String source_url;

    public PineEntity(String guild_id, String channel_id, String message_id, String source_url) {
        this.uuid = UUID.randomUUID();
        this.guild_id = guild_id;
        this.channel_id = channel_id;
        this.message_id = message_id;
        this.source_url = source_url;
    }

    public PineEntity(UUID uuid, String guild_id, String channel_id, String message_id, String source_url) {
        this.uuid = uuid;
        this.guild_id = guild_id;
        this.channel_id = channel_id;
        this.message_id = message_id;
        this.source_url = source_url;
    }

    public PineEntity() {

    }

    public String getGuild_id() {
        return guild_id;
    }

    public String getChannel_id() {
        return channel_id;
    }

    public String getMessage_id() {
        return message_id;
    }

    public String getSource_url() {
        return source_url;
    }
}
