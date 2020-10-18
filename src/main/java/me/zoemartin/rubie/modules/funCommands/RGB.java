package me.zoemartin.rubie.modules.funCommands;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.exceptions.ReplyError;
import me.zoemartin.rubie.core.exceptions.UnexpectedError;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.Check;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class RGB implements GuildCommand {
    private static final Map<String, GuildSetting> guilds = new ConcurrentHashMap<>();

    @Override
    public void run(Member user, TextChannel channel, List<String> args, Message original, String invoked) {
        Guild g = original.getGuild();
        GuildSetting gs = guilds.computeIfAbsent(g.getId(),
            k -> new GuildSetting(false, null));

        Role role;
        if (gs.roleId == null || g.getRoleById(gs.roleId) == null)
            role = g.getSelfMember().getRoles()
                       .stream()
                       .filter(Role::isManaged)
                       .findAny()
                       .orElseThrow(UnexpectedError::new);
        else role = g.getRoleById(gs.roleId);
        Check.entityNotNull(role, Role.class);
        Check.check(g.getSelfMember().getRoles().get(0) != role,
            () -> new ReplyError("I cannot edit my highest Role"));
        gs.isEnabled = !gs.isEnabled;

        if (gs.isEnabled) {
            gs.service = new ScheduledThreadPoolExecutor(1);
            ListIterator<Integer> i = List.of(0x55CDFC, 0xF7A8D8, 0xFFFFFF, 0xF7A8D8, 0x55CDFC).listIterator();
            gs.service.scheduleAtFixedRate(() -> {
                Random rand = new Random();
                role.getManager().setColor(new Color(rand.nextFloat(), rand.nextFloat(), rand.nextFloat())).queue();
            }, 0, 1, TimeUnit.SECONDS);
        } else {
            gs.service.shutdown();
            gs.service = null;
        }

        addCheckmark(original);
    }

    @NotNull
    @Override
    public String name() {
        return "rgb";
    }

    @NotNull
    @Override
    public CommandPerm commandPerm() {
        return CommandPerm.BOT_ADMIN;
    }

    @NotNull
    @Override
    public String description() {
        return "RGBifys the bot";
    }

    private static class GuildSetting {
        boolean isEnabled;
        String roleId;
        ScheduledExecutorService service;

        public GuildSetting(boolean isEnabled, String roleId) {
            this.isEnabled = isEnabled;
            this.roleId = roleId;
            this.service = null;
        }
    }
}
