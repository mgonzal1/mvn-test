package gov.fnal.controls.servers.dpm.pools.acnet;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.events.DataEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class StateTransitionTriggerTest {
    @Test
    public void test_getArmingEvents() throws AcnetStatusException {
        StateTransitionTrigger trigger = new StateTransitionTrigger("trg=ssest,urbern,12345,50000,!=");
        final List<DataEvent> armingEvents = trigger.getArmingEvents();
        Assertions.assertEquals(1,armingEvents.size());

    }
}
