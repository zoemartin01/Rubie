package me.zoemartin.rubie.modules.embeds.triggerEmbeds;

import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.annotations.DatabaseEntity;
import me.zoemartin.rubie.core.interfaces.DatabaseEntry;

import javax.persistence.*;
import java.util.UUID;

@DatabaseEntity
@Entity
@Table(name = "trigger_embeds")
public class Tee implements DatabaseEntry {
    @Id
    @Column(updatable = false, nullable = false)
    @GeneratedValue
    private UUID uuid;

    @Column(updatable = false, nullable = false)
    private String guild_id;

    @Column(updatable = false, nullable = false)
    private String trigger;

    @Column(updatable = false, nullable = false, columnDefinition = "TEXT")
    private String source_url;

    @Column(columnDefinition = "TEXT")
    private String cached_json;

    @Column(nullable = false)
    @Convert(converter = CommandPerm.Converter.class)
    private CommandPerm perm;

    /*@ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "trigger_embed_aliases")
    private Set<String> rewardRoles;*/

    public Tee(UUID uuid, String guild_id, String trigger, String source_url, String cached_json, CommandPerm perm) {
        this.uuid = uuid;
        this.guild_id = guild_id;
        this.trigger = trigger;
        this.source_url = source_url;
        this.cached_json = cached_json;
        this.perm = perm;
    }

    public Tee(String guild_id, String trigger, String source_url, String cached_json, CommandPerm perm) {
        this.uuid = UUID.randomUUID();
        this.guild_id = guild_id;
        this.trigger = trigger;
        this.source_url = source_url;
        this.cached_json = cached_json;
        this.perm = perm;
    }

    public Tee(String guild_id, String trigger, String source_url, String cached_json) {
        this.uuid = UUID.randomUUID();
        this.guild_id = guild_id;
        this.trigger = trigger;
        this.source_url = source_url;
        this.cached_json = cached_json;
        this.perm = CommandPerm.EVERYONE;
    }

    public Tee() {
    }

    public String getGuild_id() {
        return guild_id;
    }

    public String getTrigger() {
        return trigger;
    }

    public String getSource_url() {
        return source_url;
    }

    public CommandPerm getPerm() {
        return perm;
    }

    public String getCached_json() {
        return cached_json;
    }

    public void setCached_json(String cached_json) {
        this.cached_json = cached_json;
    }

    public void setPerm(CommandPerm perm) {
        if (perm == null) this.perm = CommandPerm.EVERYONE;
        else this.perm = perm;
    }
}
