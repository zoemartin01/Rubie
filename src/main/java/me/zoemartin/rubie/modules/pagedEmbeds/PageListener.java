package me.zoemartin.rubie.modules.pagedEmbeds;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.annotations.Module;
import me.zoemartin.rubie.core.interfaces.ModuleInterface;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Module
public class PageListener extends ListenerAdapter implements ModuleInterface {
    private static final Map<String, Set<PagedEmbed>> pages = new ConcurrentHashMap<>();

    @Override
    public void onGuildMessageReactionAdd(@Nonnull GuildMessageReactionAddEvent event) {
        if (pages.getOrDefault(event.getGuild().getId(), Collections.emptySet()).isEmpty()) return;

        PagedEmbed p = pages.get(event.getGuild().getId()).stream()
                           .filter(pagedEmbed -> pagedEmbed.getMessageId().equals(event.getMessageId()))
                           .findFirst().orElse(null);

        if (p == null) return;
        if (!event.getUser().getId().equals(p.getUserId())) return;
        if (!event.getReactionEmote().isEmoji()) return;

        switch (event.getReactionEmote().getAsCodepoints().toUpperCase()) {
            case PagedEmbed.BACK -> {
                p.last();
                event.getReaction().removeReaction(event.getUser()).queue();
            }
            case PagedEmbed.FORWARD -> {
                p.next();
                event.getReaction().removeReaction(event.getUser()).queue();
            }
            case PagedEmbed.STOP -> {
                p.stop();
                pages.get(event.getGuild().getId()).remove(p);
            }
        }
    }

    public static void add(PagedEmbed e) {
        if (e.getPages() > 1)
            pages.computeIfAbsent(e.getGuildId(), k -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                .add(e);
    }

    @Override
    public void init() {
        Bot.addListener(this);
        Bot.addListener(new Confirmation());
    }
}
