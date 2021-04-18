package me.zoemartin.rubie.modules.moderation;

import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.annotations.*;
import me.zoemartin.rubie.core.exceptions.CommandArgumentException;
import me.zoemartin.rubie.core.exceptions.ReplyError;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.*;
import me.zoemartin.rubie.modules.pagedEmbeds.PageListener;
import me.zoemartin.rubie.modules.pagedEmbeds.PagedEmbed;
import net.dv8tion.jda.annotations.Incubating;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.member.*;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.guild.voice.*;
import net.dv8tion.jda.api.events.message.guild.*;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.api.events.user.update.*;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

import static me.zoemartin.rubie.modules.moderation.Subscription.Event.*;
import static me.zoemartin.rubie.modules.moderation.Subscription.Keys.CHANNEL;
import static me.zoemartin.rubie.modules.moderation.Subscription.Keys.ROLE;

@Command
@CommandOptions(
    name = "subscribe",
    alias = "sub",
    description = "Subscribe to user events",
    usage = "<user> <event id/name> [param]",
    help = "List of valid events: `subscribe events`",
    perm = CommandPerm.BOT_MANAGER
)
public class Subscribe extends GuildCommand {
    @Override
    public void run(GuildCommandEvent event) {
        var args = event.getArgs();
        Check.check(args.size() > 1, CommandArgumentException::new);
        var guild = event.getGuild();

        var uRef = args.get(0);
        var user = CacheUtils.getUser(Parser.User.parse(uRef));
        Check.entityReferenceNotNull(user, User.class, uRef);

        var eRef = args.get(1);
        var subEvent = eRef.matches("\\d+") ? Subscription.Event.fromNum(Parser.Int.parse(eRef))
                           : Subscription.Event.fromString(eRef);
        Check.check(subEvent != null, () -> new ReplyError("`%s` not a valid event", eRef));

        var settings = new HashMap<String, String>();
        switch (subEvent) {
            case ROLE_ADD, ROLE_REMOVE -> {
                Check.check(args.size() == 3, CommandArgumentException::new);
                var rRef = args.get(2);
                var role = Parser.Role.getRole(guild, rRef);
                Check.entityReferenceNotNull(role, Role.class, rRef);

                settings.put(ROLE.raw(), role.getId());
            }
            case MESSAGE_ACTIVITY, MESSAGE_SENT, MESSAGE_EDIT, MESSAGE_DELETED -> {
                if (args.size() <= 2) break;
                Check.check(args.size() == 3, CommandArgumentException::new);
                var cRef = args.get(2);
                var channel = Parser.Channel.getTextChannel(guild, cRef);
                Check.entityReferenceNotNull(channel, TextChannel.class, cRef);

                settings.put(CHANNEL.raw(), channel.getId());
            }
            case VOICE_UPDATE, VOICE_JOIN, VOICE_LEAVE, VOICE_MUTE, VOICE_DEAFEN, VOICE_STREAM -> {
                if (args.size() <= 2) break;
                Check.check(args.size() == 3, CommandArgumentException::new);
                var cRef = args.get(2);
                var channel = Parser.Channel.getVoiceChannel(guild, cRef);
                Check.entityReferenceNotNull(channel, VoiceChannel.class, cRef);

                settings.put(CHANNEL.raw(), channel.getId());
            }
        }

        var sub = new Subscription(event.getGuild().getId(), event.getUser().getId(), user.getId(),
            subEvent, settings);

        SubscriptionManager.addSubscription(sub);
        event.addCheckmark();
    }

    @SubCommand(Subscribe.class)
    @CommandOptions(
        name = "onetime",
        alias = "sub",
        description = "Creates a one time subscription of a user event",
        usage = "<user> <event id/name> [param]",
        help = "List of valid events: `subscribe events`",
        perm = CommandPerm.BOT_MANAGER
    )
    @SubCommand.AsBase(name = "notify")
    private static class OneTime extends GuildCommand {
        @Override
        public void run(GuildCommandEvent event) {
            var args = event.getArgs();
            Check.check(args.size() > 1, CommandArgumentException::new);
            var guild = event.getGuild();

            var uRef = args.get(0);
            var user = CacheUtils.getUser(Parser.User.parse(uRef));
            Check.entityReferenceNotNull(user, User.class, uRef);

            var eRef = args.get(1);
            var subEvent = eRef.matches("\\d+") ? Subscription.Event.fromNum(Parser.Int.parse(eRef))
                               : Subscription.Event.fromString(eRef);
            Check.check(subEvent != null, () -> new ReplyError("`%s` not a valid event", eRef));

            var settings = new HashMap<String, String>();
            switch (subEvent) {
                case ROLE_ADD, ROLE_REMOVE -> {
                    Check.check(args.size() == 3, CommandArgumentException::new);
                    var rRef = args.get(2);
                    var role = Parser.Role.getRole(guild, rRef);
                    Check.entityReferenceNotNull(role, Role.class, rRef);

                    settings.put(ROLE.raw(), role.getId());
                }
                case MESSAGE_ACTIVITY, MESSAGE_SENT, MESSAGE_EDIT, MESSAGE_DELETED -> {
                    if (args.size() <= 2) break;
                    Check.check(args.size() == 3, CommandArgumentException::new);
                    var cRef = args.get(2);
                    var channel = Parser.Channel.getTextChannel(guild, cRef);
                    Check.entityReferenceNotNull(channel, TextChannel.class, cRef);

                    settings.put(CHANNEL.raw(), channel.getId());
                }
                case VOICE_UPDATE, VOICE_JOIN, VOICE_LEAVE, VOICE_MUTE, VOICE_DEAFEN, VOICE_STREAM -> {
                    if (args.size() <= 2) break;
                    Check.check(args.size() == 3, CommandArgumentException::new);
                    var cRef = args.get(2);
                    var channel = Parser.Channel.getVoiceChannel(guild, cRef);
                    Check.entityReferenceNotNull(channel, VoiceChannel.class, cRef);

                    settings.put(CHANNEL.raw(), channel.getId());
                }
            }

            var sub = new Subscription(event.getGuild().getId(), event.getUser().getId(), user.getId(),
                subEvent, settings, true);

            SubscriptionManager.addSubscription(sub);
            event.addCheckmark();
        }
    }

