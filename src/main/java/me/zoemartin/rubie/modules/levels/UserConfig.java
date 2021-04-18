package me.zoemartin.rubie.modules.levels;

import me.zoemartin.rubie.core.annotations.*;
import me.zoemartin.rubie.core.interfaces.DatabaseEntry;
import net.dv8tion.jda.api.entities.Member;
import org.hibernate.annotations.GenericGenerator;
import org.jetbrains.annotations.Nullable;

import javax.persistence.*;
import java.util.UUID;

@DatabaseEntity
@Entity
@Table(name = "level_userconfigs")
public class UserConfig implements DatabaseEntry {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
        name = "UUID",
        strategy = "org.hibernate.id.UUIDGenerator"
    )
    private UUID id;

    @Column(name = "guild_id")
    private String guildId;

    @Column(name = "user_id")
    private String userId;

    @Column
    private Integer color;

    public UserConfig(Member member) {
        this.guildId = member.getGuild().getId();
        this.userId = member.getId();
        this.color = null;
    }

    public UserConfig() {
    }

    @Getter("color")
    @Nullable
    public Integer getColor() {
        return color;
    }

    @Setter(name = "color", type = "hex color or reset")
    public void setColor(Color color) {
        if (color == null) this.color = null;
        else this.color = color.getRaw();
    }

    public String getGuildId() {
        return guildId;
    }

    public String getUserId() {
        return userId;
    }

    static class Color {
        private final int raw;

        public Color(int raw) {
            this.raw = raw;
        }

        public int getRaw() {
            return raw;
        }

        @Override
        public String toString() {
            return String.format("#%06X", (0xFFFFFF & raw));
        }
    }
}
