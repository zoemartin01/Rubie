package me.zoemartin.rubie.modules.commandProcessing;

import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.annotations.DatabaseEntity;
import me.zoemartin.rubie.core.interfaces.DatabaseEntry;

import javax.persistence.*;
import java.util.UUID;

@DatabaseEntity
@Entity
@Table(name = "role_permission")
public class RolePermission implements DatabaseEntry {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID uuid;

    @Column(name = "guild_id", updatable = false, nullable = false)
    private String guild_id;

    @Column(name = "role_id", updatable = false, nullable = false)
    private String role_id;

    @Column(name = "command_perm", nullable = false)
    @Convert(converter = CommandPerm.Converter.class)
    private CommandPerm perm;

    public RolePermission(UUID uuid, String guild_id, String role_id, CommandPerm perm) {
        this.uuid = uuid;
        this.guild_id = guild_id;
        this.role_id = role_id;
        this.perm = perm;
    }

    public RolePermission(String guild_id, String role_id, CommandPerm perm) {
        this.uuid = UUID.randomUUID();
        this.guild_id = guild_id;
        this.role_id = role_id;
        this.perm = perm;
    }

    public RolePermission() {
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getGuild_id() {
        return guild_id;
    }

    public String getRole_id() {
        return role_id;
    }

    public CommandPerm getPerm() {
        return perm;
    }

    public void setPerm(CommandPerm perm) {
        this.perm = perm;
    }
}