    @SubCommand(Subscribe.class)
    @CommandOptions(
        name = "list",
        description = "List your subscriptions",
        usage = "[event... name/id]",
        perm = CommandPerm.BOT_MANAGER
    )
    private static class list extends GuildCommand {
        @Override
        public void run(GuildCommandEvent event) {
            Subscription.Event[] events = event.getArgs().stream()
                                              .map(s -> s.matches("\\d+") ? Subscription.Event.fromNum(Parser.Int.parse(s))
                                                            : Subscription.Event.fromString(s))
                                              .filter(Objects::nonNull)
                                              .toArray(Subscription.Event[]::new);
            var jda = event.getJDA();

            var subs = SubscriptionManager.getMemberSubscriptions(event.getMember(), events)
                           .stream().sorted(Comparator.comparing(Subscription::getId)).collect(Collectors.toList());
            var paged = new PagedEmbed(
                EmbedUtil.pagedDescription(
                    new EmbedBuilder().setDescription("Your Subscriptions").setColor(event.getMember().getColor()).build(),
                    subs.stream()
                        .map(sub -> {
                            var target = jda.getUserById(sub.getTargetId());
                            if (target == null)
                                return String.format("%d. - `[%d] %s` - ~~User: %s~~ - Parameters: %s\n\n",
                                    subs.indexOf(sub) + 1, sub.getEvent().id(), sub.getEvent().toString(), sub.getTargetId(),
                                    sub.getSettings().entrySet().stream()
                                        .map(entry -> entry.getKey() + " " + entry.getValue())
                                        .collect(Collectors.joining(", ")));
                            return String.format("%d. - `[%d] %s` - User: %s (%s) - Parameters: %s\n\n",
                                subs.indexOf(sub) + 1, sub.getEvent().id(), sub.getEvent().toString(), target.getAsMention(), target.getId(),
                                sub.getSettings().entrySet().stream()
                                    .map(entry -> entry.getKey() + " " + entry.getValue())
                                    .collect(Collectors.joining(", ")));
                        }).collect(Collectors.toList())
                ), event);

            PageListener.add(paged);
        }
    }

    @SubCommand(Subscribe.class)
    @CommandOptions(
        name = "remove",
        description = "Remove a subscription",
        usage = "<index> [events... name/id]",
        perm = CommandPerm.BOT_MANAGER
    )
    private static class Remove extends GuildCommand {
        @Override
        public void run(GuildCommandEvent event) {
            var args = event.getArgs();
            Check.check(!args.isEmpty(), CommandArgumentException::new);
            Subscription.Event[] events = args.stream()
                                              .filter(s -> args.indexOf(s) != 0)
                                              .map(s -> s.matches("\\d+") ? Subscription.Event.fromNum(Parser.Int.parse(s))
                                                            : Subscription.Event.fromString(s))
                                              .filter(Objects::nonNull)
                                              .toArray(Subscription.Event[]::new);

            var subs = SubscriptionManager.getMemberSubscriptions(event.getMember(), events)
                           .stream().sorted(Comparator.comparing(Subscription::getId)).collect(Collectors.toList());

            var ind = Parser.Int.parse(args.get(0)) - 1;
            Check.check(ind >= 0 && ind < subs.size(), () -> new ReplyError("Not a valid index for the specified events"));

            SubscriptionManager.removeSubscription(subs.get(ind));
            event.addCheckmark();
        }
    }

    @SubCommand(Subscribe.class)
    @CommandOptions(
        name = "clear",
        description = "Remove all your subscriptions",
        usage = "[events... name/id]",
        perm = CommandPerm.BOT_MANAGER
    )
    private static class Clear extends GuildCommand {
        @Override
        public void run(GuildCommandEvent event) {
            Subscription.Event[] events = event.getArgs().stream()
                                              .map(s -> s.matches("\\d+") ? Subscription.Event.fromNum(Parser.Int.parse(s))
                                                            : Subscription.Event.fromString(s))
                                              .filter(Objects::nonNull)
                                              .toArray(Subscription.Event[]::new);

            SubscriptionManager.getMemberSubscriptions(event.getMember(), events)
                .forEach(SubscriptionManager::removeSubscription);
            event.addCheckmark();
        }
    }

