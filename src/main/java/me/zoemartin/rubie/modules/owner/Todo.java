package me.zoemartin.rubie.modules.owner;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.*;
import me.zoemartin.rubie.core.annotations.Module;
import me.zoemartin.rubie.core.annotations.*;
import me.zoemartin.rubie.core.exceptions.CommandArgumentException;
import me.zoemartin.rubie.core.interfaces.AbstractCommand;
import me.zoemartin.rubie.core.interfaces.ModuleInterface;
import me.zoemartin.rubie.core.util.Check;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import java.time.Instant;

@Module
@Command
@CommandOptions(
    name = "todo",
    description = "Create a new TODO-Entry",
    perm = CommandPerm.OWNER,
    usage = "<message>"
)
public class Todo extends AbstractCommand implements ModuleInterface {
    static final String DEV_SERVER_ID = "771750212200562749";
    static final String CHANNEL_ID = "779770625329070171";

    @Override
    public void init() {
        Bot.addListener(new Logging());
    }

    @Override
    public void run(CommandEvent event) {
        Check.check(!event.getArgs().isEmpty(), CommandArgumentException::new);
        Guild dest = event.getJDA().getGuildById(DEV_SERVER_ID);
        Check.entityReferenceNotNull(dest, Guild.class, "DEV SERVER");

        EmbedBuilder eb = new EmbedBuilder().setTitle("New TODO")
                              .setDescription(event.getArgString())
                              .setColor(0xdf136c)
                              .setAuthor(event.getUser().getAsTag(), null, event.getUser().getEffectiveAvatarUrl())
                              .setTimestamp(Instant.now())
                              .setFooter(event.isFromGuild() ? ((GuildCommandEvent) event).getGuild().toString() : "From DMs");

        TextChannel channel = dest.getTextChannelById(CHANNEL_ID);
        Check.entityReferenceNotNull(channel, TextChannel.class, "TODO Channel");
        Message m = channel.sendMessage(eb.build()).complete();
        Message orig = event.reply("Todo added", "**Message:** \n%s\n\n[Jump](%s)",
            event.getArgString(), m.getJumpUrl()).complete();
        m.editMessage(eb.addField("Original", String.format("[Jump](%s)", orig.getJumpUrl()), false).build()).queue();
    }
}
