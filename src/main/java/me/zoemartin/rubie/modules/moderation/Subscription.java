package me.zoemartin.rubie.modules.moderation;

import me.zoemartin.rubie.core.annotations.DatabaseEntity;
import me.zoemartin.rubie.core.interfaces.DatabaseEntry;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.*;

@DatabaseEntity
@Table(name = "subscriptions")
@Entity
public class Subscription implements DatabaseEntry {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
        name = "UUID",
        strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "id")
    private UUID id;

    @Column(name = "guild_id")
    private String guildId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "target_id")
    private String targetId;

    @Column(name = "one_time")
    private boolean oneTime;

    @Column(name = "event_id", nullable = false)
    @Convert(converter = Event.Converter.class)
    private Event event;

    @MapKeyColumn(name = "key")
    @CollectionTable(name = "subs_settings")
    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "value", columnDefinition = "TEXT")
    private Map<String, String> settings;

    public Subscription() {
    }

    public Subscription(String guildId, String userId, String targetId, Event event, Map<String, String> settings, boolean oneTime) {
        this.guildId = guildId;
        this.userId = userId;
        this.targetId = targetId;
        this.oneTime = oneTime;
        this.event = event;
        this.settings = settings;
    }

    public Subscription(String guildId, String userId, String targetId, Event event, Map<String, String> settings) {
        this.guildId = guildId;
        this.userId = userId;
        this.targetId = targetId;
        this.event = event;
        this.settings = settings;
        this.oneTime = false;
    }

    public UUID getId() {
        return id;
    }

    public String getGuildId() {
        return guildId;
    }

    public String getUserId() {
        return userId;
    }

    public String getTargetId() {
        return targetId;
    }

    public Event getEvent() {
        return event;
    }

    public boolean isOneTime() {
        return oneTime;
    }

    public Map<String, String> getSettings() {
        return settings;
    }

    public enum Keys {
        CHANNEL("channel_id"),
        ROLE("role_id");
        private final String raw;

        Keys(String raw) {
            this.raw = raw;
        }

        public String raw() {
            return raw;
        }
    }

    public enum Event {
        UPDATE_APPEARANCE(0, "Update appearance"),
        // ^includes the following 4
        UPDATE_NAME(1, "Update name"),
        // ^includes the following 2
        UPDATE_NICKNAME(2, "Update nickname"),
        UPDATE_USERNAME(3, "Update username"),

        UPDATE_AVATAR(4, "Update avatar"),
        //UPDATE_STATUS(5, "Update status"),

        MEMBER_JOIN(11, "Member join"),
        MEMBER_LEAVE(12, "Member leave"),

        ROLE_ADD(21, "Role add"),
        ROLE_REMOVE(22, "Role remove"),

        MESSAGE_ACTIVITY(30, "Message activity"),
        // ^includes the following 3
        MESSAGE_SENT(31, "Message send"),
        MESSAGE_DELETED(32, "Message delete"),
        MESSAGE_EDIT(33, "Message edit"),

        REACTION_ADD(41, "Reaction add"),
        REACTION_REMOVE(42, "Reaction remove"),

        VOICE_UPDATE(50, "Voice update"),
        // ^includes the following 5
        VOICE_LEAVE(51, "Voice leave"),
        VOICE_JOIN(52, "Voice join"),
        VOICE_MUTE(53, "Voice mute"),
        VOICE_DEAFEN(54, "Voice deafen"),
        VOICE_STREAM(55, "Voice stream");

        private final int id;
        private final String name;

        Event(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int id() {
            return id;
        }

        @Override
        public String toString() {
            return name;
        }

        public static Event fromNum(Integer num) {
            if (num == null) return null;

            return EnumSet.allOf(Event.class).stream().filter(event -> num.equals(event.id())).findAny()
                       .orElse(null);
        }

        public static Event fromString(String name) {
            if (name == null) return null;

            return EnumSet.allOf(Event.class).stream().filter(event -> name.equalsIgnoreCase(event.name)
                                                                           || name.equalsIgnoreCase(event.name())).findAny()
                       .orElse(null);
        }

        public static class Converter implements AttributeConverter<Event, Integer> {
            @Override
            public Integer convertToDatabaseColumn(Event attribute) {
                return attribute.id();
            }

            @Override
            public Event convertToEntityAttribute(Integer dbData) {
                return Event.fromNum(dbData);
            }
        }
    }
}
