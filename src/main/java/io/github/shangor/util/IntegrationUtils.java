package io.github.shangor.util;

import com.github.f4b6a3.uuid.UuidCreator;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class IntegrationUtils {
    private IntegrationUtils() {}

    public static UUID uuidV7() {
        return UuidCreator.getTimeOrderedEpoch();
    }
}
