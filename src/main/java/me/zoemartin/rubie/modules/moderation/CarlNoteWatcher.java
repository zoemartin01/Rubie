package me.zoemartin.rubie.modules.moderation;

import me.zoemartin.rubie.core.util.*;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public class CarlNoteWatcher extends ListenerAdapter {
    private static final String CARL_ID = "235148962103951360";
    private static final Pattern NOTE_PATTERN = Pattern.compile(".*setnote\\s+(.*?)\\s+.*");
    private static final Pattern WARN_PATTERN = Pattern.compile(".*warn\\s+(.*?)\\s+(.*)");

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        if (!event.getAuthor().getId().equals(CARL_ID)) return;
        var message = event.getMessage().getContentRaw();

        if (message.contains("Note taken."))
            processNote(event);
        else if (message.contains("has been warned"))
            processWarn(event);
    }

    private static void processNote(GuildMessageReceivedEvent event) {
        var msg = event.getMessage().getContentRaw();
        msg = msg.replaceFirst(".*\n\\*\\*Note:\\*\\* ", "");

        var reason = msg;
        var original = event.getChannel().getIterableHistory().stream()
                           .filter(message -> message.getContentRaw().contains(reason) && NOTE_PATTERN.matcher(message.getContentRaw()).find())
                           .findFirst().orElse(null);
        if (original == null) return;

        var matcher = NOTE_PATTERN.matcher(original.getContentRaw());
        if (!matcher.find()) return;
        var uRef = matcher.group(1);
        User u;
        if (Parser.User.isParsable(uRef)) u = CacheUtils.getUser(uRef);
        else if (Parser.User.tagIsParsable(uRef)) u = event.getJDA().getUserByTag(uRef);
        else u = null;
        if (u == null) return;

        var note = new NoteEntity(event.getGuild().getId(), u.getId(),
            original.getAuthor().getId(), reason,
            event.getMessage().getTimeCreated().toInstant().toEpochMilli());

        DatabaseUtil.saveObject(note);
    }

    private static void processWarn(GuildMessageReceivedEvent event) {
        var msg = event.getMessage().getContentRaw();
        msg = msg.replaceFirst(" has been warned, this is their .*? warning.", "");
        msg = msg.replaceAll("\\*\\*", "");

        var u = event.getJDA().getUserByTag(msg);
        if (u == null) return;
        var original = event.getChannel().getIterableHistory().stream()
                           .filter(message -> {
                               var m = WARN_PATTERN.matcher(message.getContentRaw());
                               return m.find() && (u.getAsTag().equals(m.group(1)) || u.getId().equals(m.group(1)));
                           }).findFirst().orElse(null);
        if (original == null) return;

        var matcher = WARN_PATTERN.matcher(original.getContentRaw());
        if (!matcher.find()) return;
        var note = new ModLogEntity(event.getGuild().getId(), u.getId(),
            original.getAuthor().getId(), matcher.group(2),
            event.getMessage().getTimeCreated().toInstant().toEpochMilli(), ModLogEntity.ModLogType.WARN);

        DatabaseUtil.saveObject(note);
    }


}
