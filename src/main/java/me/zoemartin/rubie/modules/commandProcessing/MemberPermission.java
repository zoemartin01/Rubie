package me.zoemartin.rubie.modules.commandProcessing;

import com.google.auto.service.AutoService;
import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.interfaces.DatabaseEntry;

import javax.persistence.*;
import java.util.UUID;

@AutoService(DatabaseEntry.class)
@Entity
@Table(name = "member_permission")
public class MemberPermission implements DatabaseEntry {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID uuid;

    @Column(name = "guild_id", updatable = false, nullable = false)
    private String guild_id;

    @Column(name = "member_id", updatable = false, nullable = false)
    private String member_id;

    @Column(name = "command_perm", nullable = false)
    @Convert(converter = CommandPerm.Converter.class)
    private CommandPerm perm;

    public MemberPermission(UUID uuid, String guild_id, String member_id, CommandPerm perm) {
        this.uuid = uuid;
        this.guild_id = guild_id;
        this.member_id = member_id;
        this.perm = perm;
    }

    public MemberPermission(String guild_id, String member_id, CommandPerm perm) {
        this.uuid = UUID.randomUUID();
        this.guild_id = guild_id;
        this.member_id = member_id;
        this.perm = perm;
    }

    public MemberPermission() {
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getGuild_id() {
        return guild_id;
    }

    public String getMember_id() {
        return member_id;
    }

    public CommandPerm getPerm() {
        return perm;
    }

    public void setPerm(CommandPerm perm) {
        this.perm = perm;
    }
}
