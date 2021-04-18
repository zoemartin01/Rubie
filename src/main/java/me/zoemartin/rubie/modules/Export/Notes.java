package me.zoemartin.rubie.modules.Export;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.annotations.*;
import me.zoemartin.rubie.core.exceptions.CommandArgumentException;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.Check;
import me.zoemartin.rubie.core.util.Parser;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Command
@CommandOptions(
    name = "exportnotes",
    description = "Export notes from supported bots by parsing the `notes` command output. (Carl-Bot, Dyno, Auttaja)",
    usage = "<channel...>",
    perm = CommandPerm.OWNER
)
public class Notes extends GuildCommand {
    private static final String CARL_ID = "235148962103951360";
    private static final String DYNO_ID = "155149108183695360";
    private static final String AUTTAJA_ID = "242730576195354624";

    private static final Logger log = LoggerFactory.getLogger(Notes.class);

    @Override
    public void run(GuildCommandEvent event) {
        Check.check(!event.getArgs().isEmpty(), CommandArgumentException::new);
        ScheduledExecutorService se = new ScheduledThreadPoolExecutor(1);
        se.scheduleAtFixedRate(() -> event.getChannel().sendTyping().complete(), 0, 10, TimeUnit.SECONDS);

        event.getGuild().loadMembers().get();

        List<TextChannel> channels = event.getArgs().stream().map(s -> Parser.Channel.getTextChannel(event.getGuild(), s))
                                         .collect(Collectors.toList());

        Collection<NoteEntry> notes = new ArrayList<>();
        channels.parallelStream().forEach(c -> c.getIterableHistory().stream()
                                  .filter(message -> message.getAuthor().getId().equals(CARL_ID)
                                                         || message.getAuthor().getId().equals(DYNO_ID)
                                                         || message.getAuthor().getId().equals(AUTTAJA_ID))
                                  .forEach(message -> {
                                      log.info("Read Messages in {}, Parsed: {}", c.getName(), notes.size());
                                      switch (message.getAuthor().getId()) {
                                          case CARL_ID -> message.getEmbeds().forEach(embed -> notes.addAll(parseCarl(embed)));
                                          case DYNO_ID -> message.getEmbeds().forEach(embed -> notes.addAll(parseDyno(embed)));
                                          case AUTTAJA_ID -> message.getEmbeds().forEach(embed -> notes.addAll(parseAuttaja(embed)));
                                      }
                                  }));

        var export = new HashSet<>(notes);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        event.getChannel().sendFile(gson.toJson(export).getBytes(), "notes_" + Instant.now() + ".json").complete();
        se.shutdown();
    }

