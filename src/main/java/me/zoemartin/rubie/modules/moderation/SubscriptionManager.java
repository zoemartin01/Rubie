package me.zoemartin.rubie.modules.moderation;

import me.zoemartin.rubie.core.util.CollectorsUtil;
import me.zoemartin.rubie.core.util.DatabaseUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static me.zoemartin.rubie.modules.moderation.Subscription.Event;

public class SubscriptionManager {
    private static final Map<Event, Collection<Subscription>> subs = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(SubscriptionManager.class);

    public static void init() {
        subs.putAll(DatabaseUtil.loadGroupedCollection("from Subscription", Subscription.class,
            Subscription::getEvent,
            Function.identity(),
            CollectorsUtil.toConcurrentSet()));

        subs.forEach((event, subscriptions) -> log.info("Loaded {} subscriptions for event '{}'",
            subscriptions.size(), event.toString()));
    }

    public static Collection<Subscription> getMemberSubscriptions(Member member, Event... events) {
        if (events.length == 0)
            return subs.values().stream()
                       .flatMap(Collection::stream)
                       .filter(sub -> sub.getGuildId().equals(member.getGuild().getId()) && sub.getUserId().equals(member.getId()))
                       .collect(Collectors.toSet());

        return subs.entrySet().stream()
                   .filter(e -> Arrays.stream(events).anyMatch(ev -> ev.equals(e.getKey())))
                   .flatMap(eventsSetEntry -> eventsSetEntry.getValue().stream())
                   .filter(sub -> sub.getGuildId().equals(member.getGuild().getId()) && sub.getUserId().equals(member.getId()))
                   .collect(Collectors.toSet());
    }

    public static void addSubscription(Subscription subscription) {
        subs.computeIfAbsent(subscription.getEvent(), k -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
            .add(subscription);
        DatabaseUtil.saveObject(subscription);
    }

    public static void removeSubscription(Subscription subscription) {
        subs.computeIfAbsent(subscription.getEvent(), k -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
            .remove(subscription);
        DatabaseUtil.deleteObject(subscription);
    }

    public static Collection<Subscription> getGuildEvents(Guild guild, Event... events) {
        return subs.entrySet().stream()
                   .filter(e -> Arrays.stream(events).anyMatch(ev -> ev.equals(e.getKey())))
                   .flatMap(eventsSetEntry -> eventsSetEntry.getValue().stream())
                   .filter(sub -> sub.getGuildId().equals(guild.getId()))
                   .collect(Collectors.toSet());
    }

    public static Collection<Subscription> getGlobalEvents(Event... events) {
        return subs.entrySet().stream()
                   .filter(e -> Arrays.stream(events).anyMatch(ev -> ev.equals(e.getKey())))
                   .flatMap(eventsSetEntry -> eventsSetEntry.getValue().stream())
                   .collect(Collectors.toSet());
    }
}
