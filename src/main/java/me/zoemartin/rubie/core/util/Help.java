package me.zoemartin.rubie.core.util;

import me.zoemartin.rubie.core.interfaces.Helper;

public class Help {
    private static Helper helper = null;

    public static void setHelper(Helper helper) {
        Help.helper = helper;
    }

    public static Helper getHelper() {
        return helper;
    }
}
