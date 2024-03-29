package me.zoemartin.rubie;

import me.zoemartin.rubie.core.managers.JobManager;
import me.zoemartin.rubie.core.managers.ModuleManager;
import me.zoemartin.rubie.core.util.DatabaseUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.utils.Compression;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;
import java.io.*;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Properties;

public class Bot extends ListenerAdapter {
    private static Properties properties;
    private static File configFile;

    private static JDABuilder builder;
    private static JDA jda = null;
    private static final String OWNER = "212591138945630213";

    private static int exitCode = 0;

    private static final Logger log = LoggerFactory.getLogger(Bot.class);

    public static void main(String[] args) throws LoginException {
        System.setProperty("logFilename", "rubie_" + DateTime.now().toString("yyyy-MM-dd_HH-mm-ss") + ".log");

        properties = new Properties();

        if (args.length == 0) {
            configFile = new File(".env");
        } else {
            configFile = new File(args[0]);
        }

        if (!configFile.exists()) {
            log.error("Env Configuration file does not exist!");
        }

        reloadConfig();

        builder = JDABuilder.createDefault(properties.getProperty("bot.token"));

        ModuleManager.init();

        Configuration config = new Configuration();
        Properties settings = new Properties();

        settings.put(Environment.DRIVER, "org.postgresql.Driver");
        settings.put(Environment.URL, properties.getProperty("database.url"));
        settings.put(Environment.USER, properties.getProperty("database.username"));
        settings.put(Environment.PASS, properties.getProperty("database.password"));
        settings.put(Environment.DIALECT, "org.hibernate.dialect.PostgreSQL82Dialect");
        settings.put(Environment.POOL_SIZE, 10);
        settings.put(Environment.SHOW_SQL, false);
        settings.put(Environment.HBM2DDL_AUTO, "update");
        config.setProperties(settings);

        DatabaseUtil.setConfig(config);

        ModuleManager.initLate();

        builder.enableIntents(GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES);
        builder.setMemberCachePolicy(MemberCachePolicy.ALL);
        builder.setBulkDeleteSplittingEnabled(false);
        builder.setCompression(Compression.NONE);
        builder.addEventListeners(new Bot());

        EnumSet<Message.MentionType> deny = EnumSet.of(Message.MentionType.EVERYONE, Message.MentionType.HERE,
            Message.MentionType.ROLE);
        MessageAction.setDefaultMentions(EnumSet.complementOf(deny));

        jda = builder.build();
    }

    public static Properties getProperties() {
        return properties;
    }

    public static void addListener(Object... listeners) {
        builder.addEventListeners(listeners);
    }

    public static JDA getJDA() {
        return jda;
    }

    public static String getOWNER() {
        return OWNER;
    }

    public static void shutdownWithCode(int code, boolean force) {
        exitCode = code;
        System.out.println(exitCode);
        if (force) jda.shutdownNow();
        else jda.shutdown();
    }

    public static void reloadConfig() {
        try {
            properties.load(new FileReader(configFile));
        } catch (IOException e) {
            log.error("Error loading configuration file!");
        }
    }

    @Override
    public void onShutdown(@Nonnull ShutdownEvent event) {
        System.exit(exitCode);
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        JobManager.init();
    }
}