    @SubCommand(Subscribe.class)
    @CommandOptions(
        name = "events",
        description = "Shows all valid events",
        perm = CommandPerm.BOT_MANAGER
    )
    private static class Events extends GuildCommand {
        @Override
        public void run(GuildCommandEvent event) {
            var events = new PagedEmbed(
                EmbedUtil.pagedDescription(
                    new EmbedBuilder().setTitle("Valid Events")
                        .setColor(event.getGuild().getSelfMember().getColor()).build(),
                    List.of(
                        String.format("`[%d] - %s - %s`\n", UPDATE_APPEARANCE.id(), UPDATE_APPEARANCE.toString(), "Includes [1], [2], [3], [4], [5]"),
                        String.format("`[%d] - %s - %s`\n", UPDATE_NAME.id(), UPDATE_NAME.toString(), "Includes [2], [3]"),
                        String.format("`[%d] - %s`\n", UPDATE_NICKNAME.id(), UPDATE_NICKNAME.toString()),
                        String.format("`[%d] - %s`\n", UPDATE_USERNAME.id(), UPDATE_USERNAME.toString()),
                        String.format("`[%d] - %s`\n\n", UPDATE_AVATAR.id(), UPDATE_AVATAR.toString()),
                        //String.format("`[%d] - %s`\n", UPDATE_STATUS.id(), UPDATE_STATUS.toString()),
                        String.format("`[%d] - %s`\n", MEMBER_JOIN.id(), MEMBER_JOIN.toString()),
                        String.format("`[%d] - %s`\n\n", MEMBER_LEAVE.id(), MEMBER_LEAVE.toString()),
                        String.format("`[%d] - %s - %s`\n", ROLE_ADD.id(), ROLE_ADD.toString(), "Needed parameter: role"),
                        String.format("`[%d] - %s - %s`\n\n", ROLE_REMOVE.id(), ROLE_REMOVE.toString(), "Needed parameter: role"),
                        String.format("`[%d] - %s - %s`\n", MESSAGE_ACTIVITY.id(), MESSAGE_ACTIVITY.toString(), "Optional parameter: channel | Includes [31], [32], [33]"),
                        String.format("`[%d] - %s - %s`\n", MESSAGE_SENT.id(), MESSAGE_SENT.toString(), "Optional parameter: channel"),
                        //String.format("`[%d] - %s - %s`\n", MESSAGE_DELETED.id(), MESSAGE_DELETED.toString(), "Optional parameter: channel"),
                        String.format("`[%d] - %s - %s`\n\n", MESSAGE_EDIT.id(), MESSAGE_EDIT.toString(), "Optional parameter: channel"),
                        String.format("`[%d] - %s`\n", REACTION_ADD.id(), REACTION_ADD.toString()),
                        String.format("`[%d] - %s`\n\n", REACTION_REMOVE.id(), REACTION_REMOVE.toString()),
                        String.format("`[%d] - %s - %s`\n", VOICE_UPDATE.id(), VOICE_UPDATE.toString(), "Optional parameter: voice channel | Includes [51], [52], [53], [54], [55]"),
                        String.format("`[%d] - %s - %s`\n", VOICE_LEAVE.id(), VOICE_LEAVE.toString(), "Optional parameter: channel"),
                        String.format("`[%d] - %s - %s`\n", VOICE_JOIN.id(), VOICE_JOIN.toString(), "Optional parameter: channel"),
                        String.format("`[%d] - %s - %s`\n", VOICE_MUTE.id(), VOICE_MUTE.toString(), "Optional parameter: channel"),
                        String.format("`[%d] - %s - %s`\n", VOICE_DEAFEN.id(), VOICE_DEAFEN.toString(), "Optional parameter: channel"),
                        String.format("`[%d] - %s - %s`\n", VOICE_STREAM.id(), VOICE_STREAM.toString(), "Optional parameter: channel")

                    )
                ), event
            );

            PageListener.add(events);
        }
    }

    @SuppressWarnings("ConstantConditions")
    public static class Listeners extends ListenerAdapter {
        private static final String TITLE = "An event you subscribed to just occurred";

        // UpdateName
        public void onUserUpdateName(@Nonnull UserUpdateNameEvent event) {
            var subs = SubscriptionManager.getGlobalEvents(UPDATE_APPEARANCE, UPDATE_NAME, UPDATE_USERNAME);
            if (subs.isEmpty()) return;

            var target = event.getUser();
            subs.removeIf(sub -> !sub.getTargetId().equals(target.getId()));
            if (subs.isEmpty()) return;

            var jda = event.getJDA();
            subs.removeIf(sub -> jda.getUserById(sub.getUserId()) == null);
            subs.removeIf(sub -> jda.getGuilds().stream().map(ISnowflake::getId).noneMatch(s -> s.equals(sub.getGuildId())));
            subs.removeIf(sub -> jda.getUsers().stream().map(ISnowflake::getId).noneMatch(s -> s.equals(sub.getUserId())));

            // Removes all subs where there are no mutual guilds between user and target
            subs.removeIf(sub -> {
                var user = jda.getUserById(sub.getUserId());
                return jda.getGuilds().stream().noneMatch(guild -> guild.getMembers().containsAll(List.of(user, target)));
            });
            if (subs.isEmpty()) return;

            subs.forEach(sub -> {
                var user = jda.getUserById(sub.getUserId());
                var ch = user.openPrivateChannel().complete();

                var eb = new EmbedBuilder()
                             .setTitle(TITLE)
                             .setFooter(String.format("%s#%s (%s)", target.getName(), target.getDiscriminator(), target.getId()),
                                 target.getEffectiveAvatarUrl())
                             .setDescription(String.format("%s just changed their username from `%s` to `%s`",
                                 target.getAsMention(), event.getOldValue(), event.getNewValue()))
                             .build();

                ch.sendMessage(eb).queue();

                if (sub.isOneTime()) SubscriptionManager.removeSubscription(sub);
            });
        }

