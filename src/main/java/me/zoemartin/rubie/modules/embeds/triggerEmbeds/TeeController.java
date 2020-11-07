package me.zoemartin.rubie.modules.embeds.triggerEmbeds;

import com.google.gson.JsonSyntaxException;
import com.sun.istack.Nullable;
import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.Embed;
import me.zoemartin.rubie.core.util.DatabaseUtil;
import me.zoemartin.rubie.modules.commandProcessing.PermissionHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TeeController extends ListenerAdapter {
    private static final Map<String, Set<Tee>> triggers = new ConcurrentHashMap<>();

    public static void init() {
        try (Session session = DatabaseUtil.getSessionFactory().openSession()) {
            List<Tee> load = session.createQuery("from Tee", Tee.class).list();
            load.forEach(l -> triggers.computeIfAbsent(l.getGuild_id(),
                s -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(l));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        Collection<Tee> triggerCollection = triggers.getOrDefault(event.getGuild().getId(),
            Collections.emptySet());

        if (triggerCollection.isEmpty()) return;
        Tee tee = triggerCollection.stream()
                      .filter(entity -> event.getMessage().getContentRaw()
                                            .matches(entity.getTrigger()))
                      .findAny().orElse(null);

        if (tee == null) return;
        Member member = event.getMember();
        Guild guild = event.getGuild();
        if (tee.getPerm() != null && tee.getPerm() != CommandPerm.EVERYONE) {
            if (PermissionHandler.getHighestFromUser(guild, member).raw() < tee.getPerm().raw()) return;
        }

        Embed e;
        try {
            e = Embed.fromJson(tee.getCached_json());
        } catch (JsonSyntaxException ignored) {
            return;
        }

        event.getChannel().sendMessage(e.toDiscordEmbed()).queue();
    }

    public static Collection<Tee> getTriggerEmbeds(Guild g) {
        return Collections.unmodifiableCollection(triggers.getOrDefault(g.getId(), Collections.emptySet()));
    }

    @Nullable
    public static Tee getTriggerEmbed(Guild g, String trigger) {
        return triggers.computeIfAbsent(g.getId(), s -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                   .stream().filter(entity -> entity.getTrigger().equals(trigger))
                   .findAny().orElse(null);
    }

    public static boolean removeTriggerEmbed(Tee e) {
        return triggers.computeIfAbsent(e.getGuild_id(), s -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                   .removeIf(entity -> entity.getTrigger().equals(e.getTrigger()));
    }

    public static boolean addTriggerEmbed(Tee e) {
        return triggers.computeIfAbsent(e.getGuild_id(), s -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                   .add(e);
    }
}
