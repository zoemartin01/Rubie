package me.zoemartin.rubie.core;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GuildCommandEvent extends CommandEvent {
    private final Member member;
    private final Guild guild;
    private final TextChannel textChannel;

    public GuildCommandEvent(@NotNull User user, @NotNull MessageChannel channel, @NotNull String content,
                             @NotNull JDA jda, @NotNull List<String> args, @NotNull List<String> invoked,
                             @Nullable Message message, Member member, Guild guild, TextChannel textChannel) {
        super(user, channel, content, jda, args, invoked);
        this.member = member;
        this.guild = guild;
        this.textChannel = textChannel;
    }

    public GuildCommandEvent(Member member, TextChannel textChannel, String content, JDA jda, List<String> args, List<String> invoked) {
        super(member.getUser(), textChannel, content, jda, args, invoked);
        this.member = member;
        this.guild = member.getGuild();
        this.textChannel = textChannel;
    }

    public GuildCommandEvent(Message message, List<String> args, List<String> invoked, Member member, Guild guild, TextChannel textChannel) {
        super(message, args, invoked);
        if (!message.isFromGuild()) throw new IllegalStateException("Message not from a Guild!");
        this.member = member;
        this.guild = guild;
        this.textChannel = textChannel;
    }

    public GuildCommandEvent(Message message, List<String> args, List<String> invoked) {
        super(message, args, invoked);
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
}
