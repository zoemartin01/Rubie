package me.zoemartin.rubie.core.managers;

import me.zoemartin.rubie.core.Job;
import me.zoemartin.rubie.core.annotations.Disabled;
import me.zoemartin.rubie.core.interfaces.JobProcessor;
import me.zoemartin.rubie.core.interfaces.Module;
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
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private static final Map<UUID, Collection<Job>> jobs = new ConcurrentHashMap<>();
    private static final Map<UUID, Consumer<Job>> consumers = new ConcurrentHashMap<>();
    private static final Map<Job, Future<?>> futures = new ConcurrentHashMap<>();

    private static final Logger log = LoggerFactory.getLogger(JobManager.class);

    public static void init() {
        var loader = ServiceLoader.load(JobProcessor.class);

        jobs.putAll(DatabaseUtil.loadGroupedCollection("from Job", Job.class,
            Job::getJobId, Function.identity(), CollectorsUtil.toConcurrentSet()));

        StreamSupport.stream(loader.spliterator(), true)
            .filter(c -> c.getClass().getAnnotationsByType(Disabled.class).length == 0)
            .forEach(c -> {
                consumers.put(c.job(), c.process());
                log.info("Loaded Job Processor '{}':'{}' (ID '{}') with {} jobs",
                    c.getClass().getPackageName(), c.getClass().getSimpleName(), c.job(),
                    jobs.computeIfAbsent(c.job(), k -> Collections.newSetFromMap(new ConcurrentHashMap<>())).size());
            });

        log.info("Loaded {} Job Processors", loader.stream().count());
        consumers.forEach((uuid, jobConsumer) -> jobs.get(uuid).forEach(JobManager::schedule));
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

        futures.put(job, scheduler.scheduleWithFixedDelay(() -> {
            consumer.accept(job);
            log.info("Executed a job for '{}'", job.getJobId());
            DatabaseUtil.deleteObject(job);
            futures.remove(job).cancel(true);
        }, delay, delay, TimeUnit.MILLISECONDS));
    }

    public static void newJob(JobProcessor processor, long end, Map<String, String> settings) {
        var job = new Job(processor.job(), end, settings);
        schedule(job);
        log.info("Scheduled a job ({}) for {}", job.getJobId(), new DateTime(job.getEnd()));
        DatabaseUtil.saveObject(job);
    }

    static class Imp implements JobProcessor {
        @Override
        public String uuid() {
            return "08df9546-557e-4985-9b76-a4b397e28159";
        }

        @Override
        public Consumer<Job> process() {
            return job -> log.info("Job executed!!");
        }
    }
}
