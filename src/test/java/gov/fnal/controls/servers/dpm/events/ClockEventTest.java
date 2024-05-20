package gov.fnal.controls.servers.dpm.events;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ClockEventTest {

    private static final int MAX_NUMBER_EVENTS = 256;
    private static final String[] names = new String[MAX_NUMBER_EVENTS];

    @Test
    public void ClockEventTest1() {
        ClockEvent clockEvent = new ClockEvent(8900, false, 2000L, true);

    }
    @Test
    public void ClockEventTest2() {
        ClockEvent clockEvent = new ClockEvent(8900, true, 2000L, true);

    }
    @Test
    public void ClockEventTest3() {
        ClockEvent clockEvent = new ClockEvent(8900, false, 2000L, false);

    }
    @Test
    public void ClockEventTest4() {
        ClockEvent clockEvent = new ClockEvent(8900, true, 2000L, false);

    }

    @Test
    public void ClockTest_ftd(){

        ClockEvent clockEvent = new ClockEvent(2000, false, 0L);
        assertEquals(34768,clockEvent.ftd());
    }

}
