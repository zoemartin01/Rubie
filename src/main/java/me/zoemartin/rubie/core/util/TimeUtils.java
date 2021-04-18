package me.zoemartin.rubie.core.util;

import org.joda.time.*;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.time.OffsetDateTime;

public class TimeUtils {
    public static String dateAgo(OffsetDateTime start, OffsetDateTime end) {
        Duration duration = Duration.millis(java.time.Duration.between(start, end).toMillis());
        Period period = duration.toPeriodFrom(Instant.ofEpochMilli(start.toInstant().toEpochMilli()));
        PeriodFormatter formatter;

        if (period.getMonths() < 1 && period.getYears() < 1)
            formatter = new PeriodFormatterBuilder()
                            .appendWeeks()
                            .appendSuffix(period.getWeeks() > 1 ? " weeks " : " week ")
                            .appendDays()
                            .appendSuffix(period.getDays() > 1 ? " days " : " day ")
                            .appendHours()
                            .appendSuffix(period.getHours() > 1 ? " hours " : " hour ")
                            .appendMinutes()
                            .appendSuffix(period.getMinutes() > 1 ? " minutes " : " minute ")
                            .appendPrefix(" and ")
                            .appendSeconds()
                            .appendSuffix(period.getSeconds() > 1 ? " seconds" : " second")
                            .toFormatter();
        else formatter = new PeriodFormatterBuilder()
                             .appendYears()
                             .appendSuffix(period.getYears() > 1 ? " years " : " year ")
                             .appendMonths()
                             .appendSuffix(period.getMonths() > 1 ? " months " : " month ")
                             .appendWeeks()
                             .appendSuffix(period.getWeeks() > 1 ? " weeks " : " week ")
                             .appendPrefix(" and ")
                             .appendDays()
                             .appendSuffix(period.getDays() > 1 ? " days " : " day ")
                             .toFormatter();

        return formatter.print(period);
    }
}
