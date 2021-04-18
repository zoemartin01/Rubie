package me.zoemartin.rubie.modules.embeds;

import me.zoemartin.rubie.core.*;
import me.zoemartin.rubie.core.annotations.Command;
import me.zoemartin.rubie.core.annotations.CommandOptions;
import me.zoemartin.rubie.core.exceptions.*;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.Check;
import me.zoemartin.rubie.core.util.Parser;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.util.List;

@Command
@CommandOptions(
    name = "embedsource",
    description = "Get an embeds source code",
    usage = "<message id> [channel]",
    perm = CommandPerm.BOT_USER
)
public class EmbedSource extends GuildCommand {
    private static final String HASTE_HOST = "https://starb.in";

    @Override
    public void run(GuildCommandEvent event) {
        List<String> args = event.getArgs();
        Check.check(!args.isEmpty(), CommandArgumentException::new);

        String messageId = args.get(0);
        Check.check(messageId.matches("\\d{17,19}"),
            () -> new ReplyError("Error, `%s` is not a valid message id!", messageId));

        TextChannel c;

        if (args.size() == 1) {
            c = event.getTextChannel();
        } else {
            String cRef = args.get(1);
            c = Parser.Channel.getTextChannel(event.getGuild(), cRef);
            Check.entityReferenceNotNull(c, TextChannel.class, cRef);
        }

        Check.check(event.getMember().hasPermission(c, Permission.MESSAGE_READ),
            () -> new ConsoleError("Member '%s' doesn't have read permissions in channel '%s'",
                event.getMember().getId(), c.getId()));

        Message message;
        try {
            message = c.retrieveMessageById(messageId).complete();
        } catch (ErrorResponseException e) {
            if (e.getErrorResponse().getCode() == 10008)
                throw new EntityNotFoundException("Error, message `%s` not found in %s!", messageId, c.getAsMention());
            else
                throw new UnexpectedError();
        }

        message.getEmbeds().stream().map(Embed::new).forEach(embed -> {
            if (embed.toJson().length() >= 1990) {
                try {
                    event.getChannel().sendMessageFormat("Embed source over 2k characters: %s", haste(embed.toJson())).queue();
                } catch (IOException | InterruptedException e) {
                    throw new UnexpectedError();
                }
            } else
                event.reply("Embed Source", embed.toJson().isEmpty() ? "%s" : "```%s```", embed.toJson()).queue();
        });
    }

    public static String haste(String content) throws IOException, InterruptedException {
        final HttpClient client = HttpClient.newHttpClient();
        final HttpRequest request = HttpRequest.newBuilder(URI.create(HASTE_HOST + "/documents"))
                                        .POST(HttpRequest.BodyPublishers.ofString(content)).build();

        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        final String responseContent = response.body();
        final JSONObject responseJson = new JSONObject(responseContent);
        final String key;
        try {
            key = responseJson.getString("key");
        } catch (JSONException e) {
            throw new ReplyError("An unexpected error has occurred. If this error persists please notify the developer!");
        }
        return HASTE_HOST + key;
    }
}
