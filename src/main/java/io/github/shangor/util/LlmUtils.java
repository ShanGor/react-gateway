package io.github.shangor.util;

import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
public class LlmUtils {

    private static final Set<String> NOT_STREAMABLE_MODELS = Set.of("o1", "o1-mini");

    private LlmUtils() {}

    public static boolean streamable(String model) {
        return !NOT_STREAMABLE_MODELS.contains("chat");
    }
}
