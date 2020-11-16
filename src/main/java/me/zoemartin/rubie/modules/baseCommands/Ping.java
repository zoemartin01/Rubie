package me.zoemartin.rubie.modules.baseCommands;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.CommandEvent;
import me.zoemartin.rubie.core.annotations.Command;
import me.zoemartin.rubie.core.annotations.CommandOptions;
import me.zoemartin.rubie.core.interfaces.AbstractCommand;
import net.dv8tion.jda.api.entities.*;

@Command
@CommandOptions(name = "ping", description = "Shows the Bot's Latency")
public class Ping extends AbstractCommand {
    @Override
    public void run(CommandEvent event) {
        long time = System.currentTimeMillis();
        Message m = event.getChannel().sendMessage("Pong!").complete();

        m.editMessage("Ping: " + (m.getTimeCreated().toInstant().toEpochMilli() - time)
                          + "ms | Websocket: " + Bot.getJDA().getGatewayPing() + "ms").queue();
    }
}
