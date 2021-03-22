package me.zoemartin.rubie.core.managers;

import me.zoemartin.rubie.core.Job;
import me.zoemartin.rubie.core.annotations.Disabled;
import me.zoemartin.rubie.core.interfaces.JobProcessor;
import me.zoemartin.rubie.core.util.CollectorsUtil;
import me.zoemartin.rubie.core.util.DatabaseUtil;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.StreamSupport;

public class JobManager {
    private static final int poolSize = 10;
    private static final Logger log = LoggerFactory.getLogger(JobManager.class);

    private static final ScheduledExecutorService pool = Executors.newScheduledThreadPool(poolSize);
    private static final Map<UUID, Consumer<Job>> consumers = new ConcurrentHashMap<>();

    public static void init() {
        var loader = ServiceLoader.load(JobProcessor.class);

        var jobs = new ConcurrentHashMap<UUID, Collection<Job>>(
            DatabaseUtil.loadGroupedCollection("from Job", Job.class,
                Job::getJobId, Function.identity(), CollectorsUtil.toConcurrentSet()));

        StreamSupport.stream(loader.spliterator(), true)
            .filter(c -> c.getClass().getAnnotationsByType(Disabled.class).length == 0)
            .forEach(c -> {
                consumers.put(c.job(), c.process());
                log.info("Loaded Job Processor '{}':'{}' (ID '{}') with {} jobs",
                    c.getClass().getPackageName(), c.getClass().getSimpleName(), c.job(),
                    jobs.getOrDefault(c.job(), Collections.emptySet()).size());
            });

        log.info("Loaded {} Job Processors", loader.stream().count());
        consumers.forEach((uuid, jobConsumer) ->
                              jobs.getOrDefault(uuid, Collections.emptySet()).forEach(JobManager::schedule));
    }

    private static void schedule(Job job) {
        var consumer = consumers.getOrDefault(job.getJobId(), null);
        if (consumer == null) return;

        var delay = job.getEnd() - Instant.now().toEpochMilli();
        if (delay < 0) {
            consumer.accept(job);
            DatabaseUtil.deleteObject(job);
            return;
        }

        pool.schedule(() -> {
            consumer.accept(job);
            log.info("Executed a job for '{}'", job.getJobId());
            DatabaseUtil.deleteObject(job);
        }, delay, TimeUnit.MILLISECONDS);
    }

    public static void newJob(JobProcessor processor, long end, Map<String, String> settings) {
        var job = new Job(processor.job(), end, settings);
        DatabaseUtil.saveObject(job);
        log.info("Scheduling a job ({}) for {}", job.getJobId(), new DateTime(job.getEnd()));
        schedule(job);
    }
}
