package me.zoemartin.rubie.core;

import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

public class GuildCommandEvent extends CommandEvent {
    private final Member member;
    private final Guild guild;
    private final TextChannel textChannel;

    // Exists only for debug purposes
    private GuildCommandEvent(@NotNull User user, @NotNull MessageChannel channel, @NotNull String content,
                              @NotNull JDA jda, @NotNull List<String> args, @NotNull List<String> invoked,
                              @Nullable Message message, Member member, Guild guild, TextChannel textChannel, String argString) {
        super(user, channel, content, jda, args, invoked, argString);
        this.member = member;
        this.guild = guild;
        this.textChannel = textChannel;
    }

    public GuildCommandEvent(Member member, TextChannel textChannel, String content, JDA jda, List<String> args, List<String> invoked, String argString) {
        super(member.getUser(), textChannel, content, jda, args, invoked, argString);
        this.member = member;
        this.guild = member.getGuild();
        this.textChannel = textChannel;
    }

    public GuildCommandEvent(Message message, List<String> args, List<String> invoked, Member member, Guild guild, TextChannel textChannel, String argString) {
        super(message, args, invoked, argString);
        if (!message.isFromGuild()) throw new IllegalStateException("Message not from a Guild!");
        this.member = member;
        this.guild = guild;
        this.textChannel = textChannel;
    }

    public GuildCommandEvent(Message message, List<String> args, List<String> invoked, String argString) {
        super(message, args, invoked, argString);
        if (!message.isFromGuild()) throw new IllegalStateException("Message not from a Guild!");
        this.member = message.getMember();
        this.guild = message.getGuild();
        this.textChannel = message.getTextChannel();
    }

    public Member getMember() {
        return member;
    }

    public Guild getGuild() {
        return guild;
    }

    public TextChannel getTextChannel() {
        return textChannel;
    }

    @Override
    public MessageAction reply(@Nullable String title, @NotNull String replyFormat, @Nullable Object... args) {
        if (!this.getGuild().getSelfMember()
                 .hasPermission(this.getTextChannel(), Permission.MESSAGE_EMBED_LINKS)) {
            EnumSet<Message.MentionType> deny = EnumSet.of(Message.MentionType.EVERYONE, Message.MentionType.HERE,
                Message.MentionType.ROLE);
            return getChannel().sendMessageFormat("**%s**:\n> %s", title, String.format(replyFormat, args))
                       .allowedMentions(EnumSet.complementOf(deny));
        }

        EmbedBuilder eb = new EmbedBuilder();
        if (title != null) eb.setTitle(title);
        eb.setColor(this.getGuild().getSelfMember().getColor());
        eb.setDescription(String.format(replyFormat, args));
        return getChannel().sendMessage(eb.build());
    }
}
