package gov.fnal.controls.servers.dpm.drf3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class ClockEventTest {

    @Test
    public void test_parseClock() {
        final ClockEvent clockEvent = ClockEvent.parseClock("E,1A3B,H,123H");
        assertInstanceOf(ClockEvent.class, clockEvent);
    }
}
