
package gov.fnal.controls.servers.dpm.events;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ClockEventTest {
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
        assertEquals("invalid clock event", ClockEvent.clockEventNumberToName(350));
    }

}
