package me.zoemartin.rubie.modules.commandProcessing;

import me.zoemartin.rubie.core.interfaces.CommandLogger;
import me.zoemartin.rubie.core.managers.CommandManager;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.util.Collection;

public class CommandListener extends ListenerAdapter {
    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        CommandLogger cl = CommandManager.getCommandLogger();

        if (cl != null && !event.isWebhookMessage()) cl.log(event.getMessage());

        if (event.getAuthor().isBot() || event.isWebhookMessage()) {
            return;
        }

        String message = event.getMessage().getContentRaw();
        if (event.isFromGuild()) {
            Collection<String> prefixes = Prefixes.getPrefixes(event.getGuild().getId());

            prefixes.forEach(s -> {
                if (message.startsWith(s)) {
                    CommandManager.process(event, message.substring(s.length()));
                }
            });
        } else {
            CommandManager.process(event, message);
        }
    }
}
