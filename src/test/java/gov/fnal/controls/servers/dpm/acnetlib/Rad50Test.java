package gov.fnal.controls.servers.dpm.acnetlib;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class Rad50Test {

    @Test
    public void test_encode() {
        Assertions.assertEquals(2097184219, Rad50.encode("TEST"));
    }

    @Test
    public void test_decode() {
        Assertions.assertEquals("TEST", Rad50.decode(2097184219).trim());
    }
}
