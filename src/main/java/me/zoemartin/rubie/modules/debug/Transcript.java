package me.zoemartin.rubie.modules.debug;

import com.google.gson.Gson;
import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.annotations.Command;
import me.zoemartin.rubie.core.annotations.CommandOptions;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.Parser;
import net.dv8tion.jda.api.entities.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Command
@CommandOptions(
    name = "transcript",
    description = "Create a channel transcript",
    usage = "[channel]",
    perm = CommandPerm.OWNER
)
public class Transcript extends GuildCommand {
    @Override
    public void run(GuildCommandEvent event) {
        Instant start = Instant.now();
        Runtime runtime = Runtime.getRuntime();
        long memStart = runtime.totalMemory();
        ScheduledExecutorService se = new ScheduledThreadPoolExecutor(1);
        se.scheduleAtFixedRate(() -> event.getChannel().sendTyping().complete(), 0, 10, TimeUnit.SECONDS);

        MessageChannel c = null;
        if (!event.getArgs().isEmpty()) c = Parser.Channel.getTextChannel(event.getGuild(), event.getArgs().get(0));
        if (c == null) c = event.getChannel();

        int limit = 100;
        if (!event.getArgs().isEmpty() && c == event.getChannel() && Parser.Int.isParsable(event.getArgs().get(0)))
            limit = Parser.Int.parse(event.getArgs().get(0));

        // Memory Tracking
        AtomicLong memory = new AtomicLong();
        ScheduledExecutorService mem = new ScheduledThreadPoolExecutor(1);
        mem.scheduleAtFixedRate(() -> {
            synchronized (memory) {
                long m = runtime.totalMemory();
                if (m > memory.get()) memory.set(m);
            }
        }, 0, 1, TimeUnit.MILLISECONDS);

        long count = 0;
        File f = new File(Instant.now().toString() + ".json");
        Gson gson = new Gson();

        if (limit == -1) {
            List<Message> m = c.getIterableHistory().stream().collect(Collectors.toList());
            count = m.size();
            try (FileWriter w = new FileWriter(f)) {
                gson.toJson(m.stream().map(SerializableMessage::new).collect(Collectors.toList()), w);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            List<Message> m = new ArrayList<>(c.getHistoryFromBeginning(limit).complete().getRetrievedHistory());
            count += m.size();
            Message last = m.get(0);
            Collections.reverse(m);

            try {
                f.createNewFile();
                String json = gson.toJson(m.stream().map(SerializableMessage::new).collect(Collectors.toList()));
                json = json.substring(0, json.length() - 1);
                Files.write(f.toPath(), json.getBytes(), StandardOpenOption.APPEND);
            } catch (IOException e) {
                e.printStackTrace();
            }

            while (!c.getLatestMessageId().equals(last.getId())) {
                m = new ArrayList<>(c.getHistoryAfter(last, limit).complete().getRetrievedHistory());
                count += m.size();
                last = m.get(0);
                Collections.reverse(m);

                try {
                    String json = gson.toJson(m.stream().map(SerializableMessage::new).collect(Collectors.toList()));
                    json = ", " + json.substring(1, json.length() - 1);
                    Files.write(f.toPath(), json.getBytes(), StandardOpenOption.APPEND);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            try {
                Files.write(f.toPath(), "]".getBytes(), StandardOpenOption.APPEND);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        mem.shutdown();
        embedReply(event, "Transcript",
            "Saved a transcript with %s messages\n\n" +
                "Chunk size: %d\n" +
                "Memory Used: %d MB\n" +
                "File Size: %d %s\n" +
                "Time taken: %s seconds",
            count,
            limit,
            (memory.get() - memStart) / (1024L * 1024L),
            f.length() / (1048576L) > 0 ? f.length() / (1048576L) : f.length(),
            f.length() / (1048576L) > 0 ? "MB" : "B",
            Duration.between(start, Instant.now()).toSeconds()
        ).complete();
        se.shutdown();
    }

    private static class SerializableMessage implements Serializable {
        protected final long id;
        protected final long channel_id;
        protected final String content;
        protected final String timestamp;
        protected final String edited_timestamp;
        protected final boolean tts;
        protected final boolean pinned;
        protected final boolean mentions_everyone;
        protected final List<Embed> embeds;
        protected final List<Reaction> reactions;
        protected final Author author;
        protected final int type;


        public SerializableMessage(Message m) {
            id = m.getIdLong();
            channel_id = m.getChannel().getIdLong();
            content = m.getContentRaw();
            timestamp = m.getTimeCreated().toString();
            edited_timestamp = m.isEdited() ? m.getTimeEdited().toString() : null;
            tts = m.isTTS();
            pinned = m.isPinned();
            mentions_everyone = m.mentionsEveryone();
            author = new Author(m.getAuthor());
            type = m.getType().getId();
            embeds = m.getEmbeds().isEmpty() ? null : m.getEmbeds().stream().map(Embed::new).collect(Collectors.toList());
            reactions = m.getReactions().isEmpty() ? null : m.getReactions().stream().map(Reaction::new).collect(Collectors.toList());
        }

        private static class Author implements Serializable {
            protected final long id;
            protected final String username;
            protected final String avatar;
            protected final String discriminator;
            protected final boolean bot;

            public Author(User user) {
                id = user.getIdLong();
                username = user.getName();
                avatar = user.getAvatarId();
                discriminator = user.getDiscriminator();
                bot = user.isBot();
            }
        }

        private static class Reaction implements Serializable {
            protected final Emote emoji;
            protected final boolean me;
            protected final int count;

            public Reaction(MessageReaction r) {
                emoji = new Emote(r.getReactionEmote());
                me = r.isSelf();
                count = r.getCount();
            }

            private static class Emote implements Serializable {
                protected final Long id;
                protected final String name;
                protected final Boolean animated;
                protected final Boolean available;

                public Emote(MessageReaction.ReactionEmote e) {
                    id = e.isEmote() ? e.getIdLong() : null;
                    name = e.getName();
                    animated = e.isEmote() ? e.getEmote().isAnimated() : null;
                    available = e.isEmote() ? e.getEmote().isAvailable() : null;
                }
            }
        }

        private static class Embed implements Serializable {
            protected final String url;
            protected final String title;
            protected final String description;
            protected final String type;
            protected final String timestamp;
            protected final int color;
            protected final Thumbnail thumbnail;
            protected final Provider provider;
            protected final AuthorInfo author;
            protected final VideoInfo video_info;
            protected final Footer footer;
            protected final ImageInfo image;
            protected final List<Field> fields;

            public Embed(MessageEmbed e) {
                url = e.getUrl();
                title = e.getTitle();
                description = e.getDescription();
                type = e.getType().name();
                timestamp = e.getTimestamp() == null ? null : e.getTimestamp().toString();
                color = e.getColorRaw();
                thumbnail = e.getThumbnail() == null ? null : new Thumbnail(e.getThumbnail());
                provider = e.getSiteProvider() == null ? null : new Provider(e.getSiteProvider());
                author = e.getAuthor() == null ? null : new AuthorInfo(e.getAuthor());
                video_info = e.getVideoInfo() == null ? null : new VideoInfo(e.getVideoInfo());
                footer = e.getFooter() == null ? null : new Footer(e.getFooter());
                image = e.getImage() == null ? null : new ImageInfo(e.getImage());
                fields = e.getFields().isEmpty() ? null : e.getFields().stream().map(Field::new).collect(Collectors.toList());
            }

            private static class Thumbnail implements Serializable {
                protected final String url;
                protected final String proxyUrl;
                protected final int width;
                protected final int height;

                public Thumbnail(MessageEmbed.Thumbnail t) {
                    url = t.getUrl();
                    proxyUrl = t.getProxyUrl();
                    width = t.getWidth();
                    height = t.getHeight();
                }
            }

            private static class Provider implements Serializable {
                protected final String name;
                protected final String url;

                public Provider(MessageEmbed.Provider p) {
                    name = p.getName();
                    url = p.getUrl();
                }
            }

            private static class AuthorInfo implements Serializable {
                protected final String name;
                protected final String url;
                protected final String iconUrl;
                protected final String proxyIconUrl;

                public AuthorInfo(MessageEmbed.AuthorInfo a) {
                    name = a.getName();
                    url = a.getUrl();
                    iconUrl = a.getIconUrl();
                    proxyIconUrl = a.getProxyIconUrl();
                }
            }

            private static class VideoInfo implements Serializable {
                protected final String url;
                protected final int width;
                protected final int height;

                public VideoInfo(MessageEmbed.VideoInfo v) {
                    url = v.getUrl();
                    width = v.getWidth();
                    height = v.getHeight();
                }
            }

            private static class Footer implements Serializable {
                protected final String text;
                protected final String iconUrl;
                protected final String proxyIconUrl;

                public Footer(MessageEmbed.Footer f) {
                    text = f.getText();
                    iconUrl = f.getIconUrl();
                    proxyIconUrl = f.getProxyIconUrl();
                }
            }

            private static class ImageInfo implements Serializable {
                protected final String url;
                protected final String proxyUrl;
                protected final int width;
                protected final int height;

                public ImageInfo(MessageEmbed.ImageInfo i) {
                    url = i.getUrl();
                    proxyUrl = i.getProxyUrl();
                    width = i.getWidth();
                    height = i.getHeight();
                }
            }

            private static class Field implements Serializable {
                protected final String name;
                protected final String value;
                protected final boolean inline;

                public Field(MessageEmbed.Field f) {
                    name = f.getName();
                    value = f.getValue();
                    inline = f.isInline();
                }
            }
        }
    }
}