        public void onUserUpdateDiscriminator(@Nonnull UserUpdateDiscriminatorEvent event) {
            var subs = SubscriptionManager.getGlobalEvents(UPDATE_APPEARANCE, UPDATE_NAME, UPDATE_USERNAME);
            if (subs.isEmpty()) return;

            var target = event.getUser();
            subs.removeIf(sub -> !sub.getTargetId().equals(target.getId()));
            if (subs.isEmpty()) return;

            var jda = event.getJDA();
            subs.removeIf(sub -> jda.getUserById(sub.getUserId()) == null);
            subs.removeIf(sub -> jda.getGuilds().stream().map(ISnowflake::getId).noneMatch(s -> s.equals(sub.getGuildId())));
            subs.removeIf(sub -> jda.getUsers().stream().map(ISnowflake::getId).noneMatch(s -> s.equals(sub.getUserId())));

            // Removes all subs where there are no mutual guilds between user and target
            subs.removeIf(sub -> {
                var user = jda.getUserById(sub.getUserId());
                return jda.getGuilds().stream().noneMatch(guild -> guild.getMembers().containsAll(List.of(user, target)));
            });
            if (subs.isEmpty()) return;

            subs.forEach(sub -> {
                var user = jda.getUserById(sub.getUserId());
                var ch = user.openPrivateChannel().complete();

                var eb = new EmbedBuilder()
                             .setTitle(TITLE)
                             .setFooter(String.format("%s#%s (%s)", target.getName(), target.getDiscriminator(), target.getId()),
                                 target.getEffectiveAvatarUrl())
                             .setDescription(String.format("%s just changed their username from `%s` to `%s`",
                                 target.getAsMention(), event.getOldValue(), event.getNewValue()))
                             .build();

                ch.sendMessage(eb).queue();

                if (sub.isOneTime()) SubscriptionManager.removeSubscription(sub);
            });
        }

        public void onGuildMemberUpdateNickname(@Nonnull GuildMemberUpdateNicknameEvent event) {
            var subs = SubscriptionManager.getGuildEvents(event.getGuild(),
                UPDATE_APPEARANCE, UPDATE_NAME, UPDATE_NICKNAME);
            if (subs.isEmpty()) return;

            var target = event.getUser();
            subs.removeIf(sub -> !sub.getTargetId().equals(target.getId()));
            if (subs.isEmpty()) return;

            var guild = event.getGuild();
            subs.removeIf(sub -> guild.getMemberById(sub.getUserId()) == null);
            if (subs.isEmpty()) return;

            subs.forEach(sub -> {
                var user = event.getMember();
                var ch = user.getUser().openPrivateChannel().complete();

                var eb = new EmbedBuilder()
                             .setTitle(TITLE)
                             .setFooter(String.format("%s#%s (%s)", target.getName(), target.getDiscriminator(), target.getId()),
                                 target.getEffectiveAvatarUrl())
                             .setDescription(String.format("%s just changed their nickname on *%s* from `%s` to `%s`",
                                 target.getAsMention(), guild.getName(), event.getOldValue(), event.getNewValue()))
                             .build();

                ch.sendMessage(eb).queue();

                if (sub.isOneTime()) SubscriptionManager.removeSubscription(sub);
            });
        }

        // UpdateAvatar
        public void onUserUpdateAvatar(@Nonnull UserUpdateAvatarEvent event) {
            var subs = SubscriptionManager.getGlobalEvents(UPDATE_APPEARANCE, UPDATE_AVATAR);
            if (subs.isEmpty()) return;

            var target = event.getUser();
            subs.removeIf(sub -> !sub.getTargetId().equals(target.getId()));
            if (subs.isEmpty()) return;

            var jda = event.getJDA();
            subs.removeIf(sub -> jda.getUserById(sub.getUserId()) == null);
            subs.removeIf(sub -> jda.getGuilds().stream().map(ISnowflake::getId).noneMatch(s -> s.equals(sub.getGuildId())));
            subs.removeIf(sub -> jda.getUsers().stream().map(ISnowflake::getId).noneMatch(s -> s.equals(sub.getUserId())));

            // Removes all subs where there are no mutual guilds between user and target
            subs.removeIf(sub -> {
                var user = jda.getUserById(sub.getUserId());
                return jda.getGuilds().stream().noneMatch(guild -> guild.getMembers().containsAll(List.of(user, target)));
            });
            if (subs.isEmpty()) return;

            subs.forEach(sub -> {
                var user = jda.getUserById(sub.getUserId());
                var ch = user.openPrivateChannel().complete();

                var eb = new EmbedBuilder()
                             .setTitle(TITLE)
                             .setFooter(String.format("%s#%s (%s)", target.getName(), target.getDiscriminator(), target.getId()),
                                 target.getEffectiveAvatarUrl())
                             .setDescription(String.format("%s just changed their username from `%s` to `%s`",
                                 target.getAsMention(), event.getOldValue(), event.getNewValue()))
                             .build();

                ch.sendMessage(eb).queue();

                if (sub.isOneTime()) SubscriptionManager.removeSubscription(sub);
            });
        }

