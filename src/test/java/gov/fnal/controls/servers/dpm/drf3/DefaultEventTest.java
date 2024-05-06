package gov.fnal.controls.servers.dpm.drf3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class DefaultEventTest {

    @Test
    public void test_parseDefault(){
        final DefaultEvent defaultEvent = DefaultEvent.parseDefault("u");
        assertInstanceOf(DefaultEvent.class, defaultEvent);
    }
}
