package me.zoemartin.rubie.modules.pagedEmbeds;

import me.zoemartin.rubie.Bot;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.jodah.expiringmap.ExpiringMap;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class Confirmation extends ListenerAdapter {
    private static final Map<String, ConfirmationMessage> confirmations = ExpiringMap.builder()
                                                                              .expiration(10, TimeUnit.MINUTES)
                                                                              .asyncExpirationListener((key, value) -> ((ConfirmationMessage) value).onNo())
                                                                              .build();

    @Override
    public void onGuildMessageReactionAdd(@NotNull GuildMessageReactionAddEvent event) {
        if (event.getUser().isBot()) return;
        var id = event.getMessageId();
        if (!confirmations.containsKey(id)) return;

        var cm = confirmations.get(id);
        if (cm.getUser() != event.getUser()) return;

        var emote = event.getReactionEmote();
        if (emote.isEmoji()) {
            switch (emote.getAsCodepoints().toUpperCase()) {
                case "U+2705" -> {
                    cm.onYes();
                    confirmations.remove(id);
                }
                case "U+274C" -> {
                    cm.onNo();
                    confirmations.remove(id);
                }
            }
        } else {
            if (emote.getEmote().getId().equals("825884623115780126")) {
                cm.onYes();
                confirmations.remove(id);
            }
        }

    }

    public static void newConfirmation(User user, MessageAction action, Supplier<?> onYes) {
        var msg = action.complete();

        var yes = Bot.getJDA().getEmoteById("825884623115780126");
        if (yes == null) msg.addReaction("U+2705").queue();
        else msg.addReaction(yes).queue();

        msg.addReaction("U+274C").queue();

        var cm = new ConfirmationMessage(user, msg, onYes);
        confirmations.put(msg.getId(), cm);
    }

    private static class ConfirmationMessage {
        private final Message message;
        private final User user;
        private final Supplier<?> yes;

        public ConfirmationMessage(User user, Message message, Supplier<?> yes) {
            this.user = user;
            this.message = message;
            this.yes = yes;
        }

        public Message getMessage() {
            return message;
        }

        public User getUser() {
            return user;
        }

        public void onYes() {
            yes.get();
            message.clearReactions().queue();
        }

        public void onNo() {
            message.clearReactions().queue();
        }
    }
}