        // Join/Leave
        public void onGuildMemberJoin(@Nonnull GuildMemberJoinEvent event) {
            var subs = SubscriptionManager.getGuildEvents(event.getGuild(), MEMBER_JOIN);
            if (subs.isEmpty()) return;

            var target = event.getUser();
            subs.removeIf(sub -> !sub.getTargetId().equals(target.getId()));
            if (subs.isEmpty()) return;

            var guild = event.getGuild();
            subs.removeIf(sub -> guild.getMemberById(sub.getUserId()) == null);
            if (subs.isEmpty()) return;

            subs.forEach(sub -> {
                var user = event.getMember();
                var ch = user.getUser().openPrivateChannel().complete();

                var eb = new EmbedBuilder()
                             .setTitle(TITLE)
                             .setFooter(String.format("%s#%s (%s)", target.getName(), target.getDiscriminator(), target.getId()),
                                 target.getEffectiveAvatarUrl())
                             .setDescription(String.format("%s just joined *%s*",
                                 target.getAsMention(), guild.getName()))
                             .build();

                ch.sendMessage(eb).queue();

                if (sub.isOneTime()) SubscriptionManager.removeSubscription(sub);
            });
        }

        public void onGuildMemberRemove(@Nonnull GuildMemberRemoveEvent event) {
            var subs = SubscriptionManager.getGuildEvents(event.getGuild(), MEMBER_LEAVE);
            if (subs.isEmpty()) return;

            var target = event.getUser();
            subs.removeIf(sub -> !sub.getTargetId().equals(target.getId()));
            if (subs.isEmpty()) return;

            var guild = event.getGuild();
            subs.removeIf(sub -> guild.getMemberById(sub.getUserId()) == null);
            if (subs.isEmpty()) return;

            subs.forEach(sub -> {
                var user = event.getMember();
                var ch = user.getUser().openPrivateChannel().complete();

                var eb = new EmbedBuilder()
                             .setTitle(TITLE)
                             .setFooter(String.format("%s#%s (%s)", target.getName(), target.getDiscriminator(), target.getId()),
                                 target.getEffectiveAvatarUrl())
                             .setDescription(String.format("%s just left *%s*",
                                 target.getAsMention(), guild.getName()))
                             .build();

                ch.sendMessage(eb).queue();

                if (sub.isOneTime()) SubscriptionManager.removeSubscription(sub);
            });
        }


        // Role
        public void onGuildMemberRoleAdd(@Nonnull GuildMemberRoleAddEvent event) {
            var subs = SubscriptionManager.getGuildEvents(event.getGuild(), ROLE_ADD);
            if (subs.isEmpty()) return;

            var target = event.getUser();
            subs.removeIf(sub -> !sub.getTargetId().equals(target.getId()));
            if (subs.isEmpty()) return;

            var guild = event.getGuild();
            var roles = event.getRoles();
            subs.removeIf(sub -> guild.getMemberById(sub.getUserId()) == null);
            subs.removeIf(sub -> sub.getSettings().containsKey(ROLE.raw())
                                     && roles.stream().noneMatch(
                role -> role.getId().equals(sub.getSettings().get(ROLE.raw()
                ))));
            if (subs.isEmpty()) return;

            subs.forEach(sub -> {
                var user = event.getMember();
                var ch = user.getUser().openPrivateChannel().complete();

                var ck = sub.getSettings().containsKey(ROLE.raw());
                var role = ck ? guild.getRoleById(sub.getSettings().get(ROLE.raw())) : null;

                var eb = new EmbedBuilder()
                             .setTitle(TITLE)
                             .setFooter(String.format("%s#%s (%s)", target.getName(), target.getDiscriminator(), target.getId()),
                                 target.getEffectiveAvatarUrl());
                if (ck) eb.setDescription(String.format("%s just had the role **%s (%s)** added on *%s*",
                    target.getAsMention(), role.getName(), role.getId(), guild.getName()));
                else eb.setDescription(String.format("%s just had a role added on *%s*",
                    target.getAsMention(), guild.getName()));

                ch.sendMessage(eb.build()).queue();

                if (sub.isOneTime()) SubscriptionManager.removeSubscription(sub);
            });
        }

        public void onGuildMemberRoleRemove(@Nonnull GuildMemberRoleRemoveEvent event) {
            var subs = SubscriptionManager.getGuildEvents(event.getGuild(), ROLE_REMOVE);
            if (subs.isEmpty()) return;

            var target = event.getUser();
            subs.removeIf(sub -> !sub.getTargetId().equals(target.getId()));
            if (subs.isEmpty()) return;

            var guild = event.getGuild();
            var roles = event.getRoles();
            subs.removeIf(sub -> guild.getMemberById(sub.getUserId()) == null);
            subs.removeIf(sub -> sub.getSettings().containsKey(ROLE.raw())
                                     && roles.stream().noneMatch(
                role -> role.getId().equals(sub.getSettings().get(ROLE.raw()
                ))));
            if (subs.isEmpty()) return;

            subs.forEach(sub -> {
                var user = event.getMember();
                var ch = user.getUser().openPrivateChannel().complete();

                var ck = sub.getSettings().containsKey(ROLE.raw());
                var role = ck ? guild.getRoleById(sub.getSettings().get(ROLE.raw())) : null;

                var eb = new EmbedBuilder()
                             .setTitle(TITLE)
                             .setFooter(String.format("%s#%s (%s)", target.getName(), target.getDiscriminator(), target.getId()),
                                 target.getEffectiveAvatarUrl());
                if (ck) eb.setDescription(String.format("%s just had the role **%s (%s)** removed on *%s*",
                    target.getAsMention(), role.getName(), role.getId(), guild.getName()));
                else eb.setDescription(String.format("%s just had a role removed on *%s*",
                    target.getAsMention(), guild.getName()));

                ch.sendMessage(eb.build()).queue();

                if (sub.isOneTime()) SubscriptionManager.removeSubscription(sub);
            });
        }


