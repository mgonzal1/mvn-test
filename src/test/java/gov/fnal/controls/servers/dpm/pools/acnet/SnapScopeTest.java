package gov.fnal.controls.servers.dpm.pools.acnet;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SnapScopeTest {

    @Test
    public void test_getRate() throws AcnetStatusException {
        SnapScope scope = new SnapScope(",rate=12034,dur=1203467,npts=4678,pref=4579,smpl=45679,mud=15485,5484=4571,457=471");
        final int rate = scope.getRate();
        Assertions.assertEquals(12034,rate);

    }
}
