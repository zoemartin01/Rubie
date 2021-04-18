package me.zoemartin.rubie.modules.debug;

import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.annotations.Command;
import me.zoemartin.rubie.core.annotations.CommandOptions;
import me.zoemartin.rubie.core.exceptions.CommandArgumentException;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.Check;
import me.zoemartin.rubie.core.util.Parser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Command
@CommandOptions(
    name = "purge",
    description = "Purges the last sent messages in this channel",
    usage = "<count>",
    perm = CommandPerm.BOT_ADMIN
)
public class Purge extends GuildCommand {
    @Override
    public void run(GuildCommandEvent event) {
        List<String> args = event.getArgs();
        Check.check(args.size() == 1 && Parser.Int.isParsable(args.get(0)), CommandArgumentException::new);
        int amount = Parser.Int.parse(args.get(0));

        Check.check(amount >= 0, CommandArgumentException::new);

        int y = amount % 100 == 0 ? 100 : amount % 100;
        int x = y == 0 ? (amount / 100) - 1 : amount / 100;

        OffsetDateTime twoWeeksAgo = OffsetDateTime.now().minus(2, ChronoUnit.WEEKS);


        Set<List<Message>> msgs = new HashSet<>();
        Message last = null;
        for (int i = 0; i < x; i++) {
            List<Message> m = new ArrayList<>(event.getChannel().getHistoryBefore(event.getChannel().getLatestMessageId(),
                100).complete().getRetrievedHistory());
            msgs.add(m);
            last = m.get(m.size() - 1);
        }
        msgs.add(new ArrayList<>(event.getChannel()
                                     .getHistoryBefore(last == null ? event.getChannel().getLatestMessageId() : last.getId(), y)
                                     .complete().getRetrievedHistory()));


        Map<String, Long> count = new ConcurrentHashMap<>();

        msgs.forEach(messages -> {
            messages.removeIf(m -> m.getTimeCreated().isBefore(twoWeeksAgo));

            Map<String, Long> c = messages.stream()
                                      .map(message -> message.getAuthor().getAsTag())
                                      .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

            c.forEach((s, aLong) -> count.put(s, count.getOrDefault(s + aLong, aLong)));
        });

        msgs.removeIf(List::isEmpty);

        event.deleteInvoking();
        Instant start = Instant.now();
        msgs.forEach(messages -> event.getTextChannel().deleteMessages(messages).complete());
        Instant end = Instant.now();


        long counter = count.values().stream().mapToLong(Long::longValue).sum();

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(String.format("Purged %d messages", counter));
        eb.setColor(0x2F3136);
        StringBuilder sb = new StringBuilder();
        count.forEach((s, aLong) -> sb.append("**").append(s).append("**: ").append(aLong).append(" messages\n"));
        sb.append("\nTime: ").append(Duration.between(start, end).toMillis()).append("ms");
        eb.setDescription(sb.toString());

        event.getChannel().sendMessage(eb.build()).complete().delete().queueAfter(5, TimeUnit.SECONDS);
    }
}