        // MessageActivity
        // MessageActivity_Message
        public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
            var subs = SubscriptionManager.getGuildEvents(event.getGuild(), MESSAGE_ACTIVITY, MESSAGE_SENT);
            if (subs.isEmpty()) return;

            var target = event.getAuthor();
            subs.removeIf(sub -> !sub.getTargetId().equals(target.getId()));
            if (subs.isEmpty()) return;

            var guild = event.getGuild();
            var channel = event.getChannel();
            subs.removeIf(sub -> guild.getMemberById(sub.getUserId()) == null);
            subs.removeIf(sub -> sub.getSettings().containsKey(CHANNEL.raw())
                                     && !channel.getId().equals(sub.getSettings().get(CHANNEL.raw()))
            );
            if (subs.isEmpty()) return;

            subs.forEach(sub -> {
                var user = event.getMember();
                if (!user.hasPermission(channel, Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY)) return;
                var ch = user.getUser().openPrivateChannel().complete();

                var ck = sub.getSettings().containsKey(CHANNEL.raw());

                var eb = new EmbedBuilder()
                             .setTitle(TITLE)
                             .setFooter(String.format("%s#%s (%s)", target.getName(), target.getDiscriminator(), target.getId()),
                                 target.getEffectiveAvatarUrl());

                if (ck) eb.setDescription(String.format("%s just sent a message in %s on *%s*\n\n[Jump](%s)",
                    target.getAsMention(), channel.getAsMention(), guild.getName(), event.getMessage().getJumpUrl()));
                else eb.setDescription(String.format("%s just sent a message on *%s*\n\n[Jump](%s)",
                    target.getAsMention(), guild.getName(), event.getMessage().getJumpUrl()));

                ch.sendMessage(eb.build()).queue();

                if (sub.isOneTime()) SubscriptionManager.removeSubscription(sub);
            });
        }

        public void onGuildMessageUpdate(@Nonnull GuildMessageUpdateEvent event) {
            var subs = SubscriptionManager.getGuildEvents(event.getGuild(), MESSAGE_ACTIVITY, MESSAGE_EDIT);
            if (subs.isEmpty()) return;

            var target = event.getAuthor();
            subs.removeIf(sub -> !sub.getTargetId().equals(target.getId()));
            if (subs.isEmpty()) return;

            var guild = event.getGuild();
            var channel = event.getChannel();
            subs.removeIf(sub -> guild.getMemberById(sub.getUserId()) == null);
            subs.removeIf(sub -> sub.getSettings().containsKey(CHANNEL.raw())
                                     && !channel.getId().equals(sub.getSettings().get(CHANNEL.raw()))
            );
            if (subs.isEmpty()) return;


            subs.forEach(sub -> {
                var user = event.getMember();
                if (!user.hasPermission(channel, Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY)) return;
                var ch = user.getUser().openPrivateChannel().complete();

                var ck = sub.getSettings().containsKey(CHANNEL.raw());

                var eb = new EmbedBuilder()
                             .setTitle(TITLE)
                             .setFooter(String.format("%s#%s (%s)", target.getName(), target.getDiscriminator(), target.getId()),
                                 target.getEffectiveAvatarUrl());

                if (ck) eb.setDescription(String.format("%s just edited a message in %s on *%s*\n\n[Jump](%s)",
                    target.getAsMention(), channel.getAsMention(), guild.getName(), event.getMessage().getJumpUrl()));
                else eb.setDescription(String.format("%s just edited a message on *%s*\n\n[Jump](%s)",
                    target.getAsMention(), guild.getName(), event.getMessage().getJumpUrl()));

                ch.sendMessage(eb.build()).queue();

                if (sub.isOneTime()) SubscriptionManager.removeSubscription(sub);
            });
        }

        @Incubating
        public void onGuildMessageDelete(@Nonnull GuildMessageDeleteEvent event) {
        }

        // MessageActivity_Reaction
        public void onGuildMessageReactionAdd(@Nonnull GuildMessageReactionAddEvent event) {
            var subs = SubscriptionManager.getGuildEvents(event.getGuild(), REACTION_ADD);
            if (subs.isEmpty()) return;

            var target = event.getUser();
            subs.removeIf(sub -> !sub.getTargetId().equals(target.getId()));
            if (subs.isEmpty()) return;

            var guild = event.getGuild();
            subs.removeIf(sub -> guild.getMemberById(sub.getUserId()) == null);
            if (subs.isEmpty()) return;

            subs.forEach(sub -> {
                var user = event.getMember();
                var mCh = event.getChannel();
                if (!user.hasPermission(mCh, Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY)) return;
                var ch = user.getUser().openPrivateChannel().complete();

                var msg = mCh.retrieveMessageById(event.getMessageId()).complete();

                var eb = new EmbedBuilder()
                             .setTitle(TITLE)
                             .setFooter(String.format("%s#%s (%s)", target.getName(), target.getDiscriminator(), target.getId()),
                                 target.getEffectiveAvatarUrl())
                             .setDescription(String.format("%s just added a reaction to a message on *%s*\n\n[Jump](%s)",
                                 target.getAsMention(), guild.getName(), msg.getJumpUrl()))
                             .build();

                ch.sendMessage(eb).queue();

                if (sub.isOneTime()) SubscriptionManager.removeSubscription(sub);
            });
        }

