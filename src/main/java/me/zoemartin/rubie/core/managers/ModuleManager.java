package me.zoemartin.rubie.core.managers;

import javassist.*;
import me.zoemartin.rubie.core.*;
import me.zoemartin.rubie.core.annotations.*;
import me.zoemartin.rubie.core.interfaces.*;
import me.zoemartin.rubie.core.interfaces.Module;
import me.zoemartin.rubie.core.util.DatabaseUtil;
import org.jetbrains.annotations.NotNull;
import org.reflections8.Reflections;
import org.reflections8.scanners.SubTypesScanner;
import org.reflections8.scanners.TypeAnnotationsScanner;
import org.reflections8.util.ClasspathHelper;
import org.reflections8.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ModuleManager {
    private static final Logger log = LoggerFactory.getLogger(ModuleManager.class);
    private static final Collection<Module> modules = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static void init() {
        // This is a really dumb way to suppress all the warns coming from java reflections
        // but it works so ¯\_(ツ)_/¯

        PrintStream err = System.err;
        System.setErr(new PrintStream(OutputStream.nullOutputStream()));
        Reflections reflections = new Reflections(ClasspathHelper.forJavaClassPath(), new TypeAnnotationsScanner(), new SubTypesScanner());
        System.setErr(err);


        Set<Class<?>> modules = reflections.getTypesAnnotatedWith(LoadModule.class);
        modules.removeIf(aClass -> !Set.of(aClass.getInterfaces()).contains(Module.class));
        modules.removeIf(aClass -> {
            try {
                aClass.getMethod("init");
                return false;
            } catch (NoSuchMethodException e) {
                return true;
            }
        });

        ExecutorService es = Executors.newCachedThreadPool();
        modules.forEach(module -> es.execute(() -> loadModule(module)));
        es.shutdown();
        try {
            es.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void loadModule(Class<?> module) {
        Module m = null;
        try {
            m = (Module) Stream.of(module.getConstructors()).findAny()
                             .orElseThrow(RuntimeException::new).newInstance();
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            e.printStackTrace();
        }
        if (m == null) return;
        loadModuleMappings(m);
        m.init();
        loadModuleCommands(m);
        ModuleManager.modules.add(m);
        log.info("Loaded '{}'", module.getName());
    }

    @SuppressWarnings("unchecked")
    private static void loadModuleCommands(Module m) {
        Reflections reflections = new Reflections(m.getClass().getPackageName(), new TypeAnnotationsScanner(), new SubTypesScanner());

        Set<Class<?>> commandReflect = reflections.getTypesAnnotatedWith(Command.class);

        commandReflect.stream()
            .filter(c -> c.getAnnotationsByType(Disabled.class).length == 0)
            .forEach(c -> {
                AbstractCommand command = null;
                try {
                    Constructor<? extends AbstractCommand> constructor =
                        (Constructor<? extends AbstractCommand>)
                            Arrays.stream(c.getDeclaredConstructors()).findAny().orElseThrow(
                                () -> new IllegalStateException("Command Class missing a public no-args Constructor"));
                    constructor.setAccessible(true);
                    command = constructor.newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    System.err.println(e.getMessage());
                }
                if (command == null) return;

                sub(command);
                CommandManager.register(command);
                log.info("Loaded '{}' command '{}'", m.getClass().getName(), command.name());
            });
    }

    private static void loadModuleMappings(Module m) {
        Reflections reflections = new Reflections(m.getClass().getPackageName(), new TypeAnnotationsScanner(), new SubTypesScanner());

        Set<Class<?>> commandReflect = reflections.getTypesAnnotatedWith(Mapped.class);

        commandReflect.stream()
            .filter(c -> c.getAnnotationsByType(Disabled.class).length == 0)
            .forEach(c -> {
                DatabaseUtil.setMapped(c);
                log.info("Mapped '{}':'{}'", m.getClass().getName(), c.getSimpleName());
            });
    }


    private static void sub(AbstractCommand c) {
        if (c.getClass().getAnnotationsByType(SubCommand.AsBase.class).length != 0)
            CommandManager.register(subcommand(c));


        if (!c.subCommands().isEmpty()) {
            c.subCommands().forEach(ModuleManager::sub);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends AbstractCommand> T subcommand(T c) {
        SubCommand.AsBase asBase = c.getClass().getAnnotationsByType(SubCommand.AsBase.class)[0];

        CommandConfiguration conf = new CommandConfiguration(c.subCommands(), asBase.name(), c.commandPerm(), c.usage(), c.description(), c.getConfiguration().getBotPerms(), asBase.alias(), c.getConfiguration().getHelp());

        if (c instanceof GuildCommand) {
            return (T) new GuildCommand(conf) {
                @Override
                public void run(GuildCommandEvent event) {
                    ((GuildCommand) c).run(event);
                }
            };
        } else {
            return (T) new AbstractCommand(conf) {
                @Override
                public void run(CommandEvent event) {
                    c.run(event);
                }
            };
        }

    }

    public static void initLate() {
        modules.forEach(Module::initLate);
    }
}
