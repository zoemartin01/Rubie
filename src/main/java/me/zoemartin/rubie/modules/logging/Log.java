package me.zoemartin.rubie.modules.logging;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.interfaces.Module;
import me.zoemartin.rubie.core.managers.CommandManager;
import me.zoemartin.rubie.core.util.DatabaseUtil;
import org.hibernate.Session;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class Log implements Module {
    @Override
    public void init() {
        CommandManager.setCommandLogger(new Logger());
        Bot.addListener(new Delete());
    }

    @Override
    public void initLate() {
        //clearMessageLog();
    }

    /**
     * Clears Database of Messages older then 1 week
     */
    private void clearMessageLog() {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        executor.scheduleAtFixedRate(() -> {
            try (Session session = DatabaseUtil.getSessionFactory().openSession()) {
                List<LMessage> load = session.createQuery("from LMessage", LMessage.class).list();
                load.stream().filter(lMessage -> Instant.now().minus(1, ChronoUnit.MINUTES)
                                                     .isBefore(Instant.ofEpochSecond(lMessage.getTimestamp())))
                    .forEach(DatabaseUtil::deleteObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.HOURS);
    }
}
