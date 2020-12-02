package me.zoemartin.rubie.core.util;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.exceptions.ReplyError;
import me.zoemartin.rubie.core.exceptions.UserNotFoundException;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class CacheUtils {
    private CacheUtils() {
        throw new IllegalAccessError();
    }

    @NotNull
    public static User getUserExplicit(String id) {
        Check.notNull(id, () -> new IllegalArgumentException("id may not be null"));
        Check.check(Parser.User.isParsable(id), () -> new IllegalArgumentException("id not in correct format"));

        try {
            User u = Bot.getJDA().getUserById(Parser.User.parse(id));
            return u == null ? Bot.getJDA().retrieveUserById(id).complete() : u;
        } catch (ErrorResponseException e) {
            throw new ReplyError("Error, user not found!");
        }
    }

    @NotNull
    public static Member getMemberExplicit(Guild guild, String id) {
        Check.notNull(guild, () -> new IllegalArgumentException("Guild may not be null"));
        Check.notNull(id, () -> new IllegalArgumentException("id may not be null"));
        Check.check(Parser.User.isParsable(id), () -> new IllegalArgumentException("id not in correct format"));

        User u = getUserExplicit(id);

        try {
            Member m = guild.getMember(u);
            return m == null ? guild.retrieveMember(u).complete() : m;
        } catch (ErrorResponseException e) {
            throw new UserNotFoundException();
        }
    }

    @Nullable
    public static User getUser(String id) {
        if (id == null || !Parser.User.isParsable(id)) return null;

        try {
            User u = Bot.getJDA().getUserById(Parser.User.parse(id));
            return u == null ? Bot.getJDA().retrieveUserById(id).complete() : u;
        } catch (ErrorResponseException e) {
            return null;
        }
    }

    @Nullable
    public static Member getMember(Guild guild, String id) {
        if (guild == null || id == null || !Parser.User.isParsable(id)) return null;

        User u;
        try {
            u = getUserExplicit(id);
        } catch (ReplyError e) {
            return null;
        }

        try {
            Member m = guild.getMember(u);
            return m == null ? guild.retrieveMember(u).complete() : m;
        } catch (ErrorResponseException e) {
            return null;
        }
    }

    @Nullable
    public static Emote getEmote(String id) {
        if (id == null || !Parser.Emote.isParsable(id)) return null;

        try {
            var e = Bot.getJDA().getEmoteById(Parser.Emote.parse(id));
            if (e != null) return e;
            return Bot.getJDA().getGuilds().stream()
                       .map(guild -> guild.retrieveEmoteById(id).complete())
                       .filter(Objects::nonNull)
                       .findAny()
                       .orElse(null);
        } catch (ErrorResponseException e) {
            return null;
        }
    }
}
