package io.github.shangor.gateway;

import com.fasterxml.uuid.Generators;
import org.junit.jupiter.api.Test;

class SimpleTests {
    @Test
    void testUuidV7() {
        var uuidV7 = Generators.timeBasedEpochGenerator();
        System.out.println("UUID Version 7 1st: " + uuidV7.generate());
        System.out.println("UUID Version 7 2nd: " + uuidV7.generate());
    }
}
