package me.zoemartin.rubie.modules.reactroles;

import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.stream.Collectors;

public class ReactListener extends ListenerAdapter {
    @Override
    public void onGuildMessageReactionAdd(@NotNull GuildMessageReactionAddEvent event) {
        var guild = event.getGuild();
        var config = ReactManager.getConfig(guild);
        if (!config.isEnabled()) return;

        var rrs = ReactManager.forMessage(event);
        if (rrs.isEmpty()) return;

        var member = event.getMember();
        var react = event.getReaction().getReactionEmote();
        var roles = rrs.stream()
                        .filter(reactRole -> reactRole.getReact().equals(
                            react.isEmoji() ? react.getEmoji() : react.getEmote().getId()))
                        .collect(Collectors.toList());

        roles.stream().map(reactRole -> guild.getRoleById(reactRole.getRoleId())).filter(Objects::nonNull)
            .forEach(role -> guild.addRoleToMember(member, role).queue());
    }

    @Override
    public void onGuildMessageReactionRemove(@NotNull GuildMessageReactionRemoveEvent event) {
        var guild = event.getGuild();
        var config = ReactManager.getConfig(guild);
        if (!config.isEnabled()) return;

        var rrs = ReactManager.forMessage(event);
        if (rrs.isEmpty()) return;

        var member = event.getMember();
        if (member == null) return;
        var react = event.getReaction().getReactionEmote();
        var roles = rrs.stream()
                        .filter(reactRole -> reactRole.getReact().equals(
                            react.isEmoji() ? react.getEmoji() : react.getEmote().getId()))
                        .collect(Collectors.toList());

        roles.stream().map(reactRole -> guild.getRoleById(reactRole.getRoleId())).filter(Objects::nonNull)
            .forEach(role -> guild.removeRoleFromMember(member, role).queue());
    }
}
