package gov.fnal.controls.servers.dpm.pool;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

public class AcnetPoolImplTest {

    @Test
    public void test_handle() throws AcnetStatusException {
        WhatDaq whatDaq = mock(WhatDaq.class);
//        doNothing(whatDaq.setSetting(any()));
    }
}
