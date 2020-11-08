package me.zoemartin.rubie.modules.embeds.triggerEmbeds;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.*;
import me.zoemartin.rubie.core.exceptions.*;
import me.zoemartin.rubie.core.interfaces.Command;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.*;
import me.zoemartin.rubie.modules.embeds.EmbedUtil;
import me.zoemartin.rubie.modules.levels.*;
import me.zoemartin.rubie.modules.pagedEmbeds.PageListener;
import me.zoemartin.rubie.modules.pagedEmbeds.PagedEmbed;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TeeCommand implements GuildCommand {
    @Override
    public void run(GuildCommandEvent event) {
        throw new CommandArgumentException();
    }

    @NotNull
    @Override
    public Set<Command> subCommands() {
        return Set.of(new Create(), new Update(), new list(), new Delete(), new SetPerm(), new Export(), new Import());
    }

    @NotNull
    @Override
    public String name() {
        return "triggerembed";
    }

    @NotNull
    @Override
    public String regex() {
        return "triggerembed|trembed|te";
    }

    @NotNull
    @Override
    public CommandPerm commandPerm() {
        return CommandPerm.BOT_MANAGER;
    }

    @NotNull
    @Override
    public String description() {
        return "Trigger Embeds";
    }

    private static class Create implements GuildCommand {
        @Override
        public void run(GuildCommandEvent event) {
            Check.check(event.getArgs().size() > 1, CommandArgumentException::new);

            String url = event.getArgs().get(0);
            String json = EmbedUtil.jsonFromUrl(url);
            String trigger = lastArg(1, event);

            Check.check(TeeController.getTriggerEmbed(event.getGuild(), trigger) == null,
                () -> new ReplyError("Error, a trigger embed with `%s` already exists!", trigger));

            try {
                Embed.fromJson(json);
            } catch (JsonSyntaxException ignored) {
                throw new ReplyError("Sorry, I cannot parse the json from that url!");
            }

            Tee tee = new Tee(event.getGuild().getId(), trigger, url, json);
            DatabaseUtil.saveObject(tee);
            TeeController.addTriggerEmbed(tee);
            event.addCheckmark();
            embedReply(event, "Trigger Embed Added",
                "Added Embed with trigger `%s`", trigger).queue();
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
            return "<url> <trigger>";
        }

        @NotNull
        @Override
        public String description() {
            return "Create a Trigger Embed";
        }
    }

    private static class SetPerm implements GuildCommand {
        @Override
        public void run(GuildCommandEvent event) {
            Check.check(event.getArgs().size() > 1, CommandArgumentException::new);

            String trigger = lastArg(1, event);
            String permLvl = event.getArgs().get(0);

            CommandPerm perm = permLvl.matches("\\d") ? CommandPerm.fromNum(Integer.parseInt(permLvl)) :
                                   CommandPerm.fromString(permLvl.toUpperCase());

            Check.notNull(perm, () -> new ReplyError("Error, could not find permission level `%s`", permLvl));

            Tee tee = TeeController.getTriggerEmbed(event.getGuild(), trigger);
            Check.notNull(tee, () -> new ReplyError("Error, could not find that Trigger Embed!"));

            tee.setPerm(perm);
            DatabaseUtil.updateObject(tee);
            embedReply(event, "Trigger Embed Updates",
                "Set Trigger Embed `%s` permission to `[%s] %s`", trigger, perm.raw(), perm.toString()).queue();
        }

        @NotNull
        @Override
        public String name() {
            return "perm";
        }

        @NotNull
        @Override
        public CommandPerm commandPerm() {
            return CommandPerm.BOT_MANAGER;
        }

        @NotNull
        @Override
        public String usage() {
            return "<perm lvl> <trigger>";
        }

        @NotNull
        @Override
        public String description() {
            return "Sets a Trigger Embeds permission";
        }
    }

    private static class Update implements GuildCommand {
        @NotNull
        @Override
        public Set<Command> subCommands() {
            return Set.of(new All());
        }

        @Override
        public void run(GuildCommandEvent event) {
            Check.check(!event.getArgs().isEmpty(), CommandArgumentException::new);

            String trigger = lastArg(0, event);

            Tee tee = TeeController.getTriggerEmbed(event.getGuild(), trigger);
            Check.notNull(tee, () -> new ReplyError("Error, could not find that Trigger Embed!"));

            String json = EmbedUtil.jsonFromUrl(tee.getSource_url());
            try {
                Embed.fromJson(json);
            } catch (JsonSyntaxException ignored) {
                throw new ReplyError("Sorry, I cannot parse the json from that url!");
            }

            tee.setCached_json(json);
            DatabaseUtil.updateObject(tee);

            embedReply(event, "Trigger Embed Updates",
                "Updated Trigger Embed `%s`", trigger).queue();
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
            return "<trigger> ";
        }

        @NotNull
        @Override
        public String description() {
            return "Updates a Trigger Embed";
        }

        private static class All implements GuildCommand {
            @Override
            public void run(GuildCommandEvent event) {
                String contains = lastArg(0, event);
                Guild g = event.getGuild();

                Collection<Tee> tees;
                if (contains == null) tees = TeeController.getTriggerEmbeds(g);
                else tees = TeeController.getTriggerEmbeds(g).stream()
                                .filter(tee -> tee.getTrigger().contains(contains))
                                .collect(Collectors.toList());

                List<Tee> failed = tees.stream().map(tee -> {
                    String json = EmbedUtil.jsonFromUrl(tee.getSource_url());
                    try {
                        Embed.fromJson(json);
                    } catch (JsonSyntaxException ignored) {
                        return tee;
                    }

                    tee.setCached_json(json);
                    DatabaseUtil.updateObject(tee);
                    return null;
                }).filter(Objects::nonNull).collect(Collectors.toList());

                PagedEmbed p = new PagedEmbed(me.zoemartin.rubie.core.util.EmbedUtil.pagedDescription(
                    new EmbedBuilder()
                        .setTitle("Trigger Embed Updates" + (contains == null ? "" : " containing `" + contains + "`")).build(),
                    tees.stream().map(tee -> String.format("`%s` - `%s`\n",
                        tee.getTrigger(), failed.contains(tee) ? "FAILED | Source corrupted" : "UPDATED"))
                        .collect(Collectors.toList())),
                    event.getChannel(), event.getUser()
                );

                PageListener.add(p);
            }

            @NotNull
            @Override
            public String name() {
                return "all";
            }

            @NotNull
            @Override
            public CommandPerm commandPerm() {
                return CommandPerm.BOT_MANAGER;
            }

            @NotNull
            @Override
            public String usage() {
                return "[trigger containing]";
            }

            @NotNull
            @Override
            public String description() {
                return "Updates all Trigger Embeds";
            }
        }
    }

    private static class list implements GuildCommand {
        @Override
        public void run(GuildCommandEvent event) {
            String contains = lastArg(0, event);
            Guild g = event.getGuild();

            Collection<Tee> tees;
            if (contains == null) tees = TeeController.getTriggerEmbeds(g);
            else tees = TeeController.getTriggerEmbeds(g).stream()
                            .filter(tee -> tee.getTrigger().contains(contains))
                            .collect(Collectors.toList());

            PagedEmbed p = new PagedEmbed(me.zoemartin.rubie.core.util.EmbedUtil.pagedDescription(
                new EmbedBuilder()
                    .setTitle("Trigger Embeds" + (contains == null ? "" : " containing `" + contains + "`")).build(),
                tees.stream().map(entity -> String.format("`%s` - [Source](%s) - `[%s] %s`\n",
                    entity.getTrigger(), entity.getSource_url(), entity.getPerm().raw(), entity.getPerm().toString()))
                    .collect(Collectors.toList())),
                event.getChannel(), event.getUser()
            );

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
            return "[trigger containing]";
        }

        @NotNull
        @Override
        public String description() {
            return "List all Trigger Embeds";
        }
    }

    private static class Export implements GuildCommand {
        @NotNull
        @Override
        public Set<Command> subCommands() {
            return Set.of(new Cached());
        }

        @Override
        public void run(GuildCommandEvent event) {
            String contains = lastArg(0, event);
            Guild g = event.getGuild();

            Collection<Tee> tees;
            if (contains == null) tees = TeeController.getTriggerEmbeds(g);
            else tees = TeeController.getTriggerEmbeds(g).stream()
                            .filter(tee -> tee.getTrigger().contains(contains))
                            .collect(Collectors.toList());

            tees = tees.stream().map(tee -> new Tee(null, null,
                tee.getTrigger(), tee.getSource_url(), null, tee.getPerm())).collect(Collectors.toList());


            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            event.getChannel().sendFile(gson.toJson(tees).getBytes(),
                String.format("tees_%s_%s.json", g.getId(), Instant.now())).complete();

            PagedEmbed p = new PagedEmbed(me.zoemartin.rubie.core.util.EmbedUtil.pagedDescription(
                new EmbedBuilder()
                    .setTitle("Exported Trigger Embeds" + (contains == null ? "" : " containing `" + contains + "`")).build(),
                tees.stream().map(entity -> String.format("`%s` - [Source](%s) - `[%s] %s`\n",
                    entity.getTrigger(), entity.getSource_url(), entity.getPerm().raw(), entity.getPerm().toString()))
                    .collect(Collectors.toList())),
                event.getChannel(), event.getUser()
            );

            PageListener.add(p);
        }

        @NotNull
        @Override
        public String name() {
            return "export";
        }

        @NotNull
        @Override
        public CommandPerm commandPerm() {
            return CommandPerm.BOT_MANAGER;
        }

        @NotNull
        @Override
        public String usage() {
            return "[trigger containing]";
        }

        @NotNull
        @Override
        public String description() {
            return "Export all Trigger Embeds";
        }

        private static class Cached implements GuildCommand {
            @Override
            public void run(GuildCommandEvent event) {
                String contains = lastArg(0, event);
                Guild g = event.getGuild();

                Collection<Tee> tees;
                if (contains == null) tees = TeeController.getTriggerEmbeds(g);
                else tees = TeeController.getTriggerEmbeds(g).stream()
                                .filter(tee -> tee.getTrigger().contains(contains))
                                .collect(Collectors.toList());

                tees = tees.stream().map(tee -> new Tee(null, null,
                    tee.getTrigger(), tee.getSource_url(), tee.getCached_json(), tee.getPerm())).collect(Collectors.toList());

                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                event.getChannel().sendFile(gson.toJson(tees).getBytes(),
                    String.format("tees_%s_%s_cached.json", g.getId(), Instant.now())).complete();

                PagedEmbed p = new PagedEmbed(me.zoemartin.rubie.core.util.EmbedUtil.pagedDescription(
                    new EmbedBuilder()
                        .setTitle("Exported Trigger Embeds" + (contains == null ? "" : " containing `" + contains + "`")).build(),
                    tees.stream().map(entity -> String.format("`%s` - [Source](%s) - `[%s] %s`\n",
                        entity.getTrigger(), entity.getSource_url(), entity.getPerm().raw(), entity.getPerm().toString()))
                        .collect(Collectors.toList())),
                    event.getChannel(), event.getUser()
                );

                PageListener.add(p);
            }

            @NotNull
            @Override
            public String name() {
                return "cached";
            }

            @NotNull
            @Override
            public CommandPerm commandPerm() {
                return CommandPerm.BOT_MANAGER;
            }

            @NotNull
            @Override
            public String usage() {
                return "[trigger containing]";
            }

            @NotNull
            @Override
            public String description() {
                return "Export all Trigger Embeds with the cached json";
            }
        }
    }

    private static class Delete implements GuildCommand {
        @Override
        public void run(GuildCommandEvent event) {
            Check.check(!event.getArgs().isEmpty(), CommandArgumentException::new);

            String trigger = lastArg(0, event);

            Tee tee = TeeController.getTriggerEmbed(event.getGuild(), trigger);
            Check.notNull(tee, () -> new ReplyError("Error, could not find that Trigger Embed!"));

            DatabaseUtil.deleteObject(tee);
            TeeController.removeTriggerEmbed(tee);
            embedReply(event, "Trigger Embed Deletion",
                "Deleted Trigger Embed with `%s`", trigger).queue();
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
            return "<trigger>";
        }

        @NotNull
        @Override
        public String description() {
            return "Deletes a Trigger Embed";
        }
    }

    private static class Import implements GuildCommand {
        @Override
        public void run(GuildCommandEvent event) {
            Check.check(event.getArgs().isEmpty(), CommandArgumentException::new);
            Check.check(event.getAttachments().size() == 1, CommandArgumentException::new);
            Message m = event.getChannel().sendMessage("Okay... this might take a while").complete();
            Instant start = Instant.now();

            String guildId = event.getGuild().getId();
            Type listType = new TypeToken<ArrayList<Tee>>() {
            }.getType();
            List<Tee> tees;
            try {
                InputStreamReader ir = new InputStreamReader(event.getAttachments()
                                                                 .get(0)
                                                                 .retrieveInputStream()
                                                                 .get(1, TimeUnit.MINUTES));
                BufferedReader br = new BufferedReader(ir);
                tees = new Gson().fromJson(br, listType);
            } catch (RuntimeException | InterruptedException | ExecutionException | TimeoutException e) {
                throw new ReplyError("An unexpected error occurred while importing that file. " +
                                         "Common Issues: Corrupted File, Unsupported Format");
            }


            List<Tee> imported = tees.stream()
                .map(tee -> new Tee(guildId, tee.getTrigger(), tee.getSource_url(), tee.getCached_json(), tee.getPerm()))
                .collect(Collectors.toList());

            Map<Tee, String> failed = imported.stream().filter(tee -> tee.getTrigger() == null || tee.getSource_url() == null)
                                   .collect(Collectors.toMap(Function.identity(), tee -> "trigger/source corrupted"));

            List<Tee> update = imported.stream()
                                   .filter(tee -> !failed.containsKey(tee) && tee.getCached_json() == null)
                                   .collect(Collectors.toList());

            update.forEach(tee -> {
                try {
                    Embed e = Embed.fromJson(EmbedUtil.jsonFromUrl(tee.getSource_url()));
                    tee.setCached_json(e.toJson());
                } catch (RuntimeException ignored) {
                    failed.put(tee, "JSON source corrupted");
                }
            });

            imported.stream().filter(tee -> !failed.containsKey(tee)).forEach(tee -> {
                try {
                    Embed.fromJson(tee.getCached_json());
                } catch (JsonSyntaxException ignored) {
                    failed.put(tee, "JSON cache corrupted");
                    return;
                }

                if (TeeController.getTriggerEmbed(event.getGuild(), tee.getTrigger()) != null)
                    failed.put(tee, "Trigger already exists");
            });



            imported.stream().filter(tee -> !failed.containsKey(tee))
                .filter(TeeController::addTriggerEmbed)
                .forEach(DatabaseUtil::saveObject);
            event.addCheckmark();
            m.delete().complete();

            PagedEmbed p = new PagedEmbed(me.zoemartin.rubie.core.util.EmbedUtil.pagedDescription(
                new EmbedBuilder().setTitle("Imported Trigger Embeds: " + imported.size()).build(),
                Stream.concat(
                    Stream.of(String.format("Time taken: %s seconds\n",
                        Duration.between(start, Instant.now()).toSeconds())),
                    imported.stream().map(tee -> String.format("`%s` - [Source](%s) - `[%s] %s` - `%s`\n",
                        tee.getTrigger(), tee.getSource_url(), tee.getPerm().raw(),
                        tee.getPerm().toString(),
                        failed.containsKey(tee) ? ("FAILED " + " | " + failed.get(tee))
                            : update.contains(tee) ? "UPDATED" : "CACHED"))
                ).collect(Collectors.toList())),
                event.getChannel(), event.getUser());

            PageListener.add(p);
        }

        @NotNull
        @Override
        public String name() {
            return "import";
        }

        @NotNull
        @Override
        public CommandPerm commandPerm() {
            return CommandPerm.BOT_ADMIN;
        }

        @NotNull
        @Override
        public String description() {
            return "Import Trigger Embeds from an attachment";
        }
    }
}
