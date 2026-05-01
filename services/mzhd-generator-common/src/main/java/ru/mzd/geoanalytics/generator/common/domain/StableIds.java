package ru.mzd.geoanalytics.generator.common.domain;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class StableIds {

    private StableIds() {
    }

    public static UUID nameUuid(String seed) {
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }
}