    private static final Pattern CARL_NOTE_PATTERN = Pattern.compile("(?:From <@!?)(\\d{16,19})(?:> at )(.*)(?::\\n)(.*)");
    private static final DateTimeFormatter CARL_TIME = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    public static Collection<NoteEntry> parseCarl(MessageEmbed embed) {
        List<MessageEmbed.Field> fields = embed.getFields();
        if (fields.isEmpty() || fields.get(0).getName() == null
                || !fields.get(0).getName().contains("Note")) return Collections.emptyList();

        if (embed.getAuthor() == null || embed.getAuthor().getName() == null) return Collections.emptyList();
        User u = Bot.getJDA().getUserByTag(embed.getAuthor().getName());
        if (u == null) return Collections.emptySet();
        String user_id = u.getId();

        return fields.stream().map(field -> {
            if (field.getValue() == null) return null;
            Matcher m = CARL_NOTE_PATTERN.matcher(field.getValue());
            if (!m.find()) return null;

            return new NoteEntry(user_id, m.group(1), m.group(3), CARL_TIME.parseDateTime(m.group(2)).toString());
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private static final Pattern DYNO_MOD_PATTERN = Pattern.compile("(?:Moderator: )(.{1,32}#\\d{4})");
    private static final Pattern DYNO_FIELD_PATTERN = Pattern.compile("(.*)(?: - )(\\w{2,4} \\d+ \\d+)");
    private static final Pattern DYNO_USER_PATTERN = Pattern.compile("(?:Notes for .{1,32}#\\d{4})(?: \\()(\\d{16,19})(?:\\))");
    private static final DateTimeFormatter DYNO_TIME = DateTimeFormat.forPattern("MMM dd yyyy");

    public static Collection<NoteEntry> parseDyno(MessageEmbed embed) {
        if (embed.getAuthor() == null || embed.getAuthor().getName() == null) return Collections.emptyList();
        Matcher user = DYNO_USER_PATTERN.matcher(embed.getAuthor().getName());
        if (!user.find()) return Collections.emptyList();
        String user_id = user.group(1);

        return embed.getFields().stream().map(field -> {
            if (field.getValue() == null || field.getName() == null) return null;
            Matcher mod = DYNO_MOD_PATTERN.matcher(field.getName());
            Matcher m = DYNO_FIELD_PATTERN.matcher(field.getValue());

            String mod_id = null;
            String mod_tag = null;
            if (mod.find()) {
                mod_tag = mod.group(1);
                User u = Bot.getJDA().getUserByTag(mod_tag);
                mod_id = u == null ? null : u.getId();
            }

            if (!m.find()) return null;

            DateTime time = DYNO_TIME.parseDateTime(m.group(2));

            if (mod_id != null) return new NoteEntry(user_id, mod_id, m.group(1), time.toString());
            else return new NoteEntry(user_id, null, mod_tag, m.group(1), time.toString());
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private static final Pattern AUTTAJA_USER_PATTERN = Pattern.compile("(?:Notes on <@!?)(\\d{16,19})(?:> \\(.{1,32}#\\d{4}\\))");
    private static final Pattern AUTTAJA_MOD_PATTERN = Pattern.compile("(?:\\d+ by )(.{1,32}#\\d{4})");
    private static final Pattern AUTTAJA_FIELD_PATTERN = Pattern.compile("(?:`)(.*)(?:`\\nMade at )(.*)");
    private static final DateTimeFormatter AUTTAJA_TIME = DateTimeFormat.forPattern("MM/dd/yy HH:mm:ss");

    public static Collection<NoteEntry> parseAuttaja(MessageEmbed embed) {
        if (embed.getDescription() == null) return Collections.emptyList();
        Matcher user = AUTTAJA_USER_PATTERN.matcher(embed.getDescription());
        if (!user.find()) return Collections.emptyList();
        String user_id = user.group(1);

        return embed.getFields().stream().map(field -> {
            if (field.getValue() == null) return null;
            Matcher mod = field.getName() == null ? null : AUTTAJA_MOD_PATTERN.matcher(field.getName());
            String mod_id = null;
            String mod_tag = null;
            if (mod != null && mod.find()) {
                mod_tag = mod.group(1);
                User u = Bot.getJDA().getUserByTag(mod_tag);
                mod_id = u == null ? null : u.getId();
            }

            Matcher m = AUTTAJA_FIELD_PATTERN.matcher(field.getValue());
            if (!m.find()) return null;

            DateTime time = AUTTAJA_TIME.parseDateTime(m.group(2));

            if (mod_id != null) return new NoteEntry(user_id, mod_id, m.group(1), time.toString());
            else return new NoteEntry(user_id, null, mod_tag, m.group(1), time.toString());
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public static class NoteEntry implements Serializable {
        private final String user_id;
        private final String moderator_id;
        // fallback only
        private final String moderator_tag;
        private final String note;
        private final String timestamp;

        public NoteEntry(String user_id, String moderator_id, String note, String timestamp) {
            this.user_id = user_id;
            this.moderator_id = moderator_id;
            this.note = note;
            this.timestamp = timestamp;
            this.moderator_tag = null;
        }

        public NoteEntry(String user_id, String moderator_id, String moderator_tag, String note, String timestamp) {
            this.user_id = user_id;
            this.moderator_id = moderator_id;
            this.moderator_tag = moderator_tag;
            this.note = note;
            this.timestamp = timestamp;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;

            if (o == null || getClass() != o.getClass()) return false;

            NoteEntry noteEntry = (NoteEntry) o;

            return new EqualsBuilder()
                       .append(user_id, noteEntry.user_id)
                       .append(note, noteEntry.note)
                       .append(timestamp, noteEntry.timestamp)
                       .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37)
                       .append(user_id)
                       .append(note)
                       .append(timestamp)
                       .toHashCode();
        }

        public String getUser_id() {
            return user_id;
        }

        public String getModerator_id() {
            return moderator_id;
        }

        public String getModerator_tag() {
            return moderator_tag;
        }

        public String getNote() {
            return note;
        }

        public String getTimestamp() {
            return timestamp;
        }
    }
}
