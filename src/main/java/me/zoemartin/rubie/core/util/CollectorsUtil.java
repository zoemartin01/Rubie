package me.zoemartin.rubie.core.util;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class CollectorsUtil {
    public static <T> Collector<T, ?, Set<T>> toConcurrentSet() {
        return Collectors.toCollection(() -> Collections.newSetFromMap(new ConcurrentHashMap<>()));
    }
}
