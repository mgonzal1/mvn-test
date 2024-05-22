package gov.fnal.controls.servers.dpm.pools.acnet;

import gov.fnal.controls.servers.dpm.acnetlib.Node;
import gov.fnal.controls.servers.dpm.pools.ReceiveData;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;

public class SettingDaqPoolTest {

    @Test
    public void test_insert(){
        Node node = mock(Node.class);
        ReceiveData receiveData = Mockito.mock(ReceiveData.class);
        WhatDaq whatDaq1 = Mockito.mock(WhatDaq.class);
        WhatDaq whatDaq = new WhatDaq(whatDaq1, 487, 10, 457, receiveData);
        SettingDaqPool settingDaqPool = new SettingDaqPool(node);
        settingDaqPool.insert(whatDaq);
        Assertions.assertEquals(1,settingDaqPool.queuedReqs.size());
    }
}
