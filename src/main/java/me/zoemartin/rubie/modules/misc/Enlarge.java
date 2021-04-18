package me.zoemartin.rubie.modules.misc;

import de.androidpit.colorthief.ColorThief;
import me.zoemartin.rubie.core.CommandEvent;
import me.zoemartin.rubie.core.annotations.Command;
import me.zoemartin.rubie.core.annotations.CommandOptions;
import me.zoemartin.rubie.core.exceptions.*;
import me.zoemartin.rubie.core.interfaces.AbstractCommand;
import me.zoemartin.rubie.core.util.Check;
import me.zoemartin.rubie.core.util.Parser;
import net.dv8tion.jda.api.EmbedBuilder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

@Command
@CommandOptions(
    name = "enlarge",
    description = "Shows an enlarged version of an emote",
    usage = "<emote>"
)
public class Enlarge extends AbstractCommand {
    @Override
    public void run(CommandEvent event) {
        Check.check(event.getArgs().size() == 1 && Parser.Emote.isParsable(event.getArgs().get(0)), CommandArgumentException::new);

        String emoteId = Parser.Emote.parse(event.getArgs().get(0));

        URL gif, png;
        int gifResponse, pngResponse;

        try {
            png = new URL("https://cdn.discordapp.com/emojis/" + emoteId + ".png?v=1");
            gif = new URL("https://cdn.discordapp.com/emojis/" + emoteId + ".gif?v=1");
            gifResponse = ((HttpURLConnection) gif.openConnection()).getResponseCode();
            pngResponse = ((HttpURLConnection) png.openConnection()).getResponseCode();
        } catch (IOException e) {
            throw new ReplyError("Sorry, I could not find this emoji!");
        }

        Check.check(pngResponse == 200, () -> new ReplyError("Sorry, I could not find this emoji!"));
        EmbedBuilder eb = new EmbedBuilder();
        try {
            int[] color = ColorThief.getColor(ImageIO.read(png));
            eb.setColor(new Color(color[0], color[1], color[2]));
            //eb.setColor(averageColor(ImageIO.read(png)));
        } catch (IOException e) {
            throw new UnexpectedError();
        }


        if (gifResponse == 200) {
            eb.setImage(gif.toString());
        } else {
            eb.setImage(png.toString());
        }

        event.getChannel().sendMessage(eb.build()).queue();
    }

    // Unused at the moment
    public static Color averageColor(BufferedImage bi) {
        float r = 0;
        float b = 0;
        float g = 0;
        float total = 0;

        for (int y = 0; y < bi.getHeight(); y++) {
            for (int x = 0; x < bi.getWidth(); x++) {
                Color c = new Color(bi.getRGB(x, y));

                if (c.getAlpha() == 0) continue;
                r += c.getRed();
                g += c.getGreen();
                b += c.getBlue();
                total += c.getAlpha() / 255.0;
            }
        }

        r = r / total;
        g = g / total;
        b = b / total;

        r = r > 255 ? 255 : r;
        g = g > 255 ? 255 : g;
        b = b > 255 ? 255 : b;

        System.out.println(String.format("%s %s %s", r, g, b));

        return new Color(r / 255, g / 255, b / 255);
    }
}
