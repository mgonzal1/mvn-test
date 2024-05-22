package gov.fnal.controls.servers.dpm.pools.acnet;

import gov.fnal.controls.servers.dpm.pools.ReceiveData;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

public class PoolSegmentAssemblyTest {

    @Test
    public void test_insert() {
        ReceiveData receiveData = mock(ReceiveData.class);
        WhatDaq whatDaq1 = mock(WhatDaq.class);
        DaqPool daqPool = mock(DaqPool.class);
        WhatDaq whatDaq = new WhatDaq(whatDaq1, 487, 10, 457, receiveData);
        PoolSegmentAssembly.insert(whatDaq, daqPool, 1245, true);
    }
}
