package gov.fnal.controls.servers.dpm.pools.acnet;

import org.junit.jupiter.api.Test;

public class TimeStamperTest {

    @Test
    public void test_secsTicksInSuperCycle() {
        final int ticks = TimeStamper.secsTicksInSuperCycle(10);
        final long baseTime02 = TimeStamper.getBaseTime02(12434679);
        System.out.println(ticks);
        System.out.println(baseTime02);
    }
}
