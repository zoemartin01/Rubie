package me.zoemartin.rubie.modules.reactroles;

import me.zoemartin.rubie.core.util.CollectorsUtil;
import me.zoemartin.rubie.core.util.DatabaseUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReactManager {
    private static final Map<String, Map<String, Set<ReactRole>>> reactRoles = new ConcurrentHashMap<>();
    private static final Map<String, ReactConfig> configs = new ConcurrentHashMap<>();

    private static final Logger log = LoggerFactory.getLogger(ReactManager.class);

    public static void init() {
        reactRoles.putAll(DatabaseUtil.loadCollection(
            "from ReactRole", ReactRole.class, Function.identity())
                              .stream()
                              .collect(Collectors.groupingBy(ReactRole::getGuildId,
                                  Collectors.groupingBy(ReactRole::getMessageId, CollectorsUtil.toConcurrentSet()))));

        log.info("Loaded {} react roles", reactRoles.values().stream()
                                              .flatMap(stringSetMap -> stringSetMap.values().stream())
                                              .mapToLong(Collection::size).sum());

        configs.putAll(DatabaseUtil.loadMap("from ReactConfig", ReactConfig.class,
            ReactConfig::getGuildId, Function.identity()));

        log.info("Loaded {} react configs", configs.size());
    }

    public static ReactConfig getConfig(Guild g) {
        return configs.computeIfAbsent(g.getId(), k -> {
            var config = new ReactConfig(g.getId());
            DatabaseUtil.saveObject(config);
            return config;
        });
    }

    public static Map<String, Set<ReactRole>> getReactRoles(Guild g) {
        return reactRoles.getOrDefault(g.getId(), Collections.emptyMap());
    }

    public static Collection<ReactRole> forMessage(Message message) {
        return forMessage(message.getGuild().getId(), message.getId());
    }

    public static Collection<ReactRole> forMessage(GenericGuildMessageReactionEvent event) {
        return forMessage(event.getGuild().getId(), event.getMessageId());
    }

    private static Collection<ReactRole> forMessage(String guildId, String messageId) {
        return reactRoles.getOrDefault(guildId, Collections.emptyMap()).getOrDefault(messageId, Collections.emptySet());
    }

    public static void addReactRole(ReactRole reactRole) {
        if (reactRoles.computeIfAbsent(reactRole.getGuildId(), k -> new ConcurrentHashMap<>())
                .computeIfAbsent(reactRole.getMessageId(), v -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                .add(reactRole)) DatabaseUtil.saveObject(reactRole);
    }

    public static void removeReactRole(ReactRole reactRole) {
        if (reactRoles.getOrDefault(reactRole.getGuildId(), new ConcurrentHashMap<>())
                .getOrDefault(reactRole.getMessageId(), new HashSet<>())
                .remove(reactRole)) DatabaseUtil.deleteObject(reactRole);
    }
}
