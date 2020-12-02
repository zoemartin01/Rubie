package me.zoemartin.rubie.modules.reactroles;

import com.google.auto.service.AutoService;
import me.zoemartin.rubie.core.annotations.Getter;
import me.zoemartin.rubie.core.annotations.Setter;
import me.zoemartin.rubie.core.interfaces.DatabaseEntry;

import javax.persistence.*;

@Entity
@Table(name = "reactrole_configs")
@AutoService(DatabaseEntry.class)
public class ReactConfig implements DatabaseEntry {

    @Id
    @Column(name = "guild_id")
    private String guildId;

    @Column(name = "enabled")
    private boolean enabled;

    public ReactConfig() {
    }

    public ReactConfig(String guildId) {
        this.guildId = guildId;
        this.enabled = false;
    }

    public String getGuildId() {
        return guildId;
    }

    @Getter("enabled")
    public boolean isEnabled() {
        return enabled;
    }

    @Setter(name = "enabled")
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
