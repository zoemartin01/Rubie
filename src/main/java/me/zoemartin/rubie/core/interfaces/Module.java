package me.zoemartin.rubie.core.interfaces;

public interface Module {
    void init();
    default void initLate() {
    }
}
