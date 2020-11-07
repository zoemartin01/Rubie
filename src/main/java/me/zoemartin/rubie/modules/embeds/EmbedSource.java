package me.zoemartin.rubie.modules.embeds;

import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.Embed;
import me.zoemartin.rubie.core.exceptions.*;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.Check;
import me.zoemartin.rubie.core.util.Parser;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.util.List;

public class EmbedSource implements GuildCommand {
    @Override
    public void run(Member user, TextChannel channel, List<String> args, Message original, String invoked) {
        Check.check(!args.isEmpty(), CommandArgumentException::new);

        String messageId = args.get(0);
        Check.check(messageId.matches("\\d{17,19}"),
            () -> new ReplyError("Error, `%s` is not a valid message id!", messageId));

        TextChannel c;

        if (args.size() == 1) {
            c = channel;
        } else {
            String cRef = args.get(1);
            c = Parser.Channel.getTextChannel(original.getGuild(), cRef);
            Check.entityReferenceNotNull(c, TextChannel.class, cRef);
        }

        Check.check(user.hasPermission(c, Permission.MESSAGE_READ),
            () -> new ConsoleError("Member '%s' doesn't have read permissions in channel '%s'",
                user.getId(), c.getId()));

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
            if (embed.toJson().length() >= 2000) {
                try {
                    channel.sendMessageFormat("Embed source over 2k characters: %s", haste(embed.toJson())).queue();
                } catch (IOException | InterruptedException e) {
                    throw new UnexpectedError();
                }
            } else channel.sendMessageFormat("```%s```", embed.toJson()).queue();
        });
    }

    @NotNull
    @Override
    public String name() {
        return "embedsource";
    }

    @NotNull
    @Override
    public CommandPerm commandPerm() {
        return CommandPerm.BOT_USER;
    }

    @NotNull
    @Override
    public String usage() {
        return "<message id> [channel]";
    }

    @NotNull
    @Override
    public String description() {
        return "Get an embeds source code";
    }

    public static String haste(String content) throws IOException, InterruptedException {
        final HttpClient client = HttpClient.newHttpClient();
        final HttpRequest request = HttpRequest.newBuilder(URI.create("https://hastebin.com/documents"))
                                       .POST(HttpRequest.BodyPublishers.ofString(content)).build();

        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        final String responseContent = response.body();
        final JSONObject responseJson = new JSONObject(responseContent);
        final String key = responseJson.getString("key");
        return "https://hastebin.com/" + key;
    }
}
