package me.zoemartin.rubie.modules.misc;

import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.annotations.Checks;
import me.zoemartin.rubie.core.annotations.Command;
import me.zoemartin.rubie.core.annotations.CommandOptions;
import me.zoemartin.rubie.core.exceptions.CommandPermissionException;
import me.zoemartin.rubie.core.exceptions.UnexpectedError;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.Check;
import me.zoemartin.rubie.core.util.Parser;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.stream.Collectors;

@Command
@CommandOptions(
        name = "simpletranscript",
        description = "Create a simple transcript of a channel and upload it as a txt",
        alias = "st",
        perm = CommandPerm.BOT_MANAGER,
        botPerms = Permission.MESSAGE_READ
)
@Checks.Permissions.Channel({Permission.MESSAGE_READ, Permission.MESSAGE_MANAGE})
public class SimpleTranscript extends GuildCommand {
    @Override
    public void run(GuildCommandEvent event) {
        event.addCheckmark();
        TextChannel c = null;
        if (!event.getArgs().isEmpty()) c = Parser.Channel.getTextChannel(event.getGuild(), event.getArgs().get(0));
        if (c == null) c = event.getTextChannel();

        Check.check(this.checkChannelPerms(event, c),
                () -> new CommandPermissionException(
                        "Error, you are missing the necessary permissions (READ, MANAGE_MESSAGE) in this channel for this command!"));
        Check.check(this.checkNecessaryPerms(event, c),
                () -> new CommandPermissionException(
                        "Error, I seem to be missing the necessary permissions to run this command!"));


        var m = c.getIterableHistory().stream().collect(Collectors.toList());
        Collections.reverse(m);
        var txt = m.stream().map(message -> String.format("[%s] %s (%s) >> %s",
                message.getTimeCreated().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                message.getAuthor().getAsTag(), message.getAuthor().getId(),
                message.getContentStripped()))
                .collect(Collectors.joining("\n"));
        try {
            event.getChannel().sendFile(txt.getBytes(StandardCharsets.UTF_8), "transcript.txt").queue();
        } catch (IllegalArgumentException e) {
            throw new UnexpectedError();
        }
    }
}
