package me.zoemartin.rubie.modules.reactroles;

import com.google.gson.GsonBuilder;
import me.zoemartin.rubie.core.*;
import me.zoemartin.rubie.core.annotations.*;
import me.zoemartin.rubie.core.exceptions.*;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.*;
import me.zoemartin.rubie.modules.pagedEmbeds.PageListener;
import me.zoemartin.rubie.modules.pagedEmbeds.PagedEmbed;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;

import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Command
@CommandOptions(
    name = "reactrole",
    description = "tbd",
    alias = "rr",
    perm = CommandPerm.BOT_MANAGER,
    botPerms = Permission.MANAGE_ROLES
)
public class ReactRoleCommand extends GuildCommand {
    @Override
    public void run(GuildCommandEvent event) {
        throw new CommandArgumentException();
    }

    @SubCommand(ReactRoleCommand.class)
    @CommandOptions(
        name = "config",
        description = "tbd",
        usage = "[key] [value]",
        perm = CommandPerm.BOT_MANAGER
    )
    private static class Config extends AutoConfig<ReactConfig> {
        @Override
        protected ReactConfig supply(GuildCommandEvent event) {
            return ReactManager.getConfig(event.getGuild());
        }
    }

    @SubCommand(ReactRoleCommand.class)
    @CommandOptions(
        name = "clean",
        description = "Removes all broken react roles",
        help = "Clears React Roles whose channel, message or role have been deleted",
        perm = CommandPerm.BOT_ADMIN,
        botPerms = Permission.MANAGE_ROLES
    )
    private static class Clean extends GuildCommand {
        @Override
        public void run(GuildCommandEvent event) {
            var g = event.getGuild();
            var r = ReactManager.getReactRoles(g);

            var cleaned = new HashSet<ReactRole>();
            r.values().stream().flatMap(Collection::stream).forEach(reactRole -> {
                try {
                    var role = g.getRoleById(reactRole.getRoleId());
                    var channel = g.getTextChannelById(reactRole.getChannelId());
                    if (role == null || channel == null) throw new EntityNotFoundException();
                    else {
                        try {
                            channel.retrieveMessageById(reactRole.getMessageId()).complete();
                        } catch (ErrorResponseException e) {
                            if (e.getErrorResponse() == ErrorResponse.UNKNOWN_MESSAGE)
                                throw new EntityNotFoundException();
                            else throw e;
                        }
                    }
                } catch (EntityNotFoundException e) {
                    cleaned.add(reactRole);
                    ReactManager.removeReactRole(reactRole);
                }

            });
            var gson = new GsonBuilder().setPrettyPrinting().create();
            var export = gson.toJson(cleaned);

            event.reply("React Role Cleaner",
                "Cleaned all broken react roles from the database. " +
                    "The attached file contains the export of the removed settings")
                .addFile(export.getBytes(), "rr_export_" + Instant.now().toEpochMilli() + ".json").queue();
        }
    }

    @SubCommand(ReactRoleCommand.class)
    @CommandOptions(
        name = "show",
        description = "View react roles",
        perm = CommandPerm.BOT_MANAGER,
        botPerms = Permission.MANAGE_ROLES
    )
    private static class Show extends GuildCommand {
        @Override
        public void run(GuildCommandEvent event) {
            var g = event.getGuild();
            var args = event.getArgs();
            var all = ReactManager.getReactRoles(g);
            if (!args.isEmpty()) {
                var msg = args.get(0);
                var show = all.get(msg);
                var pages = EmbedUtil.pagedDescription(
                    new EmbedBuilder().setTitle("React Roles for `" + msg + "`").build(),
                    show.stream().map(reactRole -> {
                        var role = Parser.Role.getRole(g, reactRole.getRoleId());

                        if (reactRole.getReact().matches("\\d+")) {
                            var emote = event.getJDA().getEmoteById(reactRole.getReact());
                            return String.format("%s - %s\n", emote == null ? reactRole.getReact() : emote.getAsMention(),
                                Objects.requireNonNull(role).getAsMention());
                        } else
                            return String.format("%s - %s\n", reactRole.getReact(), Objects.requireNonNull(role).getAsMention());
                    }).collect(Collectors.toUnmodifiableList()));

                var paged = new PagedEmbed(pages, event);

                PageListener.add(paged);
            } else {
                var pages = all.entrySet().stream().map(e -> EmbedUtil.pagedDescription(
                    new EmbedBuilder().setTitle("React Roles for `" + e.getKey() + "`").build(),
                    e.getValue().stream().map(reactRole -> {
                        var role = Parser.Role.getRole(g, reactRole.getRoleId());

                        if (reactRole.getReact().matches("\\d+")) {
                            var emote = event.getJDA().getEmoteById(reactRole.getReact());
                            return String.format("%s - %s\n", emote == null ? reactRole.getReact() : emote.getAsMention(),
                                Objects.requireNonNull(role).getAsMention());
                        } else
                            return String.format("%s - %s\n", reactRole.getReact(), Objects.requireNonNull(role).getAsMention());
                    }).collect(Collectors.toUnmodifiableList()))).flatMap(Collection::stream).collect(Collectors.toList());

                var paged = new PagedEmbed(pages, event);

                PageListener.add(paged);
            }
        }
    }

