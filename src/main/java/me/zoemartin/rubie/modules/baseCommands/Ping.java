package me.zoemartin.rubie.modules.baseCommands;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.interfaces.Command;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Ping implements Command {
    @Override
    public @NotNull String name() {
        return "ping";
    }

    @Override
    public void run(User user, MessageChannel channel, List<String> args, Message original, String invoked) {
        long time = System.currentTimeMillis();
        Message m = channel.sendMessage("Pong!").complete();

        m.editMessage("Ping: " + (m.getTimeCreated().toInstant().toEpochMilli() - time)
                          + "ms | Websocket: " + Bot.getJDA().getGatewayPing() + "ms").queue();
    }

    @Override
    public @NotNull CommandPerm commandPerm() {
        return CommandPerm.EVERYONE;
    }

    @Override
    public @NotNull String description() {
        return "table tennis?";
    }
}