        public void onGuildMessageReactionRemove(@Nonnull GuildMessageReactionRemoveEvent event) {
            var subs = SubscriptionManager.getGuildEvents(event.getGuild(), REACTION_REMOVE);
            if (subs.isEmpty()) return;

            var target = event.getUser();
            subs.removeIf(sub -> !sub.getTargetId().equals(target.getId()));
            if (subs.isEmpty()) return;

            var guild = event.getGuild();
            subs.removeIf(sub -> guild.getMemberById(sub.getUserId()) == null);
            if (subs.isEmpty()) return;

            subs.forEach(sub -> {
                var user = event.getMember();
                var mCh = event.getChannel();
                if (!user.hasPermission(mCh, Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY)) return;
                var ch = user.getUser().openPrivateChannel().complete();

                var msg = mCh.retrieveMessageById(event.getMessageId()).complete();

                var eb = new EmbedBuilder()
                             .setTitle(TITLE)
                             .setFooter(String.format("%s#%s (%s)", target.getName(), target.getDiscriminator(), target.getId()),
                                 target.getEffectiveAvatarUrl())
                             .setDescription(String.format("%s just removed a reaction to a message on *%s*\n\n[Jump](%s)",
                                 target.getAsMention(), guild.getName(), msg.getJumpUrl()))
                             .build();

                ch.sendMessage(eb).queue();

                if (sub.isOneTime()) SubscriptionManager.removeSubscription(sub);
            });
        }


        // VoiceActivity
        public void onGuildVoiceJoin(@Nonnull GuildVoiceJoinEvent event) {
            var subs = SubscriptionManager.getGuildEvents(event.getGuild(), VOICE_UPDATE, VOICE_JOIN);
            if (subs.isEmpty()) return;

            var target = event.getMember().getUser();
            subs.removeIf(sub -> !sub.getTargetId().equals(target.getId()));
            if (subs.isEmpty()) return;

            var guild = event.getGuild();
            var channel = event.getChannelJoined();
            subs.removeIf(sub -> guild.getMemberById(sub.getUserId()) == null);
            subs.removeIf(sub -> sub.getSettings().containsKey(CHANNEL.raw())
                                     && !channel.getId().equals(sub.getSettings().get(CHANNEL.raw()))
            );
            if (subs.isEmpty()) return;


            subs.forEach(sub -> {
                var user = event.getMember();
                if (!user.hasPermission(channel, Permission.VIEW_CHANNEL)) return;
                var ch = user.getUser().openPrivateChannel().complete();

                var ck = sub.getSettings().containsKey(CHANNEL.raw());

                var eb = new EmbedBuilder()
                             .setTitle(TITLE)
                             .setFooter(String.format("%s#%s (%s)", target.getName(), target.getDiscriminator(), target.getId()),
                                 target.getEffectiveAvatarUrl());

                if (ck) eb.setDescription(String.format("%s just joined the voice chat **%s (%s)** on *%s*",
                    target.getAsMention(), channel.getName(), channel.getId(), guild.getName()));
                else eb.setDescription(String.format("\"%s just joined a voice chat on *%s*\"",
                    target.getAsMention(), guild.getName()));

                ch.sendMessage(eb.build()).queue();

                if (sub.isOneTime()) SubscriptionManager.removeSubscription(sub);
            });
        }

        public void onGuildVoiceLeave(@Nonnull GuildVoiceLeaveEvent event) {
            var subs = SubscriptionManager.getGuildEvents(event.getGuild(), VOICE_UPDATE, VOICE_LEAVE);
            if (subs.isEmpty()) return;

            var target = event.getMember().getUser();
            subs.removeIf(sub -> !sub.getTargetId().equals(target.getId()));
            if (subs.isEmpty()) return;

            var guild = event.getGuild();
            var channel = event.getChannelLeft();
            subs.removeIf(sub -> guild.getMemberById(sub.getUserId()) == null);
            subs.removeIf(sub -> sub.getSettings().containsKey(CHANNEL.raw())
                                     && !channel.getId().equals(sub.getSettings().get(CHANNEL.raw()))
            );
            if (subs.isEmpty()) return;


            subs.forEach(sub -> {
                var user = event.getMember();
                if (!user.hasPermission(channel, Permission.VIEW_CHANNEL)) return;
                var ch = user.getUser().openPrivateChannel().complete();

                var ck = sub.getSettings().containsKey(CHANNEL.raw());

                var eb = new EmbedBuilder()
                             .setTitle(TITLE)
                             .setFooter(String.format("%s#%s (%s)", target.getName(), target.getDiscriminator(), target.getId()),
                                 target.getEffectiveAvatarUrl());

                if (ck) eb.setDescription(String.format("%s just left the voice chat **%s (%s)** on *%s*",
                    target.getAsMention(), channel.getName(), channel.getId(), guild.getName()));
                else eb.setDescription(String.format("\"%s just left a voice chat on *%s*\"",
                    target.getAsMention(), guild.getName()));

                ch.sendMessage(eb.build()).queue();

                if (sub.isOneTime()) SubscriptionManager.removeSubscription(sub);
            });
        }

