package me.zoemartin.rubie.modules.commandProcessing;

import me.zoemartin.rubie.core.CommandPerm;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class GuildJoin extends ListenerAdapter {
    private static final String DEFAULT_PREFIX = "r;";

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        var roles = event.getGuild().getRoles();
        var guildId = event.getGuild().getId();

        roles.stream().filter(role -> role.hasPermission(Permission.ADMINISTRATOR))
            .map(ISnowflake::getId)
            .forEach(role -> PermissionHandler.setRolePerm(guildId, role, CommandPerm.BOT_ADMIN));

        roles.stream().filter(role -> role.hasPermission(Permission.MANAGE_SERVER))
            .map(ISnowflake::getId)
            .forEach(role -> PermissionHandler.setRolePerm(guildId, role, CommandPerm.BOT_MANAGER));

        Prefixes.addPrefix(guildId, DEFAULT_PREFIX);
    }
}
