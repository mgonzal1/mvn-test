package gov.fnal.controls.servers.dpm.events;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

public class AbsoluteTimeEventTest {

    @Test
    public void test_addObserver()
    {
        AbsoluteTimeEvent absoluteTimeEvent = new AbsoluteTimeEvent(50000);
        DataEventObserver dataEventObserver = mock(DataEventObserver.class);
        absoluteTimeEvent.addObserver(dataEventObserver);
    }

    @Test
    public void test_run()
    {
        AbsoluteTimeEvent absoluteTimeEvent = new AbsoluteTimeEvent(50000);
        absoluteTimeEvent.run();
    }
}
