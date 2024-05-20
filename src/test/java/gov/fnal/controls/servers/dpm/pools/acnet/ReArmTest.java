package gov.fnal.controls.servers.dpm.pools.acnet;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ReArmTest {

    @Test
    public void test_setRecent() throws AcnetStatusException {
        ReArm reArm = new ReArm("rearm=false");
        reArm.setRecent(14678951);
    }
    @Test
    public void test_getReArmTime() throws AcnetStatusException {
        ReArm reArm = new ReArm("rearm=false");
        final long reArmTime = reArm.getReArmTime(185484126);
        Assertions.assertEquals(0,reArmTime);
    }
}
