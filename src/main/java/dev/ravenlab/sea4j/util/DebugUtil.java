package dev.ravenlab.sea4j.util;

import dev.ravenlab.sea4j.Constant;

public final class DebugUtil {

    private DebugUtil() {}

    public static String checkForDebugUrl(String url) {
        if(System.getProperty(Constant.DEBUG_KEY) != null) {
            String[] split = url.split(":");
            return "localhost:" + split[1];
        }
        return url;
    }
}