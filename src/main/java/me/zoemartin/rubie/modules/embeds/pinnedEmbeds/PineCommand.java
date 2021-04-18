package me.zoemartin.rubie.modules.embeds.pinnedEmbeds;

import com.google.gson.JsonSyntaxException;
import me.zoemartin.rubie.core.*;
import me.zoemartin.rubie.core.annotations.*;
import me.zoemartin.rubie.core.exceptions.*;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.*;
import me.zoemartin.rubie.modules.embeds.EmbedUtil;
import me.zoemartin.rubie.modules.pagedEmbeds.PageListener;
import me.zoemartin.rubie.modules.pagedEmbeds.PagedEmbed;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Command
@CommandOptions(
    name = "pinnedembed",
    description = "Pinned Embeds",
    perm = CommandPerm.BOT_MANAGER,
    alias = "pine"
)
public class PineCommand extends GuildCommand {
    @Override
    public void run(GuildCommandEvent event) {
        throw new CommandArgumentException();
    }

    @SubCommand(PineCommand.class)
    @CommandOptions(
        name = "create",
        description = "Creates a PINE",
        usage = "<url> [channel]",
        perm = CommandPerm.BOT_MANAGER
    )
    private static class Create extends GuildCommand {
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
            } else c = event.getTextChannel();

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
    }

    @SubCommand(PineCommand.class)
    @CommandOptions(
        name = "update",
        description = "Updates a PINE",
        usage = "<message id> [channel]",
        perm = CommandPerm.BOT_MANAGER
    )
    private static class Update extends GuildCommand {
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
            } else c = event.getTextChannel();

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
            event.reply("Pine Updates",
                "Updated Pine [%s](%s) in %s", mRef, message.getJumpUrl(), c.getAsMention()).queue();
        }
    }

    @SubCommand(PineCommand.class)
    @CommandOptions(
        name = "list",
        description = "Lists all PINEs",
        usage = "[channel]",
        perm = CommandPerm.BOT_MANAGER
    )
    private static class list extends GuildCommand {
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
                }).collect(Collectors.toList())), event);

            PageListener.add(p);

        }
    }

    @SubCommand(PineCommand.class)
    @CommandOptions(
        name = "delete",
        description = "Deletes a PINE",
        usage = "<message id> [channel]",
        perm = CommandPerm.BOT_MANAGER
    )
    private static class Delete extends GuildCommand {
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
            } else c = event.getTextChannel();

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

            if (message == null) event.reply("Pine Deletion",
                "Deleted Pine `%s` in %s", mRef, c.getAsMention()).queue();
            else event.reply("Pine Deletion",
                "Deleted Pine [%s](%s) in %s", mRef, message.getJumpUrl(), c.getAsMention()).queue();
        }
    }
}
