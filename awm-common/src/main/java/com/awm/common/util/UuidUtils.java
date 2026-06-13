package com.awm.common.util;

import java.util.UUID;

public final class UuidUtils {

    private UuidUtils() {}

    public static String random() {
        return UUID.randomUUID().toString();
    }

    public static String randomCompact() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
