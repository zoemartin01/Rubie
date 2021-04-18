package me.zoemartin.rubie.modules.misc;

import de.androidpit.colorthief.ColorThief;
import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.annotations.Command;
import me.zoemartin.rubie.core.annotations.CommandOptions;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.CacheUtils;
import me.zoemartin.rubie.core.util.Parser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;

@Command
@CommandOptions(
    name = "avatar",
    description = "Shows a user's avatar",
    usage = "[user]"
)
public class Avatar extends GuildCommand {
    @Override
    public void run(GuildCommandEvent event) {
        User u = null;
        String arg;
        if (event.getArgs().isEmpty()) u = event.getUser();
        else if (Parser.User.isParsable(arg = lastArg(0, event))) u = CacheUtils.getUser(arg);
        else if (Parser.User.tagIsParsable(arg)) u = event.getJDA().getUserByTag(Objects.requireNonNull(arg));
        if (u == null) u = event.getUser();

        String avatarId = u.getAvatarId();
        String id = u.getId();
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Avatar for " + u.getAsTag());
        eb.setImage(u.getEffectiveAvatarUrl() + "?size=1024");

        if (avatarId == null) {
            eb.setDescription("[**Link**](" + u.getEffectiveAvatarUrl() + ")");
        } else {
            eb.setDescription(String.format("**Link as**\n" +
                                                "[png](https://cdn.discordapp.com/avatars/%s/%s.png?size=1024) | " +
                                                "[jpg](https://cdn.discordapp.com/avatars/%s/%s.jpg?size=1024) | " +
                                                "[webp](https://cdn.discordapp.com/avatars/%s/%s.webp?size=1024)",
                id, avatarId, id, avatarId, id, avatarId));
        }

        try {
            int[] color = ColorThief.getColor(ImageIO.read(new URL(u.getEffectiveAvatarUrl())));
            eb.setColor(new Color(color[0], color[1], color[2]));
        } catch (IOException ignored) {
        }

        event.getChannel().sendMessage(eb.build()).queue();
    }
}
