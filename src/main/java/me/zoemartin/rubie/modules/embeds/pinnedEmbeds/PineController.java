package me.zoemartin.rubie.modules.embeds.pinnedEmbeds;

import com.sun.istack.Nullable;
import me.zoemartin.rubie.core.util.DatabaseUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import org.hibernate.Session;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PineController {
    private static final Map<String, Set<PineEntity>> pines = new ConcurrentHashMap<>();

    public static void init() {
        try (Session session = DatabaseUtil.getSessionFactory().openSession()) {
            List<PineEntity> load = session.createQuery("from PineEntity", PineEntity.class).list();
            load.forEach(l -> pines.computeIfAbsent(l.getGuild_id(),
                s -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(l));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Collection<PineEntity> getPines(Guild g) {
        return Collections.unmodifiableCollection(pines.getOrDefault(g.getId(), Collections.emptySet()));
    }

    @Nullable
    public static PineEntity getPine(Guild g, TextChannel c, String message_id) {
        return pines.computeIfAbsent(g.getId(), s -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
            .stream().filter(pineEntity -> pineEntity.getChannel_id().equals(c.getId())
                                               && pineEntity.getMessage_id().equals(message_id))
            .findAny().orElse(null);
    }

    public static boolean removePine(PineEntity entity) {
        return pines.computeIfAbsent(entity.getGuild_id(), s -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                   .removeIf(pineEntity -> pineEntity.getChannel_id().equals(entity.getChannel_id())
                                               && pineEntity.getMessage_id().equals(entity.getMessage_id()));
    }

    public static boolean addPine(PineEntity pineEntity) {
        return pines.computeIfAbsent(pineEntity.getGuild_id(), s -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
            .add(pineEntity);
    }
}
