package me.zoemartin.rubie.modules.levels;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.annotations.LoadModule;
import me.zoemartin.rubie.core.interfaces.Module;
import me.zoemartin.rubie.core.util.DatabaseUtil;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.jodah.expiringmap.ExpiringMap;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@LoadModule
public class Levels extends ListenerAdapter implements Module {
    private static final Map<String, Map<String, UserLevel>> levels = new ConcurrentHashMap<>();
    private static final Map<String, LevelConfig> configs = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> timeout = new ConcurrentHashMap<>();

    private final Logger log = LoggerFactory.getLogger(Levels.class);

    @Override
    public void init() {
        Bot.addListener(this);
    }

    @Override
    public void initLate() {
        initLevels();
        initConfigs();
    }

    private void initLevels() {
        levels.putAll(DatabaseUtil.loadGroupedMap("from UserLevel", UserLevel.class,
            UserLevel::getGuild_id, UserLevel::getUser_id, Function.identity()));
        levels.forEach((s, stringUserLevelMap) -> log.info(
            "Loaded '{}' levels for '{}'",
            stringUserLevelMap.keySet().size(), s));
    }

    private void initConfigs() {
        configs.putAll(DatabaseUtil.loadMap("from LevelConfig", LevelConfig.class,
            LevelConfig::getGuild_id, Function.identity(), LevelConfig::getGuild_id));
        configs.forEach((s, levelConfig) -> log.info(
            "Loaded config for '{}' with UUID `{}`",
            levelConfig.getGuild_id(),
            levelConfig.getUUID()));
        log.info("Loaded '{}' configuration files", configs.keySet().size());
    }

    @Override
    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
        new Thread(() -> process(event)).start();
    }

    @SuppressWarnings("ConstantConditions")
    private void process(@Nonnull GuildMessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        Guild g = event.getGuild();
        User u = event.getAuthor();
        LevelConfig config = getConfig(g);
        if (!config.isEnabled()) return;
        if (timeout.getOrDefault(g.getId(), Collections.emptySet()).contains(u.getId())) return;
        if (config.getBlacklistedChannels().contains(event.getChannel().getId())) return;
        if (event.getMember().getRoles().stream().anyMatch(role -> config.getBlacklistedRoles().contains(role.getId())))
            return;


        UserLevel level = getUserLevel(g, u);

        int before = calcLevel(level.getExp());
        level.addExp(ThreadLocalRandom.current().nextInt(15, 26));
        int after = calcLevel(level.getExp());

        timeout.computeIfAbsent(g.getId(),
            k -> Collections.newSetFromMap(ExpiringMap.builder().expiration(1, TimeUnit.MINUTES).build()))
            .add(u.getId());

        if (after > before) {
            levelUp(event, after);
        }
        DatabaseUtil.updateObject(level);
    }

    @SuppressWarnings("ConstantConditions")
    private void levelUp(GuildMessageReceivedEvent event, int level) {
        Guild g = event.getGuild();
        Collection<String> roles = getConfig(g).getRewardRoles(level);

        long rewards = roles.stream().filter(s -> {
            Role r = g.getRoleById(s);
            if (r == null) return false;
            return !event.getMember().getRoles().contains(r);
        }).count();

        roles.forEach(s -> {
            Role r = g.getRoleById(s);
            if (r != null) g.addRoleToMember(event.getMember(), r).queue();
        });

        LevelConfig config = getConfig(g);
        String name = event.getMember().getEffectiveName();

        switch (config.getAnnouncements()) {
            case ALL:
                if (rewards > 0)
                    event.getAuthor().openPrivateChannel().complete().sendMessageFormat(
                        "Hey, %s! Congratulations on hitting level %s in %s! " +
                            "Hope you enjoy your new server privileges, and hey, thanks for being here " +
                            "\uD83D\uDC9A",
                        name, level, g.getName()
                    ).queue();
                else
                    event.getAuthor().openPrivateChannel().complete().sendMessageFormat(
                        "Hey, %s! Congratulations on hitting level %s in %s! " +
                            "Thanks for being here! " +
                            "\uD83D\uDC9A",
                        name, level, g.getName()
                    ).queue();
                break;

            case REWARDS:
                if (rewards > 0)
                    event.getAuthor().openPrivateChannel().complete().sendMessageFormat(
                        "Hey, %s! Congratulations on hitting level %s in %s! " +
                            "Hope you enjoy your new server privileges, and hey, thanks for being here " +
                            "\uD83D\uDC9A",
                        name, level, g.getName()
                    ).queue();
                break;
        }

        log.info("User {}/({}) leveled up on {}. Reward Roles {}",
            event.getAuthor().getAsTag(), event.getAuthor().getId(), g, String.join(", ", roles));
    }

    @Nonnull
    public static UserLevel getUserLevel(Guild g, User user) {
        return levels.computeIfAbsent(g.getId(), k -> new ConcurrentHashMap<>()).computeIfAbsent(user.getId(), v -> {
            UserLevel l = new UserLevel(g.getId(), user.getId());
            DatabaseUtil.saveObject(l);
            return l;
        });
    }

    public static Collection<UserLevel> getLevels(Guild g) {
        return Collections.unmodifiableCollection(levels.getOrDefault(g.getId(), Collections.emptyMap()).values());
    }

    public static void importLevel(UserLevel userLevel) {
        Map<String, UserLevel> lvls = levels.computeIfAbsent(userLevel.getGuild_id(),
            k -> new ConcurrentHashMap<>());
        if (lvls.containsKey(userLevel.getUser_id())) DatabaseUtil.deleteObject(lvls.get(userLevel.getUser_id()));
        lvls.put(userLevel.getUser_id(), userLevel);
        DatabaseUtil.saveObject(userLevel);
    }

    public static LevelConfig getConfig(Guild g) {
        return configs.computeIfAbsent(g.getId(), v -> {
            LevelConfig config = new LevelConfig(g.getId(), false);
            DatabaseUtil.saveObject(config);
            return config;
        });
    }

    public static void clearGuildCache(Guild g) {
        if (!levels.containsKey(g.getId())) return;
        levels.get(g.getId()).clear();
    }

    public static int calcLevel(int exp) {
        double x = exp + 1;
        double pow = Math.cbrt(
            Math.sqrt(3) * Math.sqrt(3888.0 * Math.pow(x, 2) + (291600.0 * x) - 207025.0) - 108.0 * x - 4050.0);
        return (int) (-pow / (2.0 * Math.pow(3.0, 2.0 / 3.0) * Math.pow(5.0, 1.0 / 3.0)) -
                          (61.0 * Math.cbrt(5.0 / 3.0)) / (
                              2.0 * pow) - (9.0 / 2.0));
    }

    public static int calcExp(int lvl) {
        return (int) (5.0 / 6.0 * lvl * (2 * Math.pow(lvl, 2) + 27 * lvl + 91));
    }

}

