package gov.fnal.controls.servers.dpm.events;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DeltaTimeEventTest {

    @Test
    public void test_DefaultTimeoutT1T2MillisZero() {
        DeltaTimeEvent deltaTimeEvent = new DeltaTimeEvent(1000L, 2000L);
        assertEquals(5000L, deltaTimeEvent.defaultTimeout());
    }

    @Test
    public void test_DefaultTimeoutT1T2Millis() {
        DeltaTimeEvent deltaTimeEvent = new DeltaTimeEvent(1000L, 2000L, 5000L);
        assertEquals(15000L, deltaTimeEvent.defaultTimeout());
    }

    @Test
    public void test_DefaultTimeoutT1T2Millis15000() {
        DeltaTimeEvent deltaTimeEvent = new DeltaTimeEvent(1000L, 2000L, 1000L);
        assertEquals(3500, deltaTimeEvent.defaultTimeout());
    }

    @Test
    public void test_DefaultTimeoutMillis() {
        DeltaTimeEvent deltaTimeEvent = new DeltaTimeEvent(5000L);
        assertEquals(15000L, deltaTimeEvent.defaultTimeout());
    }

    @Test
    public void test_DefaultTimeoutMImm() {
        DeltaTimeEvent deltaTimeEvent = new DeltaTimeEvent(5000L, false);
        assertEquals(15000L, deltaTimeEvent.defaultTimeout());
    }

    @Test
    public void test_ftdmytest() {
        DeltaTimeEvent deltaTimeEvent = new DeltaTimeEvent(1000L, 2000L, 0);
        assertEquals(0, deltaTimeEvent.ftd());
    }

    @Test
    public void test_ftdGreaterThan4() {
        DeltaTimeEvent deltaTimeEvent = new DeltaTimeEvent(1000L, 2000L, 1000L);
        assertEquals(60, deltaTimeEvent.ftd());
    }

    @Test
    public void test_ftdLessThan4() {
        DeltaTimeEvent deltaTimeEvent = new DeltaTimeEvent(1000L, 2000L, 20L);
        assertEquals(4L, deltaTimeEvent.ftd());
    }

    @Test
    public void test_ftdLessThan4WhenDeltaTimeEventIsMillis() {
        DeltaTimeEvent deltaTimeEvent = new DeltaTimeEvent(5000L);
        assertEquals(300L, deltaTimeEvent.ftd());
    }
}
