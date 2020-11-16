package me.zoemartin.rubie.modules.baseCommands;

import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.annotations.Command;
import me.zoemartin.rubie.core.annotations.CommandOptions;
import me.zoemartin.rubie.core.exceptions.CommandArgumentException;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import java.time.Instant;
import java.time.OffsetDateTime;

@Command
@CommandOptions(
    name = "roleinfo",
    description = "Shows information about a Role",
    usage = "<role>",
    perm = CommandPerm.BOT_MODERATOR
)
public class RoleInfo extends GuildCommand {
    // TODO: Refactor into ROLE Command
    @Override
    public void run(GuildCommandEvent event) {
        Check.check(!event.getArgs().isEmpty(), CommandArgumentException::new);

        String roleRef = lastArg(0, event);
        Role role = Parser.Role.getRole(event.getGuild(), roleRef);
        Check.entityReferenceNotNull(role, Role.class, roleRef);

        EmbedBuilder eb =
            new EmbedBuilder()
                .addField("Name", role.getName(), true)
                .addField("ID", role.getId(), true)
                .addField("Color", role.getColor() != null ? "#" + Integer.toHexString(
                    role.getColor().getRGB()).substring(2) : "n/a", true)
                .addField("Mention", role.getAsMention(), true)
                .addField("Member Count", String.valueOf(role.getGuild().getMembersWithRoles(role).size()), true)
                .addField("Position", String.valueOf(role.getPositionRaw()), true)
                .addField("Hoisted", String.valueOf(role.isHoisted()), true)
                .addField("Mentionable", String.valueOf(role.isMentionable()), true)
                .addField("Created ago", TimeUtils.dateAgo(role.getTimeCreated(), OffsetDateTime.now()), false)

                .setFooter("ID: " + role.getId())
                .setTimestamp(Instant.now())
                .setColor(role.getColor());

        event.getChannel().sendMessage(eb.build()).queue();
    }
}