        public void onGuildVoiceSelfMute(@Nonnull GuildVoiceSelfMuteEvent event) {
            var subs = SubscriptionManager.getGuildEvents(event.getGuild(), VOICE_UPDATE, VOICE_MUTE);
            if (subs.isEmpty()) return;

            var target = event.getMember().getUser();
            subs.removeIf(sub -> !sub.getTargetId().equals(target.getId()));
            if (subs.isEmpty()) return;

            var guild = event.getGuild();
            var channel = event.getVoiceState().getChannel();
            subs.removeIf(sub -> guild.getMemberById(sub.getUserId()) == null);
            subs.removeIf(sub -> sub.getSettings().containsKey(CHANNEL.raw())
                                     && !channel.getId().equals(sub.getSettings().get(CHANNEL.raw()))
            );
            if (subs.isEmpty()) return;


            subs.forEach(sub -> {
                var user = event.getMember();
                if (!user.hasPermission(channel, Permission.VIEW_CHANNEL)) return;
                var ch = user.getUser().openPrivateChannel().complete();

                var ck = sub.getSettings().containsKey(CHANNEL.raw());

                var eb = new EmbedBuilder()
                             .setTitle(TITLE)
                             .setFooter(String.format("%s#%s (%s)", target.getName(), target.getDiscriminator(), target.getId()),
                                 target.getEffectiveAvatarUrl());

                if (ck)
                    eb.setDescription(String.format("%s just muted themselves in the voice chat **%s (%s)** on *%s*",
                        target.getAsMention(), channel.getName(), channel.getId(), guild.getName()));
                else eb.setDescription(String.format("\"%s just muted themselves in a voice chat on *%s*\"",
                    target.getAsMention(), guild.getName()));

                ch.sendMessage(eb.build()).queue();

                if (sub.isOneTime()) SubscriptionManager.removeSubscription(sub);
            });
        }

        public void onGuildVoiceSelfDeafen(@Nonnull GuildVoiceSelfDeafenEvent event) {
            var subs = SubscriptionManager.getGuildEvents(event.getGuild(), VOICE_UPDATE, VOICE_DEAFEN);
            if (subs.isEmpty()) return;

            var target = event.getMember().getUser();
            subs.removeIf(sub -> !sub.getTargetId().equals(target.getId()));
            if (subs.isEmpty()) return;

            var guild = event.getGuild();
            var channel = event.getVoiceState().getChannel();
            subs.removeIf(sub -> guild.getMemberById(sub.getUserId()) == null);
            subs.removeIf(sub -> sub.getSettings().containsKey(CHANNEL.raw())
                                     && !channel.getId().equals(sub.getSettings().get(CHANNEL.raw()))
            );
            if (subs.isEmpty()) return;


            subs.forEach(sub -> {
                var user = event.getMember();
                if (!user.hasPermission(channel, Permission.VIEW_CHANNEL)) return;
                var ch = user.getUser().openPrivateChannel().complete();

                var ck = sub.getSettings().containsKey(CHANNEL.raw());

                var eb = new EmbedBuilder()
                             .setTitle(TITLE)
                             .setFooter(String.format("%s#%s (%s)", target.getName(), target.getDiscriminator(), target.getId()),
                                 target.getEffectiveAvatarUrl());

                if (ck)
                    eb.setDescription(String.format("%s just deafened themselves in the voice chat **%s (%s)** on *%s*",
                        target.getAsMention(), channel.getName(), channel.getId(), guild.getName()));
                else eb.setDescription(String.format("\"%s just deafened themselves in a voice chat on *%s*\"",
                    target.getAsMention(), guild.getName()));

                ch.sendMessage(eb.build()).queue();

                if (sub.isOneTime()) SubscriptionManager.removeSubscription(sub);
            });
        }

        public void onGuildVoiceStream(@Nonnull GuildVoiceStreamEvent event) {
            var subs = SubscriptionManager.getGuildEvents(event.getGuild(), VOICE_UPDATE, VOICE_STREAM);
            if (subs.isEmpty()) return;

            var target = event.getMember().getUser();
            subs.removeIf(sub -> !sub.getTargetId().equals(target.getId()));
            if (subs.isEmpty()) return;

            var guild = event.getGuild();
            var channel = event.getVoiceState().getChannel();
            subs.removeIf(sub -> guild.getMemberById(sub.getUserId()) == null);
            subs.removeIf(sub -> sub.getSettings().containsKey(CHANNEL.raw())
                                     && !channel.getId().equals(sub.getSettings().get(CHANNEL.raw()))
            );
            if (subs.isEmpty()) return;


            subs.forEach(sub -> {
                var user = event.getMember();
                if (!user.hasPermission(channel, Permission.VIEW_CHANNEL)) return;
                var ch = user.getUser().openPrivateChannel().complete();

                var ck = sub.getSettings().containsKey(CHANNEL.raw());

                var eb = new EmbedBuilder()
                             .setTitle(TITLE)
                             .setFooter(String.format("%s#%s (%s)", target.getName(), target.getDiscriminator(), target.getId()),
                                 target.getEffectiveAvatarUrl());

                if (ck)
                    eb.setDescription(String.format("%s just started/stopped streaming in the voice chat **%s (%s)** on *%s*",
                        target.getAsMention(), channel.getName(), channel.getId(), guild.getName()));
                else eb.setDescription(String.format("\"%s just started/stopped streaming in a voice chat on *%s*\"",
                    target.getAsMention(), guild.getName()));

                ch.sendMessage(eb.build()).queue();

                if (sub.isOneTime()) SubscriptionManager.removeSubscription(sub);
            });
        }
    }
}
