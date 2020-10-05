package me.zoemartin.rubie.modules.logging;

import me.zoemartin.rubie.core.interfaces.CommandLogger;
import me.zoemartin.rubie.core.util.DatabaseUtil;
import net.dv8tion.jda.api.entities.Message;

public class Logger implements CommandLogger {
    @Override
    public void log(Message message) {
        LMessage m = new LMessage(message);
        DatabaseUtil.saveObject(m);
    }
}
