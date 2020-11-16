package me.zoemartin.rubie.core.interfaces;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.CommandEvent;
import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.util.Help;
import me.zoemartin.rubie.core.util.MessageUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class GuildCommand extends AbstractCommand {
    @Deprecated
    protected MessageAction embedReply(@NotNull Message original, @NotNull MessageChannel channel,
                                     @Nullable String title, @NotNull String replyFormat, @Nullable Object... args) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(original.getGuild().getSelfMember().getColor());
        if (title != null) eb.setTitle(title);
        eb.setDescription(String.format(replyFormat, args));
        return channel.sendMessage(eb.build());
    }

    protected MessageAction embedReply(GuildCommandEvent event, @NotNull MessageChannel channel,
                                     @Nullable String title, @NotNull String replyFormat, @Nullable Object... args) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(event.getGuild().getSelfMember().getColor());
        if (title != null) eb.setTitle(title);
        eb.setDescription(String.format(replyFormat, args));
        return channel.sendMessage(eb.build());
    }

    protected MessageAction embedReply(GuildCommandEvent event, @Nullable String title, @NotNull String replyFormat,
                                     @Nullable Object... args) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(event.getGuild().getSelfMember().getColor());
        if (title != null) eb.setTitle(title);
        eb.setDescription(String.format(replyFormat, args));
        return event.getChannel().sendMessage(eb.build());
    }

    @Deprecated
    protected String lastArg(int expectedIndex, List<String> args, Message original) {
        if (args.size() == expectedIndex + 1) return args.get(expectedIndex);

        String orig = original.getContentRaw();
        if (expectedIndex > 0) for (String s : new ArrayList<>(args.subList(0, expectedIndex)))
            orig = orig.replace(s, "");

        return MessageUtils.getArgsFrom(orig, args.get(expectedIndex));
    }

    public abstract void run(GuildCommandEvent event);

    @Override
    public void run(CommandEvent event) {
        if (event instanceof GuildCommandEvent) {
            run((GuildCommandEvent) event);
        } else {
            throw new IllegalStateException("Command Event not from a Guild!");
        }
    }

    @Deprecated
    public void run(Member user, TextChannel channel, List<String> args, Message original, String invoked) {
        run(new GuildCommandEvent(user, channel, original.getContentRaw(), original.getJDA(), args, List.of(invoked)));
    }

    public void run(User user, MessageChannel channel, List<String> args, Message original, String invoked) {
        run(original.getMember(), original.getTextChannel(), args, original, invoked);
    }

    @SuppressWarnings("ConstantConditions")
    @Deprecated
    protected void addCheckmark(Message message) {
        message.addReaction(Bot.getJDA().getEmoteById("762424762412040192")).queue();
    }

    @Deprecated
    public void help(Member user, MessageChannel channel, List<String> args, Message original) {
        throw new IllegalAccessError("Deprecated");
    }

    public void help(GuildCommandEvent event) {
        if (Help.getHelper() != null) Help.getHelper().send(event);
    }
}
