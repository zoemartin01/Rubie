package me.zoemartin.rubie.core.managers;

import me.zoemartin.rubie.core.Job;
import me.zoemartin.rubie.core.annotations.Disabled;
import me.zoemartin.rubie.core.annotations.JobProcessor;
import me.zoemartin.rubie.core.interfaces.JobProcessorInterface;
import me.zoemartin.rubie.core.util.CollectorsUtil;
import me.zoemartin.rubie.core.util.DatabaseUtil;
import org.joda.time.DateTime;
import org.reflections8.Reflections;
import org.reflections8.scanners.SubTypesScanner;
import org.reflections8.scanners.TypeAnnotationsScanner;
import org.reflections8.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JobManager {
    private static final int poolSize = 10;
    private static final Logger log = LoggerFactory.getLogger(JobManager.class);

    private static final ScheduledExecutorService pool = Executors.newScheduledThreadPool(poolSize);
    private static final Map<UUID, Consumer<Job>> consumers = new ConcurrentHashMap<>();

    public static void init() {
        var jobs = new ConcurrentHashMap<UUID, Collection<Job>>(
            DatabaseUtil.loadGroupedCollection("from Job", Job.class,
                Job::getJobId, Function.identity(), CollectorsUtil.toConcurrentSet()));
        var load = loadProcessors();

        load
            .forEach(c -> {
                consumers.put(c.job(), c.process());
                log.info("Loaded Job Processor '{}':'{}' (ID '{}') with {} jobs",
                    c.getClass().getPackageName(), c.getClass().getSimpleName(), c.job(),
                    jobs.getOrDefault(c.job(), Collections.emptySet()).size());
            });

        log.info("Loaded {} Job Processors", load.size());
        consumers.forEach((uuid, jobConsumer) ->
                              jobs.getOrDefault(uuid, Collections.emptySet()).forEach(JobManager::schedule));
    }

    private static Collection<JobProcessorInterface> loadProcessors() {
        var modulePaths = ModuleManager.getModulePaths();
        var reflections = new Reflections(new ConfigurationBuilder()
                                              .setUrls(
                                                  modulePaths.stream().map(ClasspathHelper::forPackage)
                                                      .flatMap(Collection::stream).collect(Collectors.toList())
                                              )
                                              .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner())
                                              .filterInputsBy(new FilterBuilder().includePackage(modulePaths.toArray(String[]::new)))
                                              .setExecutorService(Executors.newFixedThreadPool(4)));

        var processors = reflections.getTypesAnnotatedWith(JobProcessor.class);

        return processors.parallelStream()
                   .filter(c -> c.getAnnotationsByType(Disabled.class).length == 0)
                   .map(aClass -> {
                       try {
                           if (Arrays.stream(aClass.getInterfaces()).noneMatch(clazz -> clazz == JobProcessorInterface.class)) {
                               log.error("Trying to load a class {} that doesn't implement the job processor interface", aClass.getSimpleName());
                               return null;
                           }
                           var constructor = aClass.getDeclaredConstructor();
                           return (JobProcessorInterface) constructor.newInstance();
                       } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                           log.error("Error getting default constructor for module", e);
                           return null;
                       }
                   })
                   .filter(Objects::nonNull)
                   .collect(Collectors.toList());
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

    public static void newJob(JobProcessorInterface processor, long end, Map<String, String> settings) {
        var job = new Job(processor.job(), end, settings);
        DatabaseUtil.saveObject(job);
        log.info("Scheduling a job ({}) for {}", job.getJobId(), new DateTime(job.getEnd()));
        schedule(job);
    }
}