    @SubCommand(ReactRoleCommand.class)
    @CommandOptions(
        name = "clear",
        description = "Clear the React Roles from a message",
        usage = "<message>",
        perm = CommandPerm.BOT_MANAGER,
        botPerms = Permission.MANAGE_ROLES
    )
    private static class Clear extends GuildCommand {
        @Override
        public void run(GuildCommandEvent event) {
            var g = event.getGuild();
            var args = event.getArgs();
            Check.check(!args.isEmpty(), CommandArgumentException::new);

            var all = ReactManager.getReactRoles(g);
            var ref = args.get(0);
            var message = Collections.unmodifiableSet(all.getOrDefault(ref, null));
            Check.notNull(message, () -> new ReplyError("No react roles found for `%s`", ref));

            message.forEach(ReactManager::removeReactRole);
            var gson = new GsonBuilder().setPrettyPrinting().create();
            var export = gson.toJson(message);

            event.reply("React Roles Cleared",
                "Cleaned react roles for `%s`." +
                    "The attached file contains the export of the removed settings", ref)
                .addFile(export.getBytes(), "rr_export_" + ref + "_" + Instant.now().toEpochMilli() + ".json").queue();
        }
    }

    @SubCommand(ReactRoleCommand.class)
    @CommandOptions(
        name = "add",
        description = "Add React Roles to a Message",
        help = "Only use one `react - role` pair per line. " +
                   "When the react is already on the target message I will not add another one!",
        usage = "<message id> [channel]",
        perm = CommandPerm.BOT_MANAGER,
        botPerms = Permission.MANAGE_ROLES
    )
    @Checks.Permissions.Guild(Permission.MANAGE_ROLES)
    private static class Add extends GuildCommand {
        private static final Pattern EMOTE_PATTERN = Pattern.compile("<?(?:a:)?.*?:?(\\d{17,19})>?[ \\t]+(.+)");
        private static final Pattern EMOJI_PATTERN = Pattern.compile("([^\\w]*)\\s+(.*)");

        @Override
        public void run(GuildCommandEvent event) {
            var args = event.getArgs();
            Check.check(!args.isEmpty(), CommandArgumentException::new);

            var guild = event.getGuild();
            var jda = event.getJDA();

            var channel = Parser.Channel.getTextChannel(guild, args.get(1));
            var message = channel == null ?
                              event.getChannel().retrieveMessageById(args.get(0)).complete()
                              : channel.retrieveMessageById(args.get(0)).complete();

            var input = event.getArgString().replaceFirst(args.get(0), "");
            if (channel != null) input = input.replaceFirst(args.get(2), "");
            var lines = input.lines();

            Check.notNull(message, () -> new EntityNotFoundException("Sorry, I couldn't find that message!"));
            var reacts = lines.map(s -> {
                var emoteMatcher = EMOTE_PATTERN.matcher(s);
                var emojiMatcher = EMOJI_PATTERN.matcher(s);
                if (emoteMatcher.find()) {
                    var emote = Parser.Emote.parse(emoteMatcher.group(1));
                    var role = Parser.Role.getRole(guild, emoteMatcher.group(2));
                    Check.notNull(role, () -> new EntityNotFoundException("Malformed Input: Could not find role %s", emoteMatcher.group(2)));
                    return new ReactRole(message, role, emote);
                } else if (emojiMatcher.find()) {
                    var emoji = emojiMatcher.group(1).trim();
                    var role = Parser.Role.getRole(guild, emojiMatcher.group(2));
                    Check.notNull(role, () -> new EntityNotFoundException("Malformed Input: Could not find role %s", emoteMatcher.group(2)));
                    return new ReactRole(message, role, emoji);
                } else throw new ReplyError("Malformed Input: `%s`", s);
            }).filter(reactRole -> !ReactManager.forMessage(message).contains(reactRole))
                             .collect(Collectors.toList());

            var existing = message.getReactions();
            var failed = new ArrayList<ReactRole>();
            reacts.stream()
                .filter(reactRole -> existing.stream()
                                         .map(MessageReaction::getReactionEmote)
                                         .map(react -> react.isEmoji() ? react.getEmoji() : react.getEmote().getId())
                                         .noneMatch(react -> reactRole.getReact().equals(react)))
                .forEach(reactRole -> {
                    if (reactRole.getReact().matches("\\d+")) {
                        var emote = jda.getEmoteById(reactRole.getReact());
                        if (emote != null) message.addReaction(emote).queue();
                    } else {
                        try {
                            message.addReaction(reactRole.getReact()).complete();
                        } catch (ErrorResponseException e) {
                            if (e.getErrorResponse() == ErrorResponse.UNKNOWN_EMOJI) {
                                failed.add(reactRole);
                            } else {
                                throw e;
                            }
                        }
                    }
                });

            reacts.removeIf(failed::contains);
            reacts.forEach(ReactManager::addReactRole);
            event.reply("React Roles Add Bulk", reacts.stream().map(reactRole -> {
                var role = Parser.Role.getRole(guild, reactRole.getRoleId());

                if (reactRole.getReact().matches("\\d+")) {
                    var emote = jda.getEmoteById(reactRole.getReact());
                    return String.format("%s - %s", emote == null ? reactRole.getReact() : emote.getAsMention(),
                        Objects.requireNonNull(role).getAsMention());
                } else
                    return String.format("%s - %s", reactRole.getReact(), Objects.requireNonNull(role).getAsMention());
            }).collect(Collectors.joining("\n"))).queue();
        }
    }
}
