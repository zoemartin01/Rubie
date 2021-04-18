package me.zoemartin.rubie.core;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class CommandEvent {
    @Nonnull
    private final User user;
    @Nonnull
    private final MessageChannel channel;
    @Nonnull
    private final String content;
    @Nonnull
    private final JDA jda;
    @Nonnull
    private final List<String> args;
    @Nonnull
    private final List<String> invoked;
    @Nonnull
    private final String argString;
    @Nullable
    private final Message message;

    public CommandEvent(@Nonnull User user, @Nonnull MessageChannel channel, @Nonnull String content, @Nonnull JDA jda,
                        @Nonnull List<String> args, @Nonnull List<String> invoked, @NotNull String argString) {
        this.user = user;
        this.channel = channel;
        this.content = content;
        this.jda = jda;
        this.args = args;
        this.invoked = invoked;
        this.message = null;
        this.argString = argString;
    }

    public CommandEvent(@Nonnull User user, @Nonnull MessageChannel channel, @Nonnull String content, @Nonnull JDA jda,
                        @Nonnull List<String> args, @Nonnull List<String> invoked, @Nullable Message message, @NotNull String argString) {
        this.user = user;
        this.channel = channel;
        this.content = content;
        this.jda = jda;
        this.args = args;
        this.invoked = invoked;
        this.message = message;
        this.argString = argString;
    }

    public CommandEvent(@Nonnull Message message, @Nonnull List<String> args, @Nonnull List<String> invoked, @NotNull String argString) {
        this.message = message;
        this.user = message.getAuthor();
        this.channel = message.getChannel();
        this.content = message.getContentRaw();
        this.jda = message.getJDA();
        this.args = args;
        this.invoked = invoked;
        this.argString = argString;
    }

    @Nonnull
    public String getArgString() {
        return argString;
    }

    @Nonnull
    public User getUser() {
        return user;
    }

    @Nonnull
    public MessageChannel getChannel() {
        return channel;
    }

    @Nonnull
    public String getContent() {
        return content;
    }

    @Nonnull
    public List<String> getArgs() {
        return args;
    }

    @Nonnull
    public LinkedList<String> getInvoked() {
        return new LinkedList<>(invoked);
    }

    @Nonnull
    public JDA getJDA() {
        return jda;
    }

    public void addCheckmark() {
        if (message == null) return;
        Emote emote = jda.getEmoteById("762424762412040192");
        if (emote == null) message.addReaction("U+2705").queue();
        else message.addReaction(emote).queue();
    }

    public void deleteInvoking() {
        if (message == null) return;
        message.delete().queue();
    }

    public List<Message.Attachment> getAttachments() {
        if (message == null) return Collections.emptyList();
        return message.getAttachments();
    }

    public boolean isFromGuild() {
        return this instanceof GuildCommandEvent;
    }

    public MessageAction reply(@Nullable String title, @NotNull String replyFormat,
                               @Nullable Object... args) {

        EmbedBuilder eb = new EmbedBuilder();
        if (title != null) eb.setTitle(title);
        eb.setDescription(String.format(replyFormat, args));
        return getChannel().sendMessage(eb.build());
    }
}
