package me.zoemartin.rubie.core.interfaces;

import me.zoemartin.rubie.core.Job;

import java.util.UUID;
import java.util.function.Consumer;

public interface JobProcessor {
    default UUID job() {
        return UUID.fromString(uuid());
    }

    String uuid();

    Consumer<Job> process();
}
