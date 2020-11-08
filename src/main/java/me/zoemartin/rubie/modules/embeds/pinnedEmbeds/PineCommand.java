package me.zoemartin.rubie.modules.embeds.pinnedEmbeds;

import com.google.gson.JsonSyntaxException;
import me.zoemartin.rubie.core.*;
import me.zoemartin.rubie.core.exceptions.*;
import me.zoemartin.rubie.core.interfaces.Command;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.*;
import me.zoemartin.rubie.modules.embeds.EmbedUtil;
import me.zoemartin.rubie.modules.pagedEmbeds.PageListener;
import me.zoemartin.rubie.modules.pagedEmbeds.PagedEmbed;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class PineCommand implements GuildCommand {
    @Override
    public void run(GuildCommandEvent event) {
        throw new CommandArgumentException();
    }

    @NotNull
    @Override
    public Set<Command> subCommands() {
        return Set.of(new Create(), new Update(), new list(), new Delete());
    }

    @NotNull
    @Override
    public String name() {
        return "pinnedembed";
    }

    @NotNull
    @Override
    public String regex() {
        return "pinnedembed|pine";
    }

    @NotNull
    @Override
    public CommandPerm commandPerm() {
        return CommandPerm.BOT_MANAGER;
    }

    @NotNull
    @Override
    public String description() {
        return "Pinned Embeds";
    }

    private static class Create implements GuildCommand {
        @Override
        public void run(GuildCommandEvent event) {
            List<String> args = event.getArgs();
            Check.check(!args.isEmpty(), CommandArgumentException::new);

            String json = EmbedUtil.jsonFromUrl(args.get(0));

            TextChannel c;

            if (args.size() > 1) {
                String cRef = lastArg(1, event);
                c = Parser.Channel.getTextChannel(event.getGuild(), cRef);
                Check.entityReferenceNotNull(c, TextChannel.class, cRef);
            } else c = event.getChannel();

            Check.check(event.getMember().hasPermission(c, Permission.MESSAGE_WRITE),
                () -> new ConsoleError("Member '%s' doesn't have write permissions in channel '%s'",
                    event.getMember().getId(), c.getId()));

            Embed e;
            try {
                e = Embed.fromJson(json);
            } catch (JsonSyntaxException ignored) {
                throw new ReplyError("Sorry, I cannot parse the json from that url!");
            }

            Message m = c.sendMessage(e.toDiscordEmbed()).complete();

            PineEntity pine = new PineEntity(event.getGuild().getId(), c.getId(), m.getId(), args.get(0));
            PineController.addPine(pine);
            DatabaseUtil.saveObject(pine);
        }

        @NotNull
        @Override
        public String name() {
            return "create";
        }

        @NotNull
        @Override
        public CommandPerm commandPerm() {
            return CommandPerm.BOT_MANAGER;
        }

        @NotNull
        @Override
        public String usage() {
            return "<url> [channel]";
        }

        @NotNull
        @Override
        public String description() {
            return "Create a Pine";
        }
    }

    private static class Update implements GuildCommand {

        @Override
        public void run(GuildCommandEvent event) {
            List<String> args = event.getArgs();
            Check.check(!args.isEmpty(), CommandArgumentException::new);

            String mRef = args.get(0);
            Check.check(mRef.matches("\\d{17,19}"), () -> new ReplyError("Error, `%s` is not a valid id", mRef));
            TextChannel c;

            if (args.size() > 1) {
                String cRef = lastArg(1, event);
                c = Parser.Channel.getTextChannel(event.getGuild(), cRef);
                Check.entityReferenceNotNull(c, TextChannel.class, cRef);
            } else c = event.getChannel();

            Message message;
            try {
                message = c.retrieveMessageById(mRef).complete();
            } catch (ErrorResponseException e) {
                if (e.getErrorResponse().getCode() == 10008)
                    throw new EntityNotFoundException("Error, message `%s` not found in %s!", mRef, c.getAsMention());
                else
                    throw e;
            }

            PineEntity pine = PineController.getPine(event.getGuild(), c, mRef);
            Check.notNull(pine, () -> new ReplyError("Error, could not find that pine!"));

            String json = EmbedUtil.jsonFromUrl(pine.getSource_url());
            Embed e;
            try {
                e = Embed.fromJson(json);
            } catch (JsonSyntaxException ignored) {
                throw new ReplyError("Sorry, I cannot parse the json from that url!");
            }

            message.editMessage(e.toDiscordEmbed()).complete();
            embedReply(event, "Pine Updates",
                "Updated Pine [%s](%s) in %s", mRef, message.getJumpUrl(), c.getAsMention()).queue();
        }

        @NotNull
        @Override
        public String name() {
            return "update";
        }

        @NotNull
        @Override
        public CommandPerm commandPerm() {
            return CommandPerm.BOT_MANAGER;
        }

        @NotNull
        @Override
        public String usage() {
            return "<message id> [channel]";
        }

        @NotNull
        @Override
        public String description() {
            return "Updates a Pine";
        }
    }

    private static class list implements GuildCommand {
        @Override
        public void run(GuildCommandEvent event) {
            Guild g = event.getGuild();
            TextChannel c;
            if (!event.getArgs().isEmpty()) c = Parser.Channel.getTextChannel(g, event.getArgs().get(0));
            else c = null;

            Collection<PineEntity> pines = PineController.getPines(g);
            if (c != null) pines = pines.stream()
                                       .filter(pineEntity -> pineEntity.getChannel_id().equals(c.getId()))
                                       .collect(Collectors.toList());

            PagedEmbed p = new PagedEmbed(me.zoemartin.rubie.core.util.EmbedUtil.pagedDescription(
                new EmbedBuilder().setTitle("Pinned Embeds" + (c == null ? "" : " in " + c.getName())).build(),
                pines.stream().map(pineEntity -> {
                    TextChannel tx = g.getTextChannelById(pineEntity.getChannel_id());
                    if (c == null) return String.format("%s - %s - %s\n",
                        tx == null ? "`" + pineEntity.getChannel_id() + "`" : tx.getAsMention(),
                        String.format("[%s](https://discord.com/channels/%s/%s/%s)", pineEntity.getMessage_id(),
                            pineEntity.getGuild_id(), pineEntity.getChannel_id(), pineEntity.getMessage_id()),
                        pineEntity.getSource_url());
                    else return String.format("%s - %s\n", String.format("[%s](https://discord.com/channels/%s/%s/%s)",
                        pineEntity.getMessage_id(), pineEntity.getGuild_id(), pineEntity.getChannel_id(),
                        pineEntity.getMessage_id()), pineEntity.getSource_url());
                }).collect(Collectors.toList())),
                event.getChannel(), event.getUser());

            PageListener.add(p);

        }

        @NotNull
        @Override
        public String name() {
            return "list";
        }

        @NotNull
        @Override
        public CommandPerm commandPerm() {
            return CommandPerm.BOT_MANAGER;
        }

        @NotNull
        @Override
        public String usage() {
            return "[channel]";
        }

        @NotNull
        @Override
        public String description() {
            return "List all pines";
        }
    }

    private static class Delete implements GuildCommand {
        @Override
        public void run(GuildCommandEvent event) {
            List<String> args = event.getArgs();
            Check.check(!args.isEmpty(), CommandArgumentException::new);

            String mRef = args.get(0);
            Check.check(mRef.matches("\\d{17,19}"), () -> new ReplyError("Error, `%s` is not a valid id", mRef));
            TextChannel c;

            if (args.size() > 1) {
                String cRef = lastArg(1, event);
                c = Parser.Channel.getTextChannel(event.getGuild(), cRef);
                Check.entityReferenceNotNull(c, TextChannel.class, cRef);
            } else c = event.getChannel();

            PineEntity pine = PineController.getPine(event.getGuild(), c, mRef);
            Check.notNull(pine, () -> new ReplyError("Error, could not find that pine!"));

            Message message;
            try {
                message = c.retrieveMessageById(mRef).complete();
            } catch (ErrorResponseException e) {
                message = null;
            }

            DatabaseUtil.deleteObject(pine);
            PineController.removePine(pine);

            if (message == null) embedReply(event, "Pine Deletion",
                "Deleted Pine `%s` in %s", mRef, c.getAsMention()).queue();
            else embedReply(event, "Pine Deletion",
                "Deleted Pine [%s](%s) in %s", mRef, message.getJumpUrl(), c.getAsMention()).queue();
        }

        @NotNull
        @Override
        public String name() {
            return "delete";
        }

        @NotNull
        @Override
        public CommandPerm commandPerm() {
            return CommandPerm.BOT_MANAGER;
        }

        @NotNull
        @Override
        public String usage() {
            return "<message id> [channel]";
        }

        @NotNull
        @Override
        public String description() {
            return "Deletes a pine";
        }
    }
}
