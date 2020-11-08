package me.zoemartin.rubie.core;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;

import javax.annotation.Nonnull;
import java.util.List;

public class GuildCommandEvent extends CommandEvent {
    private final Member member;
    private final Guild guild;
    private final TextChannel channel;

    public GuildCommandEvent(Member member, TextChannel channel, String content, JDA jda, List<String> args, List<String> invoked) {
        super(member.getUser(), channel, content, jda, args, invoked);
        this.member = member;
        this.guild = member.getGuild();
        this.channel = channel;
    }

    public GuildCommandEvent(Message message, List<String> args, List<String> invoked, Member member, Guild guild, TextChannel channel) {
        super(message, args, invoked);
        if (!message.isFromGuild()) throw new IllegalStateException("Message not from a Guild!");
        this.member = member;
        this.guild = guild;
        this.channel = channel;
    }

    public GuildCommandEvent(Message message, List<String> args, List<String> invoked) {
        super(message, args, invoked);
        if (!message.isFromGuild()) throw new IllegalStateException("Message not from a Guild!");
        this.member = message.getMember();
        this.guild = message.getGuild();
        this.channel = message.getTextChannel();
    }

    public Member getMember() {
        return member;
    }

    public Guild getGuild() {
        return guild;
    }

    @Override
    @Nonnull
    public TextChannel getChannel() {
        return channel;
    }
}
