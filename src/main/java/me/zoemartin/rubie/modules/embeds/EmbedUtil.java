package me.zoemartin.rubie.modules.embeds;

import me.zoemartin.rubie.core.exceptions.ReplyError;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmbedUtil {
    public static String jsonFromUrl(String url) {
        Matcher paste = Pattern.compile(
            "(?:(?:http(?:s)?://)?(pastebin.com|hastebin.com|starb.in)/(?:raw/)?)([-a-zA-Z0-9@:%_+.~#?&=]*)"
        ).matcher(url);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request;
        if (paste.find()) {
            request = HttpRequest.newBuilder()
                          .uri(URI.create("https://" + paste.group(1) + "/raw/" + paste.group(2)))
                          .build();
        } else {
            request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        }

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (IOException | InterruptedException e) {
            throw new ReplyError("Sorry, I cannot process this url!");
        }
    }
}
