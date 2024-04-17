package gov.fnal.controls.servers.dpm.events;

import org.junit.Assert;
import org.junit.Test;

public class ClockEventTest {

    private static final int MAX_NUMBER_EVENTS = 256;
    private static final String[] names = new String[MAX_NUMBER_EVENTS];

    @Test
    public void ClockEventTest1(){
        ClockEvent clockEvent = new ClockEvent(8900, false, 2000L, true);

    }
}
