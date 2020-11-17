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
        System.out.printf("\u001B[95m[Module Loader] Loaded '%s'\u001B[0m\n", module.getName());
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
                System.out.printf("\u001B[95m[Command Loader] Loaded '%s' command '%s'\u001B[0m\n", m.getClass().getName(), command.name());
            });
    }

    private static void loadModuleMappings(Module m) {
        Reflections reflections = new Reflections(m.getClass().getPackageName(), new TypeAnnotationsScanner(), new SubTypesScanner());

        Set<Class<?>> commandReflect = reflections.getTypesAnnotatedWith(Mapped.class);

        commandReflect.stream()
            .filter(c -> c.getAnnotationsByType(Disabled.class).length == 0)
            .forEach(c -> {
                DatabaseUtil.setMapped(c);
                System.out.printf("\u001B[95m[Database Mapper] Mapped '%s':'%s'\u001B[0m\n", m.getClass().getName(), c.getName());
            });
    }


    private static void sub(AbstractCommand c) {
        if (c.getClass().getAnnotationsByType(SubCommand.AsBase.class).length != 0) {
            if (c instanceof GuildCommand)
                CommandManager.register(new SubcommandAdapter<>((GuildCommand) c));
            else
                CommandManager.register(new SubcommandAdapter<>(c));
        }

        if (!c.subCommands().isEmpty()) {
            c.subCommands().forEach(ModuleManager::sub);
        }
    }

    public static void initLate() {
        modules.forEach(Module::initLate);
    }
}
