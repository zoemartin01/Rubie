package me.zoemartin.rubie.core;

import me.zoemartin.rubie.Bot;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class CommandEvent {
    @Nonnull private final User user;
    @Nonnull private final MessageChannel channel;
    @Nonnull private final String content;
    @Nonnull private final JDA jda;
    @Nonnull private final List<String> args;
    @Nonnull private final List<String> invoked;
    @Nullable private final Message message;

    public CommandEvent(@Nonnull User user, @Nonnull MessageChannel channel, @Nonnull String content, @Nonnull JDA jda,
                        @Nonnull List<String> args, @Nonnull List<String> invoked) {
        this.user = user;
        this.channel = channel;
        this.content = content;
        this.jda = jda;
        this.args = args;
        this.invoked = invoked;
        this.message = null;
    }

    public CommandEvent(@Nonnull User user, @Nonnull MessageChannel channel, @Nonnull String content, @Nonnull JDA jda,
                        @Nonnull List<String> args, @Nonnull List<String> invoked, @Nullable Message message) {
        this.user = user;
        this.channel = channel;
        this.content = content;
        this.jda = jda;
        this.args = args;
        this.invoked = invoked;
        this.message = message;
    }

    public CommandEvent(@Nonnull Message message, @Nonnull List<String> args, @Nonnull List<String> invoked) {
        this.message = message;
        this.user = message.getAuthor();
        this.channel = message.getChannel();
        this.content = message.getContentRaw();
        this.jda = message.getJDA();
        this.args = args;
        this.invoked = invoked;
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

    @SuppressWarnings("ConstantConditions")
    public void addCheckmark() {
        if (message == null) return;
        message.addReaction(Bot.getJDA().getEmoteById("762424762412040192")).queue();
    }

    public void deleteInvoking() {
        if (message == null) return;
        message.delete().queue();
    }

    public List<Message.Attachment> getAttachments() {
        if (message == null) return Collections.emptyList();
        return message.getAttachments();
    }

}
