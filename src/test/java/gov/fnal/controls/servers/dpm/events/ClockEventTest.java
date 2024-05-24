
package gov.fnal.controls.servers.dpm.events;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ClockEventTest {

    private static final int MAX_NUMBER_EVENTS = 256;
    private static final String[] names = new String[MAX_NUMBER_EVENTS];

    @Test
    public void ClockTest_ftdWhenDelayIsNotZero(){

        ClockEvent clockEvent = new ClockEvent(2000, false, 10);
        assertEquals(0,clockEvent.ftd());
    }

    @Test
    public void ClockTest_ftdWhenDelayIsZero(){

        ClockEvent clockEvent = new ClockEvent(2000, false);
        assertEquals(34768,clockEvent.ftd());
    }

    @Test
    public void ClockTest_ftd(){

        ClockEvent clockEvent = new ClockEvent(23237);
        assertEquals(56005,clockEvent.ftd());
    }
    @Test
    public void ClockTest_ClockEventNumberToNameWhenClockEventIsGreaterThan256(){

        ClockEvent clockEvent = new ClockEvent(2000);
        assertEquals("invalid clock event", ClockEvent.clockEventNumberToName(350));
   }

}
