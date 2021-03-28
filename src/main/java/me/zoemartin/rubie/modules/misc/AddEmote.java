package me.zoemartin.rubie.modules.misc;

import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.annotations.*;
import me.zoemartin.rubie.core.exceptions.*;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.Check;
import me.zoemartin.rubie.core.util.Parser;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

import java.io.IOException;
import java.net.*;
import java.util.regex.Pattern;

@Command
@CommandOptions(
    name = "addemote",
    alias = "steal",
    description = "Add an emote from an image or another server's emote",
    usage = "<image url|emote> [name]",
    botPerms = Permission.MANAGE_EMOTES,
    perm = CommandPerm.BOT_MANAGER
)
@Checks.Permissions.Guild(Permission.MANAGE_EMOTES)
public class AddEmote extends GuildCommand {
    private static final String GIF_URI = "https://cdn.discordapp.com/emojis/%s.gif";
    private static final String PNG_URI = "https://cdn.discordapp.com/emojis/%s.png";

    @Override
    public void run(GuildCommandEvent event) {
        var args = event.getArgs();
        Check.check(!args.isEmpty(), CommandArgumentException::new);
        var urlRef = args.get(0);

        var uri = "";
        var name = "";
        if (Parser.Emote.isParsable(urlRef)) {
            var id = Parser.Emote.parse(urlRef);
            uri = String.format(GIF_URI, id);
            if (!exists(uri)) uri = String.format(PNG_URI, id);
            name = Parser.Emote.parseName(urlRef);
        } else {
            uri = urlRef;
            var matcher = Pattern.compile("(?=(\\w+)\\.\\w{3,4}(?:\\?.*)?$).+").matcher(uri);
            Check.check(matcher.find(), () -> new ReplyError("Malformed URL!"));
            name = matcher.group(1);
        }

        URL url;
        try {
            url = new URL(uri);
        } catch (MalformedURLException e) {
            throw new ReplyError("Malformed URL!");
        }

        try {
            var icon = Icon.from(url.openStream());
            var emote = event.getGuild().createEmote(args.size() > 1 ? args.get(1) : name, icon).complete();

            event.reply("Added Emote", "Added %s with name `%s`", emote.getAsMention(), emote.getName()).queue();
        } catch (IOException e) {
            throw new UnexpectedError();
        } catch (ErrorResponseException e) {
            throw new ReplyError(e.getMeaning());
        }
    }

    public static boolean exists(String URLName) {
        try {
            var con = (HttpURLConnection) new URL(URLName).openConnection();
            con.setRequestMethod("HEAD");
            return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
        } catch (Exception e) {
            return false;
        }
    }
}
