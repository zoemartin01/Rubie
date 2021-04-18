package me.zoemartin.rubie.core;

import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serializable Wrapper for {@link MessageEmbed}
 * <p>
 * Q: Why does this exist and not just use {@link MessageEmbed} which is theoretically also serializable? A: Because the
 * use of {@link OffsetDateTime} makes {@link Gson} go crazy :(
 */
public class Embed implements Serializable {
    private final String title;
    private final String type;
    private final String description;
    private final String url;
    private final String timestamp;
    private final Integer color;
    private final Footer footer;
    private final Image image;
    private final Thumbnail thumbnail;
    private final Video video;
    private final Provider provider;
    private final Author author;
    private final List<Field> fields;

    private static Gson gson = null;

    public Embed(String title, String type, String description, String url, DateTime timestamp, Integer color,
                 Footer footer, Image image, Thumbnail thumbnail, Video video, Provider provider,
                 Author author, List<Field> fields) {
        this.title = title;
        this.type = type;
        this.description = description;
        this.url = url;
        this.timestamp = timestamp.toString();
        this.color = color;
        this.footer = footer;
        this.image = image;
        this.thumbnail = thumbnail;
        this.video = video;
        this.provider = provider;
        this.author = author;
        this.fields = fields;
    }

    public Embed(MessageEmbed embed) {
        this.title = embed.getTitle();
        this.type = embed.getType().name();
        this.description = embed.getDescription();
        this.url = embed.getUrl();
        this.timestamp = embed.getTimestamp() == null ? null : embed.getTimestamp().toString();
        this.color = embed.getColorRaw();
        this.footer = embed.getFooter() == null ? null : new Footer(embed.getFooter());
        this.image = embed.getImage() == null ? null : new Image(embed.getImage());
        this.thumbnail = embed.getThumbnail() == null ? null : new Thumbnail(embed.getThumbnail());
        this.video = embed.getVideoInfo() == null ? null : new Video(embed.getVideoInfo());
        this.provider = embed.getSiteProvider() == null ? null : new Provider(embed.getSiteProvider());
        this.author = embed.getAuthor() == null ? null : new Author(embed.getAuthor());
        this.fields = embed.getFields().stream().map(Field::new).collect(Collectors.toList());
    }

    public static Embed fromJson(String json) {
        if (gson == null) gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.fromJson(json, Embed.class);
    }

    public String toJson() {
        if (gson == null) gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }

    public MessageEmbed toDiscordEmbed() {
        EmbedBuilder eb = new EmbedBuilder()
                              .setTitle(title)
                              .setDescription(description);
        if (color != null) eb.setColor(color);
        if (timestamp != null) eb.setTimestamp(OffsetDateTime.parse(timestamp));
        if (footer != null) {
            if (footer.icon_url != null) eb.setFooter(footer.text, footer.icon_url);
            else eb.setFooter(footer.text);
        }
        if (author != null) {
            if (author.url == null && author.icon_url == null) eb.setAuthor(author.name);
            else if (author.icon_url == null) eb.setAuthor(author.name, author.url);
            else if (author.url == null) eb.setAuthor(author.name, null, author.icon_url);
            else eb.setAuthor(author.name, author.url, author.icon_url);
        }
        if (image != null && image.url != null) eb.setImage(image.url);
        if (thumbnail != null && thumbnail.url != null) eb.setThumbnail(thumbnail.url);
        if (fields != null && !fields.isEmpty())
            fields.forEach(field -> {
                if (field != null)
                    eb.addField(field.name, field.value, field.inline);
            });

        return eb.build();
    }

    public WebhookEmbed toWebhookEmbed() {
        WebhookEmbedBuilder eb = new WebhookEmbedBuilder()
                                     .setTitle(new WebhookEmbed.EmbedTitle(title, url))
                                     .setDescription(description);
        if (color != null) eb.setColor(color);
        if (timestamp != null) eb.setTimestamp(OffsetDateTime.parse(timestamp));
        if (footer != null) eb.setFooter(new WebhookEmbed.EmbedFooter(footer.text, footer.icon_url));
        if (author != null) eb.setAuthor(new WebhookEmbed.EmbedAuthor(author.name, author.icon_url, author.url));
        if (image != null && image.url != null) eb.setImageUrl(image.url);
        if (thumbnail != null && thumbnail.url != null) eb.setThumbnailUrl(thumbnail.url);
        if (fields != null && !fields.isEmpty())
            fields.forEach(field -> {
                if (field != null)
                    eb.addField(new WebhookEmbed.EmbedField(field.inline, field.name, field.value));
            });
        return eb.build();
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public String getUrl() {
        return url;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public Integer getColor() {
        return color;
    }

    public Footer getFooter() {
        return footer;
    }

    public Image getImage() {
        return image;
    }

    public Thumbnail getThumbnail() {
        return thumbnail;
    }

    public Video getVideo() {
        return video;
    }

    public Provider getProvider() {
        return provider;
    }

    public Author getAuthor() {
        return author;
    }

    public List<Field> getFields() {
        return fields;
    }

    private static class Thumbnail implements Serializable {
        private final String url;
        private final String proxy_url;
        private final Integer height;
        private final Integer width;

        public Thumbnail(String url, String proxy_url, Integer height, Integer width) {
            this.url = url;
            this.proxy_url = proxy_url;
            this.height = height;
            this.width = width;
        }

        public Thumbnail(MessageEmbed.Thumbnail thumbnail) {
            this.url = thumbnail.getUrl();
            this.proxy_url = thumbnail.getProxyUrl();
            this.height = thumbnail.getHeight();
            this.width = thumbnail.getWidth();
        }

        public String getUrl() {
            return url;
        }

        public String getProxy_url() {
            return proxy_url;
        }

        public Integer getHeight() {
            return height;
        }

        public Integer getWidth() {
            return width;
        }
    }

    private static class Video implements Serializable {
        private final String url;
        private final Integer height;
        private final Integer width;

        public Video(String url, Integer height, Integer width) {
            this.url = url;
            this.height = height;
            this.width = width;
        }

        public Video(MessageEmbed.VideoInfo video) {
            this.url = video.getUrl();
            this.height = video.getHeight();
            this.width = video.getWidth();
        }

        public String getUrl() {
            return url;
        }

        public Integer getHeight() {
            return height;
        }

        public Integer getWidth() {
            return width;
        }
    }

    private static class Image implements Serializable {
        private final String url;
        private final String proxy_url;
        private final Integer height;
        private final Integer width;

        public Image(String url, String proxy_url, Integer height, Integer width) {
            this.url = url;
            this.proxy_url = proxy_url;
            this.height = height;
            this.width = width;
        }

        public Image(MessageEmbed.ImageInfo image) {
            this.url = image.getUrl();
            this.proxy_url = image.getProxyUrl();
            this.height = image.getHeight();
            this.width = image.getWidth();
        }

        public String getUrl() {
            return url;
        }

        public String getProxy_url() {
            return proxy_url;
        }

        public Integer getHeight() {
            return height;
        }

        public Integer getWidth() {
            return width;
        }
    }

    private static class Provider implements Serializable {
        private final String name;
        private final String url;

        public Provider(String name, String url) {
            this.name = name;
            this.url = url;
        }

        public Provider(MessageEmbed.Provider provider) {
            this.name = provider.getName();
            this.url = provider.getUrl();
        }

        public String getName() {
            return name;
        }

        public String getUrl() {
            return url;
        }
    }

    private static class Author implements Serializable {
        private final String name;
        private final String url;
        private final String icon_url;
        private final String proxy_icon_url;

        public Author(String name, String url, String icon_url, String proxy_icon_url) {
            this.name = name;
            this.url = url;
            this.icon_url = icon_url;
            this.proxy_icon_url = proxy_icon_url;
        }

        public Author(MessageEmbed.AuthorInfo author) {
            this.name = author.getName();
            this.url = author.getUrl();
            this.icon_url = author.getIconUrl();
            this.proxy_icon_url = author.getProxyIconUrl();
        }

        public String getName() {
            return name;
        }

        public String getUrl() {
            return url;
        }

        public String getIcon_url() {
            return icon_url;
        }

        public String getProxy_icon_url() {
            return proxy_icon_url;
        }
    }

    private static class Footer implements Serializable {
        private final String text;
        private final String icon_url;
        private final String proxy_icon_url;

        public Footer(String text, String icon_url, String proxy_icon_url) {
            this.text = text;
            this.icon_url = icon_url;
            this.proxy_icon_url = proxy_icon_url;
        }

        public Footer(MessageEmbed.Footer footer) {
            this.text = footer.getText();
            this.icon_url = footer.getIconUrl();
            this.proxy_icon_url = footer.getProxyIconUrl();
        }

        public String getText() {
            return text;
        }

        public String getIcon_url() {
            return icon_url;
        }

        public String getProxy_icon_url() {
            return proxy_icon_url;
        }
    }

    private static class Field implements Serializable {
        private final String name;
        private final String value;
        private final boolean inline;

        public Field(String name, String value, boolean inline) {
            this.name = name;
            this.value = value;
            this.inline = inline;
        }

        public Field(MessageEmbed.Field field) {
            this.name = field.getName();
            this.value = field.getValue();
            this.inline = field.isInline();
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        public Boolean getInline() {
            return inline;
        }
    }
}
